/**
 * GET /api/handshake/pending?userId=...
 * Returns pending handshakes where the user is the receiver (matched by telegram handle or email).
 * Uses service role to bypass RLS since receiver_user_id is NULL on pending handshakes.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL || process.env.SUPABASE_URL || '',
  process.env.SUPABASE_SERVICE_ROLE_KEY || ''
);

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const userId = req.query.userId as string;
  if (!userId) {
    return res.status(400).json({ error: 'userId required' });
  }

  try {
    // Look up user's identifiers (telegram handle and email)
    const { data: telegramLink } = await supabase
      .from('telegram_links')
      .select('telegram_username')
      .eq('user_id', userId)
      .single();

    const { data: authUser } = await supabase.auth.admin.getUserById(userId);
    const userEmail = authUser?.user?.email;
    const userTelegram = telegramLink?.telegram_username;

    if (!userEmail && !userTelegram) {
      return res.status(200).json({ handshakes: [] });
    }

    // Build OR filter for receiver_identifier matching user's identifiers
    const identifiers: string[] = [];
    if (userTelegram) {
      identifiers.push(userTelegram.toLowerCase());
      identifiers.push(`@${userTelegram.toLowerCase()}`);
    }
    if (userEmail) {
      identifiers.push(userEmail.toLowerCase());
    }

    // Query pending handshakes where receiver_identifier matches
    const { data, error } = await supabase
      .from('handshakes')
      .select('*')
      .eq('status', 'pending')
      .is('receiver_user_id', null)
      .neq('initiator_user_id', userId)
      .in('receiver_identifier', identifiers)
      .order('created_at', { ascending: false });

    if (error) {
      console.error('Error fetching pending handshakes:', error);
      return res.status(500).json({ error: 'Failed to fetch pending handshakes' });
    }

    // Resolve initiator names so the receiver knows who sent each handshake
    const handshakes = data || [];
    const initiatorIds = [...new Set(handshakes.map((h) => h.initiator_user_id))];

    const nameMap: Record<string, string> = {};
    for (const uid of initiatorIds) {
      const { data: initiator } = await supabase.auth.admin.getUserById(uid);
      if (initiator?.user) {
        nameMap[uid] =
          initiator.user.user_metadata?.full_name ||
          initiator.user.email?.split('@')[0] ||
          'Someone';
      }
    }

    const enriched = handshakes.map((h) => ({
      ...h,
      initiator_name: nameMap[h.initiator_user_id] || 'Someone',
    }));

    return res.status(200).json({ handshakes: enriched });
  } catch (error) {
    console.error('Pending handshakes error:', error);
    return res.status(500).json({ error: 'Failed to fetch pending handshakes' });
  }
}
