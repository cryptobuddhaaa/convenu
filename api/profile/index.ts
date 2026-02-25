/**
 * GET/PUT /api/profile?action=...
 * Read or update the authenticated user's profile.
 * Also routes admin dashboard actions via ?action=admin-* to avoid consuming
 * an additional Vercel function slot (Hobby plan limit: 12).
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import { requireAuth } from '../_lib/auth.js';
import { handleAdminAction } from '../_lib/admin-handler.js';

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

  const authUser = await requireAuth(req, res);
  if (!authUser) return;

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
