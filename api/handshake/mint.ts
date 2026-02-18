/**
 * POST /api/handshake/mint
 * Mints soulbound compressed NFTs (cNFTs) for a matched handshake.
 * Called after both parties have paid. Uses Metaplex Bubblegum V2.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import { createUmi } from '@metaplex-foundation/umi-bundle-defaults';
import {
  mintV1,
  mplBubblegum,
} from '@metaplex-foundation/mpl-bubblegum';
import {
  createSignerFromKeypair,
  publicKey,
  generateSigner,
} from '@metaplex-foundation/umi';
import { base58 } from '@metaplex-foundation/umi/serializers';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL || process.env.SUPABASE_URL || '',
  process.env.SUPABASE_SERVICE_ROLE_KEY || ''
);

const SOLANA_RPC = process.env.VITE_SOLANA_RPC_URL || 'https://api.devnet.solana.com';
const TREE_KEYPAIR_BASE58 = process.env.HANDSHAKE_TREE_KEYPAIR || '';
const MERKLE_TREE_ADDRESS = process.env.HANDSHAKE_MERKLE_TREE || '';

const POINTS_PER_HANDSHAKE = 10;

async function mintCNFT(
  umi: ReturnType<typeof createUmi>,
  merkleTree: string,
  recipient: string,
  metadata: {
    name: string;
    uri: string;
    eventTitle: string;
    eventDate: string;
  }
) {
  const leafOwner = publicKey(recipient);

  const { signature } = await mintV1(umi, {
    leafOwner,
    merkleTree: publicKey(merkleTree),
    metadata: {
      name: metadata.name,
      uri: metadata.uri,
      sellerFeeBasisPoints: 0,
      collection: null,
      creators: [],
    },
  }).sendAndConfirm(umi);

  return base58.deserialize(signature)[0];
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { handshakeId } = req.body || {};
  if (!handshakeId) {
    return res.status(400).json({ error: 'handshakeId required' });
  }

  if (!TREE_KEYPAIR_BASE58 || !MERKLE_TREE_ADDRESS) {
    return res.status(500).json({ error: 'Merkle tree not configured' });
  }

  try {
    // Fetch matched handshake
    const { data: handshake, error: hsError } = await supabase
      .from('handshakes')
      .select('*')
      .eq('id', handshakeId)
      .single();

    if (hsError || !handshake) {
      return res.status(404).json({ error: 'Handshake not found' });
    }

    if (handshake.status === 'minted') {
      return res.status(409).json({ error: 'Already minted' });
    }

    if (handshake.status !== 'matched') {
      return res.status(400).json({ error: 'Handshake must be matched before minting' });
    }

    // Both sides must have paid
    if (!handshake.initiator_tx_signature || !handshake.receiver_tx_signature) {
      return res.status(400).json({ error: 'Both parties must pay before minting' });
    }

    // Initialize Umi with tree authority
    const umi = createUmi(SOLANA_RPC).use(mplBubblegum());

    const keypairBytes = base58.serialize(TREE_KEYPAIR_BASE58);
    const keypair = umi.eddsa.createKeypairFromSecretKey(keypairBytes);
    const signer = createSignerFromKeypair(umi, keypair);
    umi.identity = signer;
    umi.payer = signer;

    // Build metadata
    const eventInfo = handshake.event_title || 'Meeting';
    const eventDate = handshake.event_date || new Date().toISOString().split('T')[0];

    // Simple on-chain metadata (Arweave URI would be set up in production)
    const metadataUri = `https://arweave.net/placeholder-${handshakeId}`;

    // Mint cNFT for initiator
    const initiatorNftSig = await mintCNFT(umi, MERKLE_TREE_ADDRESS, handshake.initiator_wallet, {
      name: `Handshake: ${eventInfo}`,
      uri: metadataUri,
      eventTitle: eventInfo,
      eventDate,
    });

    // Mint cNFT for receiver
    const receiverNftSig = await mintCNFT(umi, MERKLE_TREE_ADDRESS, handshake.receiver_wallet, {
      name: `Handshake: ${eventInfo}`,
      uri: metadataUri,
      eventTitle: eventInfo,
      eventDate,
    });

    // Update handshake to minted
    await supabase
      .from('handshakes')
      .update({
        status: 'minted',
        initiator_nft_address: initiatorNftSig,
        receiver_nft_address: receiverNftSig,
        points_awarded: POINTS_PER_HANDSHAKE,
      })
      .eq('id', handshakeId);

    // Award points to both users
    const pointEntries = [
      {
        user_id: handshake.initiator_user_id,
        handshake_id: handshakeId,
        points: POINTS_PER_HANDSHAKE,
        reason: `Handshake: ${eventInfo}`,
      },
      {
        user_id: handshake.receiver_user_id,
        handshake_id: handshakeId,
        points: POINTS_PER_HANDSHAKE,
        reason: `Handshake: ${eventInfo}`,
      },
    ];

    await supabase.from('user_points').insert(pointEntries);

    // Recompute trust scores for both users
    for (const uid of [handshake.initiator_user_id, handshake.receiver_user_id]) {
      if (uid) {
        const { count } = await supabase
          .from('handshakes')
          .select('*', { count: 'exact', head: true })
          .eq('status', 'minted')
          .or(`initiator_user_id.eq.${uid},receiver_user_id.eq.${uid}`);

        await supabase
          .from('trust_scores')
          .update({
            total_handshakes: count || 0,
            updated_at: new Date().toISOString(),
          })
          .eq('user_id', uid);
      }
    }

    return res.status(200).json({
      status: 'minted',
      initiatorNft: initiatorNftSig,
      receiverNft: receiverNftSig,
      pointsAwarded: POINTS_PER_HANDSHAKE,
    });
  } catch (error) {
    console.error('Mint error:', error);
    return res.status(500).json({ error: 'Failed to mint handshake NFTs' });
  }
}
