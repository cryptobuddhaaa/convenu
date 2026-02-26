/**
 * Consolidated auth endpoint.
 * POST /api/auth?action=telegram|wallet-login
 *
 * Replaces the individual auth/telegram.ts and auth/wallet-login.ts functions
 * to reduce Vercel Hobby plan function count.
 *
 * Note: auth/x.ts remains separate because X OAuth redirects to that exact path.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { handleTelegramAuth } from '../_lib/auth-telegram.js';
import { handleWalletLogin } from '../_lib/auth-wallet-login.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const action = String(req.query.action || '');

  switch (action) {
    case 'telegram':
      return handleTelegramAuth(req, res);
    case 'wallet-login':
      return handleWalletLogin(req, res);
    default:
      return res.status(400).json({ error: `Unknown auth action: ${action}` });
  }
}
