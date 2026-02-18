/**
 * POST /api/handshake/confirm-tx
 * Confirms a signed transaction for either initiator or receiver side,
 * submits it to Solana, and updates handshake state.
 * When both sides have paid, triggers minting.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import { Connection, Transaction } from '@solana/web3.js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL || process.env.SUPABASE_URL || '',
  process.env.SUPABASE_SERVICE_ROLE_KEY || ''
);

const SOLANA_RPC = process.env.VITE_SOLANA_RPC_URL || 'https://api.devnet.solana.com';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { handshakeId, signedTransaction, side } = req.body || {};

  if (!handshakeId || !signedTransaction || !['initiator', 'receiver'].includes(side)) {
    return res.status(400).json({ error: 'handshakeId, signedTransaction, and side (initiator|receiver) required' });
  }

  try {
    const { data: handshake, error: hsError } = await supabase
      .from('handshakes')
      .select('*')
      .eq('id', handshakeId)
      .single();

    if (hsError || !handshake) {
      return res.status(404).json({ error: 'Handshake not found' });
    }

    // Submit the signed transaction to Solana
    const connection = new Connection(SOLANA_RPC, 'confirmed');
    const txBuffer = Buffer.from(signedTransaction, 'base64');
    const transaction = Transaction.from(txBuffer);

    const txSignature = await connection.sendRawTransaction(transaction.serialize(), {
      skipPreflight: false,
      preflightCommitment: 'confirmed',
    });

    // Wait for confirmation
    const latestBlockhash = await connection.getLatestBlockhash('confirmed');
    await connection.confirmTransaction({
      signature: txSignature,
      blockhash: latestBlockhash.blockhash,
      lastValidBlockHeight: latestBlockhash.lastValidBlockHeight,
    }, 'confirmed');

    // Update the handshake record
    const now = new Date().toISOString();
    const updateFields: Record<string, unknown> = {};

    if (side === 'initiator') {
      updateFields.initiator_tx_signature = txSignature;
      updateFields.initiator_minted_at = now;
    } else {
      updateFields.receiver_tx_signature = txSignature;
      updateFields.receiver_minted_at = now;
    }

    await supabase
      .from('handshakes')
      .update(updateFields)
      .eq('id', handshakeId);

    // Check if both sides have now paid — if so, mark as ready for minting
    const { data: updated } = await supabase
      .from('handshakes')
      .select('initiator_tx_signature, receiver_tx_signature, status')
      .eq('id', handshakeId)
      .single();

    const bothPaid = updated?.initiator_tx_signature && updated?.receiver_tx_signature;

    return res.status(200).json({
      txSignature,
      side,
      bothPaid: !!bothPaid,
      status: updated?.status,
    });
  } catch (error) {
    console.error('Confirm tx error:', error);
    return res.status(500).json({ error: 'Failed to confirm transaction' });
  }
}
