-- ============================================================================
-- Security Audit 3: RLS WITH CHECK on UPDATE + SECURITY DEFINER fixes
-- Run this in Supabase SQL Editor
-- ============================================================================

-- M1: itineraries UPDATE policy missing WITH CHECK
-- Prevents a user from changing user_id on their own row
DROP POLICY IF EXISTS "Users can update their own itineraries" ON itineraries;
CREATE POLICY "Users can update their own itineraries"
  ON itineraries FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- M2: user_wallets UPDATE policy missing WITH CHECK
DROP POLICY IF EXISTS "Users can update own wallets" ON user_wallets;
CREATE POLICY "Users can update own wallets"
  ON user_wallets FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- M3: shared_itineraries UPDATE policy missing WITH CHECK
DROP POLICY IF EXISTS "Users can update their own share links" ON shared_itineraries;
CREATE POLICY "Users can update their own share links"
  ON shared_itineraries
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM itineraries
      WHERE itineraries.id = shared_itineraries.itinerary_id
      AND itineraries.user_id = auth.uid()
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM itineraries
      WHERE itineraries.id = shared_itineraries.itinerary_id
      AND itineraries.user_id = auth.uid()
    )
  );

-- M4: ai_conversations UPDATE policy missing WITH CHECK
DROP POLICY IF EXISTS "Users can update their own conversations" ON ai_conversations;
CREATE POLICY "Users can update their own conversations"
  ON ai_conversations FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- M5: Remove client-side INSERT on ai_usage (usage should be tracked server-side)
-- Users can manipulate their own usage tracking to bypass limits
DROP POLICY IF EXISTS "Users can insert their own usage" ON ai_usage;

-- M6: SECURITY DEFINER functions — enforce auth.uid() check
-- These functions accept any UUID, allowing cross-user probing

-- get_user_tier: restrict to own user_id
CREATE OR REPLACE FUNCTION get_user_tier(p_user_id UUID)
RETURNS TEXT AS $$
DECLARE
  v_tier TEXT;
BEGIN
  -- Only allow querying own tier (or service role which bypasses RLS)
  IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
    RAISE EXCEPTION 'Access denied: can only query own tier';
  END IF;

  SELECT tier INTO v_tier
  FROM subscriptions
  WHERE user_id = p_user_id
  AND status IN ('active', 'trialing')
  LIMIT 1;

  RETURN COALESCE(v_tier, 'free');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- check_ai_usage_limit: restrict to own user_id
CREATE OR REPLACE FUNCTION check_ai_usage_limit(p_user_id UUID, p_feature_type TEXT)
RETURNS BOOLEAN AS $$
DECLARE
  v_tier TEXT;
  v_usage_count INTEGER;
  v_limit INTEGER;
BEGIN
  IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
    RAISE EXCEPTION 'Access denied: can only check own usage';
  END IF;

  v_tier := get_user_tier(p_user_id);

  CASE v_tier
    WHEN 'free' THEN v_limit := 3;
    WHEN 'premium' THEN v_limit := 50;
    WHEN 'pro' THEN RETURN TRUE;
    ELSE v_limit := 0;
  END CASE;

  SELECT COUNT(*) INTO v_usage_count
  FROM ai_usage
  WHERE user_id = p_user_id
  AND feature_type = p_feature_type
  AND created_at >= date_trunc('month', CURRENT_TIMESTAMP)
  AND success = TRUE;

  RETURN v_usage_count < v_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- detect_abuse_patterns: restrict to own user_id
CREATE OR REPLACE FUNCTION detect_abuse_patterns(p_user_id UUID)
RETURNS TABLE(
  is_suspicious BOOLEAN,
  reason TEXT,
  action TEXT
) AS $$
DECLARE
  v_queries_last_hour INTEGER;
  v_queries_last_day INTEGER;
  v_failed_queries_percent DECIMAL;
  v_account_age INTERVAL;
BEGIN
  IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
    RAISE EXCEPTION 'Access denied: can only check own abuse patterns';
  END IF;

  SELECT COUNT(*) INTO v_queries_last_hour
  FROM ai_usage
  WHERE user_id = p_user_id
  AND created_at >= NOW() - INTERVAL '1 hour';

  SELECT COUNT(*) INTO v_queries_last_day
  FROM ai_usage
  WHERE user_id = p_user_id
  AND created_at >= NOW() - INTERVAL '24 hours';

  SELECT
    CASE
      WHEN COUNT(*) = 0 THEN 0
      ELSE (COUNT(*) FILTER (WHERE success = FALSE)::DECIMAL / COUNT(*) * 100)
    END INTO v_failed_queries_percent
  FROM ai_usage
  WHERE user_id = p_user_id
  AND created_at >= NOW() - INTERVAL '7 days';

  SELECT NOW() - created_at INTO v_account_age
  FROM auth.users
  WHERE id = p_user_id;

  IF v_queries_last_hour > 30 THEN
    RETURN QUERY SELECT TRUE, 'Excessive queries per hour: ' || v_queries_last_hour::TEXT, 'rate_limit';
  END IF;

  IF v_queries_last_day > 100 AND get_user_tier(p_user_id) = 'free' THEN
    RETURN QUERY SELECT TRUE, 'Excessive daily queries for free tier: ' || v_queries_last_day::TEXT, 'temp_block';
  END IF;

  IF v_failed_queries_percent > 50 AND v_queries_last_day > 10 THEN
    RETURN QUERY SELECT TRUE, 'High failure rate: ' || v_failed_queries_percent::TEXT || '%', 'review';
  END IF;

  IF v_account_age < INTERVAL '1 day' AND v_queries_last_day > 20 THEN
    RETURN QUERY SELECT TRUE, 'New account with high usage', 'review';
  END IF;

  RETURN QUERY SELECT FALSE, 'No suspicious activity', 'none';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- M7: get_user_total_points: restrict to own user_id
CREATE OR REPLACE FUNCTION get_user_total_points(p_user_id UUID)
RETURNS INTEGER AS $$
BEGIN
  IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
    RAISE EXCEPTION 'Access denied: can only query own points';
  END IF;

  RETURN (
    SELECT COALESCE(SUM(points), 0)::INTEGER
    FROM user_points
    WHERE user_id = p_user_id
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
