/**
 * Pure trust score computation function.
 * Shared by: auth/telegram handler, _lib/trust-recompute, _lib/trust-handler,
 * and telegram/_flows/account.
 *
 * Extracted from trust/compute.ts to allow the handler to be consolidated
 * into profile/index.ts without breaking cross-module imports.
 */

/** Scoring constants */
const MAX_HANDSHAKES = 30;
const MAX_WALLET = 20;
const MAX_SOCIALS = 20;

export function computeTrustCategories(signals: {
  totalHandshakes: number;
  walletConnected: boolean;
  walletAgeDays: number | null;
  walletTxCount: number | null;
  walletHasTokens: boolean;
  telegramPremium: boolean;
  hasUsername: boolean;
  telegramAccountAgeDays: number | null;
  xVerified: boolean;
  xPremium: boolean;
}) {
  // --- Handshakes (max 30): 1 point per minted handshake ---
  const scoreHandshakes = Math.min(MAX_HANDSHAKES, signals.totalHandshakes);

  // --- Wallet (max 20) ---
  let scoreWallet = 0;
  if (signals.walletConnected) scoreWallet += 5;
  if (signals.walletAgeDays != null && signals.walletAgeDays > 90) scoreWallet += 5;
  if (signals.walletTxCount != null && signals.walletTxCount > 10) scoreWallet += 5;
  if (signals.walletHasTokens) scoreWallet += 5;
  scoreWallet = Math.min(MAX_WALLET, scoreWallet);

  // --- Socials (max 20): 4 points each, 5 signals ---
  let scoreSocials = 0;
  if (signals.telegramPremium) scoreSocials += 4;
  if (signals.hasUsername) scoreSocials += 4;
  if (signals.telegramAccountAgeDays != null && signals.telegramAccountAgeDays > 365) scoreSocials += 4;
  if (signals.xVerified) scoreSocials += 4;
  if (signals.xPremium) scoreSocials += 4;
  scoreSocials = Math.min(MAX_SOCIALS, scoreSocials);

  // --- Events (max 20): TBD — placeholder, always 0 for now ---
  const scoreEvents = 0;

  // --- Community (max 10): TBD — placeholder, always 0 for now ---
  const scoreCommunity = 0;

  const trustScore = scoreHandshakes + scoreWallet + scoreSocials + scoreEvents + scoreCommunity;

  // Legacy 1-5 mapping (approximate: 0-100 → 1-5)
  let trustLevel: number;
  if (trustScore >= 60) trustLevel = 5;
  else if (trustScore >= 40) trustLevel = 4;
  else if (trustScore >= 25) trustLevel = 3;
  else if (trustScore >= 10) trustLevel = 2;
  else trustLevel = 1;

  return {
    trustScore,
    scoreHandshakes,
    scoreWallet,
    scoreSocials,
    scoreEvents,
    scoreCommunity,
    trustLevel,
  };
}
