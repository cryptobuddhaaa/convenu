/**
 * Consolidated calendar/Luma endpoint.
 * /api/calendar?action=fetch-luma|exchange-token|luma-events
 *
 * Replaces the individual fetch-luma.ts, google-calendar/exchange-token.ts,
 * and google-calendar/luma-events.ts functions to reduce Vercel Hobby plan
 * function count.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { handleFetchLuma } from '../_lib/fetch-luma.js';
import { handleExchangeToken } from '../_lib/calendar-exchange.js';
import { handleLumaEvents } from '../_lib/calendar-luma.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const action = String(req.query.action || '');

  switch (action) {
    case 'fetch-luma':
      return handleFetchLuma(req, res);
    case 'exchange-token':
      return handleExchangeToken(req, res);
    case 'luma-events':
      return handleLumaEvents(req, res);
    default:
      return res.status(400).json({ error: `Unknown calendar action: ${action}` });
  }
}
