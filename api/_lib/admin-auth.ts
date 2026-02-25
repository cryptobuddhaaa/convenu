/**
 * Admin authentication helper.
 * Verifies the user is authenticated AND present in the admin_users table.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import { getAuthUser, type AuthUser } from './auth.js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL || process.env.SUPABASE_URL || '',
  process.env.SUPABASE_SERVICE_ROLE_KEY || ''
);

export interface AdminUser extends AuthUser {
  role: 'admin' | 'super_admin';
}

/**
 * Verifies the request is from an authenticated admin user.
 * Returns the admin user if authorized, or null after sending an error response.
 */
export async function requireAdmin(req: VercelRequest, res: VercelResponse): Promise<AdminUser | null> {
  const user = await getAuthUser(req);
  if (!user) {
    res.status(401).json({ error: 'Authentication required' });
    return null;
  }

  const { data: adminRow } = await supabase
    .from('admin_users')
    .select('role')
    .eq('user_id', user.id)
    .single();

  if (!adminRow) {
    res.status(403).json({ error: 'Admin access required' });
    return null;
  }

  return { ...user, role: adminRow.role as 'admin' | 'super_admin' };
}
