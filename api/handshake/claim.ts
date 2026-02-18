/**
 * POST /api/handshake/claim
 * Receiver claims a pending handshake, pays 0.01 SOL fee, triggers match.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import {
  Connection,
  PublicKey,
  Transaction,
  SystemProgram,
  LAMPORTS_PER_SOL,
} from '@solana/web3.js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL || process.env.SUPABASE_URL || '',
  process.env.SUPABASE_SERVICE_ROLE_KEY || ''
);

const SOLANA_RPC = process.env.VITE_SOLANA_RPC_URL || 'https://api.devnet.solana.com';
const TREASURY_WALLET = process.env.VITE_TREASURY_WALLET || '';
const MINT_FEE_LAMPORTS = 0.01 * LAMPORTS_PER_SOL;

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { handshakeId, userId, walletAddress } = req.body || {};

  if (!handshakeId || !userId || !walletAddress) {
    return res.status(400).json({ error: 'handshakeId, userId, and walletAddress required' });
  }

  if (!TREASURY_WALLET) {
    return res.status(500).json({ error: 'Treasury wallet not configured' });
  }

  try {
    // Fetch the handshake
    const { data: handshake, error: hsError } = await supabase
      .from('handshakes')
      .select('*')
      .eq('id', handshakeId)
      .single();

    if (hsError || !handshake) {
      return res.status(404).json({ error: 'Handshake not found' });
    }

    if (handshake.status !== 'pending') {
      return res.status(409).json({
        error: `Handshake is already ${handshake.status}`,
        status: handshake.status,
      });
    }

    // Verify the claimer is not the initiator
    if (handshake.initiator_user_id === userId) {
      return res.status(400).json({ error: 'Cannot claim your own handshake' });
    }

    // Check expiry
    if (new Date(handshake.expires_at) < new Date()) {
      await supabase
        .from('handshakes')
        .update({ status: 'expired' })
        .eq('id', handshakeId);
      return res.status(410).json({ error: 'Handshake has expired' });
    }

    // Verify the claimer matches the receiver identifier (telegram handle or email)
    // Look up user's telegram link or email
    const { data: telegramLink } = await supabase
      .from('telegram_links')
      .select('telegram_username')
      .eq('user_id', userId)
      .single();

    const { data: authUser } = await supabase.auth.admin.getUserById(userId);
    const userEmail = authUser?.user?.email;
    const userTelegram = telegramLink?.telegram_username;

    const receiverId = handshake.receiver_identifier;
    const identifierMatch =
      (userTelegram && receiverId.replace('@', '').toLowerCase() === userTelegram.toLowerCase()) ||
      (userEmail && receiverId.toLowerCase() === userEmail.toLowerCase());

    if (!identifierMatch) {
      return res.status(403).json({ error: 'You are not the intended receiver of this handshake' });
    }

    // Update handshake with receiver info → matched
    await supabase
      .from('handshakes')
      .update({
        receiver_user_id: userId,
        receiver_wallet: walletAddress,
        status: 'matched',
      })
      .eq('id', handshakeId);

    // Build Solana transaction for receiver's 0.01 SOL fee
    const connection = new Connection(SOLANA_RPC, 'confirmed');
    const payerKey = new PublicKey(walletAddress);
    const treasuryKey = new PublicKey(TREASURY_WALLET);

    const transaction = new Transaction().add(
      SystemProgram.transfer({
        fromPubkey: payerKey,
        toPubkey: treasuryKey,
        lamports: MINT_FEE_LAMPORTS,
      })
    );

    const { blockhash, lastValidBlockHeight } = await connection.getLatestBlockhash('confirmed');
    transaction.recentBlockhash = blockhash;
    transaction.lastValidBlockHeight = lastValidBlockHeight;
    transaction.feePayer = payerKey;

    const serialized = transaction.serialize({
      requireAllSignatures: false,
      verifySignatures: false,
    });

    return res.status(200).json({
      handshakeId,
      status: 'matched',
      transaction: Buffer.from(serialized).toString('base64'),
      initiatorName: handshake.event_title
        ? `Handshake from ${handshake.event_title}`
        : 'Proof of Handshake',
    });
  } catch (error) {
    console.error('Handshake claim error:', error);
    return res.status(500).json({ error: 'Failed to claim handshake' });
  }
}
