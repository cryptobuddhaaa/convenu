-- ============================================================================
-- Admin Users Table
-- Whitelist of users who can access the admin dashboard.
-- Only super_admins can add new admins (done via SQL or service role key).
-- ============================================================================

CREATE TABLE IF NOT EXISTS admin_users (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  role TEXT NOT NULL DEFAULT 'admin' CHECK (role IN ('admin', 'super_admin')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL
);

ALTER TABLE admin_users ENABLE ROW LEVEL SECURITY;

-- Admins can read their own row (to check if they're an admin on the client side)
CREATE POLICY "Users can check own admin status"
  ON admin_users FOR SELECT
  USING (auth.uid() = user_id);

-- No INSERT/UPDATE/DELETE policies for regular users.
-- All management is done server-side with service role key.
