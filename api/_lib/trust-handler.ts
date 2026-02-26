/**
 * Trust score computation handler.
 * Routed via /api/profile?action=compute-trust
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import { requireAuth } from './auth.js';
import { enrichWalletData } from './wallet-enrich.js';
import { estimateTelegramAccountAgeDays } from './telegram-age.js';
import { computeTrustCategories } from './trust-categories.js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL || process.env.SUPABASE_URL || '',
  process.env.SUPABASE_SERVICE_ROLE_KEY || ''
);

const X_CLIENT_ID = process.env.X_CLIENT_ID || '';
const X_CLIENT_SECRET = process.env.X_CLIENT_SECRET || '';

export async function handleComputeTrust(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const authUser = await requireAuth(req, res);
    if (!authUser) return;
    const userId = authUser.id;

    // Fetch existing trust data
    const { data: existing } = await supabase
      .from('trust_scores')
      .select('*')
      .eq('user_id', userId)
      .single();

    // Fetch wallet data
    const { data: wallets } = await supabase
      .from('user_wallets')
      .select('verified_at')
      .eq('user_id', userId);

    const verifiedWallet = wallets?.find((w: { verified_at: string | null }) => w.verified_at != null);
    const walletConnected = !!verifiedWallet;

    // Enrich wallet data from Solana RPC (age, tx count, token holdings)
    let walletAgeDays = existing?.wallet_age_days as number | null;
    let walletTxCount = existing?.wallet_tx_count as number | null;
    let walletHasTokens = existing?.wallet_has_tokens || false;

    if (walletConnected && verifiedWallet) {
      // Fetch wallet address for RPC enrichment
      const { data: walletRow } = await supabase
        .from('user_wallets')
        .select('wallet_address')
        .eq('user_id', userId)
        .not('verified_at', 'is', null)
        .limit(1)
        .single();

      if (walletRow?.wallet_address) {
        try {
          const enrichment = await enrichWalletData(walletRow.wallet_address);
          walletAgeDays = enrichment.walletAgeDays;
          walletTxCount = enrichment.walletTxCount;
          walletHasTokens = enrichment.walletHasTokens;
        } catch (err) {
          console.error('Wallet enrichment failed, using cached values:', err);
        }
      }
    }

    // Fetch linked Telegram user ID (used for account age + profile photo)
    const { data: tgLink } = await supabase
      .from('telegram_links')
      .select('telegram_user_id')
      .eq('user_id', userId)
      .single();

    // Estimate Telegram account age if not yet computed
    let telegramAccountAgeDays = existing?.telegram_account_age_days as number | null;
    if (telegramAccountAgeDays == null && tgLink?.telegram_user_id) {
      telegramAccountAgeDays = estimateTelegramAccountAgeDays(tgLink.telegram_user_id);
    }

    // Count completed handshakes
    const { count: handshakeCount } = await supabase
      .from('handshakes')
      .select('*', { count: 'exact', head: true })
      .eq('status', 'minted')
      .or(`initiator_user_id.eq.${userId},receiver_user_id.eq.${userId}`);

    const totalHandshakes = handshakeCount || 0;

    // Re-verify X connection and check premium status if we have a stored refresh token
    let xVerified = existing?.x_verified || false;
    let xPremium = existing?.x_premium || false;
    if (xVerified && existing?.x_refresh_token && X_CLIENT_ID && X_CLIENT_SECRET) {
      try {
        const refreshRes = await fetch('https://api.x.com/2/oauth2/token', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            Authorization: `Basic ${Buffer.from(`${X_CLIENT_ID}:${X_CLIENT_SECRET}`).toString('base64')}`,
          },
          body: new URLSearchParams({
            grant_type: 'refresh_token',
            refresh_token: existing.x_refresh_token as string,
          }),
        });

        if (refreshRes.ok) {
          const refreshData = await refreshRes.json() as { access_token?: string; refresh_token?: string };
          // Store rotated refresh token
          if (refreshData.refresh_token) {
            await supabase
              .from('trust_scores')
              .update({ x_refresh_token: refreshData.refresh_token })
              .eq('user_id', userId);
          }
          // Use the new access token to check premium status
          // Use verified_type instead of legacy verified field — verified_type === 'blue' is X Premium only
          if (refreshData.access_token) {
            try {
              const userRes = await fetch('https://api.x.com/2/users/me?user.fields=verified_type', {
                headers: { Authorization: `Bearer ${refreshData.access_token}` },
              });
              if (userRes.ok) {
                const userData = await userRes.json() as { data?: { verified_type?: string } };
                xPremium = userData.data?.verified_type === 'blue';
              }
            } catch {
              // Non-fatal — keep existing xPremium value
            }
          }
        } else {
          // Token refresh failed — user likely revoked the app on X
          console.log(`X token refresh failed for user ${userId}, marking x_verified=false`);
          xVerified = false;
          xPremium = false;
          await supabase
            .from('trust_scores')
            .update({ x_refresh_token: null })
            .eq('user_id', userId);
        }
      } catch (err) {
        // Network error — keep existing values (don't penalize for transient failures)
        console.error('X re-verification network error:', err);
      }
    }

    // Gather all signals
    const signals = {
      totalHandshakes,
      walletConnected,
      walletAgeDays,
      walletTxCount,
      walletHasTokens,
      telegramPremium: existing?.telegram_premium || false,
      hasUsername: existing?.has_username || false,
      telegramAccountAgeDays,
      xVerified,
      xPremium,
    };

    const scores = computeTrustCategories(signals);

    const trustData = {
      user_id: userId,
      telegram_premium: signals.telegramPremium,
      has_username: signals.hasUsername,
      telegram_account_age_days: telegramAccountAgeDays,
      wallet_connected: walletConnected,
      wallet_age_days: walletAgeDays,
      wallet_tx_count: walletTxCount,
      wallet_has_tokens: walletHasTokens,
      x_verified: signals.xVerified,
      x_premium: signals.xPremium,
      total_handshakes: totalHandshakes,
      trust_score: scores.trustScore,
      score_handshakes: scores.scoreHandshakes,
      score_wallet: scores.scoreWallet,
      score_socials: scores.scoreSocials,
      score_events: scores.scoreEvents,
      score_community: scores.scoreCommunity,
      trust_level: scores.trustLevel,
      updated_at: new Date().toISOString(),
    };

    await supabase.from('trust_scores').upsert(trustData, { onConflict: 'user_id' });

    return res.status(200).json({
      trustScore: scores.trustScore,
      scoreHandshakes: scores.scoreHandshakes,
      scoreWallet: scores.scoreWallet,
      scoreSocials: scores.scoreSocials,
      scoreEvents: scores.scoreEvents,
      scoreCommunity: scores.scoreCommunity,
      totalHandshakes,
      walletConnected,
    });
  } catch (error) {
    console.error('Trust compute error:', error);
    return res.status(500).json({ error: 'Failed to compute trust score' });
  }
}
