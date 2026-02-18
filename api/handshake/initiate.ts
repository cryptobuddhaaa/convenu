/**
 * POST /api/handshake/initiate
 * Creates a pending handshake and returns a transaction to sign (0.01 SOL fee).
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
const MINT_FEE_LAMPORTS = 0.01 * LAMPORTS_PER_SOL; // 10,000,000 lamports

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { userId, contactId, walletAddress } = req.body || {};

  if (!userId || !contactId || !walletAddress) {
    return res.status(400).json({ error: 'userId, contactId, and walletAddress required' });
  }

  if (!TREASURY_WALLET) {
    return res.status(500).json({ error: 'Treasury wallet not configured' });
  }

  try {
    // Fetch the contact to get identifier and event info
    const { data: contact, error: contactError } = await supabase
      .from('contacts')
      .select('id, first_name, last_name, telegram_handle, email, event_id, event_title, date_met')
      .eq('id', contactId)
      .eq('user_id', userId)
      .single();

    if (contactError || !contact) {
      return res.status(404).json({ error: 'Contact not found' });
    }

    // Determine receiver identifier (prefer telegram, fall back to email)
    const receiverIdentifier = contact.telegram_handle || contact.email;
    if (!receiverIdentifier) {
      return res.status(400).json({ error: 'Contact must have a Telegram handle or email for handshake' });
    }

    // Check for existing pending/minted handshake with this contact
    const { data: existing } = await supabase
      .from('handshakes')
      .select('id, status')
      .eq('initiator_user_id', userId)
      .eq('contact_id', contactId)
      .in('status', ['pending', 'matched', 'minted'])
      .single();

    if (existing) {
      return res.status(409).json({
        error: 'Handshake already exists for this contact',
        handshakeId: existing.id,
        status: existing.status,
      });
    }

    // Create the handshake record
    const { data: handshake, error: hsError } = await supabase
      .from('handshakes')
      .insert({
        initiator_user_id: userId,
        receiver_identifier: receiverIdentifier,
        contact_id: contactId,
        event_id: contact.event_id,
        event_title: contact.event_title,
        event_date: contact.date_met,
        initiator_wallet: walletAddress,
        mint_fee_lamports: MINT_FEE_LAMPORTS,
        status: 'pending',
      })
      .select('id')
      .single();

    if (hsError || !handshake) {
      console.error('Error creating handshake:', hsError);
      return res.status(500).json({ error: 'Failed to create handshake' });
    }

    // Build a Solana transaction: transfer 0.01 SOL to treasury
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

    // Serialize the transaction (unsigned) for the client to sign
    const serialized = transaction.serialize({
      requireAllSignatures: false,
      verifySignatures: false,
    });

    return res.status(200).json({
      handshakeId: handshake.id,
      transaction: Buffer.from(serialized).toString('base64'),
      receiverIdentifier,
      contactName: `${contact.first_name} ${contact.last_name}`,
    });
  } catch (error) {
    console.error('Handshake initiation error:', error);
    return res.status(500).json({ error: 'Failed to initiate handshake' });
  }
}
