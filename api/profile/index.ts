/**
 * GET/PUT /api/profile?action=...
 * Consolidated profile, admin, trust, and wallet endpoint.
 *
 * Actions routed here to minimize Vercel Hobby plan function count:
 *   admin-*          — Admin dashboard (delegated to admin-handler)
 *   wallet-auth      — Wallet-based login/signup (no JWT)
 *   compute-trust    — Recompute trust score (JWT)
 *   verify-wallet    — Verify wallet ownership (JWT)
 *   enrich           — AI-powered contact enrichment (POST, JWT)
 *   enrich-usage     — Current month's usage + all enrichments (GET, JWT)
 *   (none)           — Profile CRUD (GET/PUT, JWT)
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import { requireAuth } from '../_lib/auth.js';
import { handleAdminAction } from '../_lib/admin-handler.js';
import {
  performEnrichment,
  getUsage,
  getEnrichmentsForUser,
} from '../_lib/enrichment.js';
import { handleComputeTrust } from '../_lib/trust-handler.js';
import { handleWalletAuth, handleWalletVerify } from '../_lib/wallet-handler.js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL || process.env.SUPABASE_URL || '',
  process.env.SUPABASE_SERVICE_ROLE_KEY || ''
);

const ALLOWED_FIELDS = [
  'first_name', 'last_name', 'company', 'position', 'bio',
  'twitter_handle', 'linkedin_url', 'website', 'avatar_url',
] as const;

const MAX_FIELD_LENGTH = 500;
const MAX_BIO_LENGTH = 2000;

export default async function handler(req: VercelRequest, res: VercelResponse) {
  // Route admin actions (admin auth handled inside handleAdminAction)
  const action = String(req.query.action || '');
  if (action.startsWith('admin-')) {
    return handleAdminAction(action, req, res);
  }

  // Wallet-based auth (no JWT required — this IS the auth flow)
  if (action === 'wallet-auth') {
    return handleWalletAuth(req, res);
  }

  // Trust score computation (has own auth check inside)
  if (action === 'compute-trust') {
    return handleComputeTrust(req, res);
  }

  // Wallet verification (has own auth check inside)
  if (action === 'verify-wallet') {
    return handleWalletVerify(req, res);
  }

  const authUser = await requireAuth(req, res);
  if (!authUser) return;

  // --- Enrichment actions ---
  if (action === 'enrich' && req.method === 'POST') {
    try {
      const { contactId, name, context } = req.body || {};
      if (!contactId || !name) {
        return res.status(400).json({ error: 'contactId and name are required' });
      }
      const enrichment = await performEnrichment(
        authUser.id,
        String(contactId),
        String(name).slice(0, 200),
        context ? String(context).slice(0, 500) : undefined
      );
      return res.status(200).json({ enrichment });
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Enrichment failed';
      if (msg.startsWith('LIMIT_REACHED:')) {
        return res.status(429).json({ error: msg });
      }
      console.error('[Enrich] Error:', err);
      return res.status(500).json({ error: msg });
    }
  }

  if (action === 'enrich-usage' && req.method === 'GET') {
    try {
      const usage = await getUsage(authUser.id);
      const enrichments = await getEnrichmentsForUser(authUser.id);
      return res.status(200).json({ usage, enrichments });
    } catch (err) {
      console.error('[Enrich] Usage fetch error:', err);
      return res.status(500).json({ error: 'Failed to load enrichment data' });
    }
  }

  if (req.method === 'GET') {
    const { data, error } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('user_id', authUser.id)
      .single();

    if (error && error.code !== 'PGRST116') {
      return res.status(500).json({ error: 'Failed to load profile' });
    }

    return res.status(200).json({ profile: data || null });
  }

  if (req.method === 'PUT') {
    const body = req.body || {};

    // Sanitize: only allow known fields, enforce length limits
    const updates: Record<string, string | null> = {};
    for (const field of ALLOWED_FIELDS) {
      if (field in body) {
        const val = body[field];
        if (val === null || val === '') {
          updates[field] = null;
        } else if (typeof val === 'string') {
          const maxLen = field === 'bio' ? MAX_BIO_LENGTH : MAX_FIELD_LENGTH;
          updates[field] = val.slice(0, maxLen).trim();
        }
      }
    }

    if (Object.keys(updates).length === 0) {
      return res.status(400).json({ error: 'No valid fields to update' });
    }

    updates.updated_at = new Date().toISOString();

    const { data, error } = await supabase
      .from('user_profiles')
      .upsert(
        { user_id: authUser.id, ...updates },
        { onConflict: 'user_id' }
      )
      .select()
      .single();

    if (error) {
      console.error('Profile update error:', error);
      return res.status(500).json({ error: 'Failed to update profile' });
    }

    return res.status(200).json({ profile: data });
  }

  return res.status(405).json({ error: 'Method not allowed' });
}
