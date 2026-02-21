-- Fix: Add SET search_path = '' to all functions (Supabase security lint)
-- Run this in the Supabase SQL Editor (Dashboard > SQL Editor)
--
-- Why: Functions without explicit search_path could theoretically be exploited
-- if a malicious actor creates shadow objects in an earlier schema.
-- Adding SET search_path = '' requires all table references to be fully qualified.

-- ============================================================================
-- 1. Trigger functions (no table references in body)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER
SET search_path = ''
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.update_contacts_updated_at()
RETURNS TRIGGER
SET search_path = ''
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.update_subscriptions_updated_at()
RETURNS TRIGGER
SET search_path = ''
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 2. Utility functions
-- ============================================================================

CREATE OR REPLACE FUNCTION public.generate_share_id()
RETURNS TEXT
SET search_path = ''
AS $$
DECLARE
  chars TEXT := 'abcdefghijklmnopqrstuvwxyz0123456789';
  result TEXT := '';
  random_bytes BYTEA;
  i INTEGER;
BEGIN
  random_bytes := gen_random_bytes(16);
  FOR i IN 0..15 LOOP
    result := result || substr(chars, (get_byte(random_bytes, i) % 36) + 1, 1);
  END LOOP;
  RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 3. User limit functions
-- ============================================================================

CREATE OR REPLACE FUNCTION public.check_user_limit()
RETURNS BOOLEAN
SET search_path = ''
AS $$
DECLARE
  user_count INTEGER;
BEGIN
  SELECT COUNT(DISTINCT user_id) INTO user_count
  FROM public.itineraries;

  RETURN user_count < 100;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.get_user_count()
RETURNS INTEGER
SET search_path = ''
AS $$
DECLARE
  user_count INTEGER;
BEGIN
  SELECT COUNT(DISTINCT user_id) INTO user_count
  FROM public.itineraries;

  RETURN user_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.enforce_user_limit()
RETURNS TRIGGER
SET search_path = ''
AS $$
DECLARE
  user_count INTEGER;
  is_new_user BOOLEAN;
BEGIN
  SELECT NOT EXISTS (
    SELECT 1 FROM public.itineraries
    WHERE user_id = NEW.user_id
  ) INTO is_new_user;

  IF is_new_user THEN
    SELECT COUNT(DISTINCT user_id) INTO user_count
    FROM public.itineraries;

    IF user_count >= 100 THEN
      RAISE EXCEPTION 'USER_LIMIT_REACHED:The app has reached its maximum capacity of 100 users. We are currently in beta and not accepting new users at this time.';
    END IF;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 4. Subscription / tier functions
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_user_tier(p_user_id UUID)
RETURNS TEXT
SET search_path = ''
AS $$
DECLARE
  v_tier TEXT;
BEGIN
  IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
    RAISE EXCEPTION 'Access denied: can only query own tier';
  END IF;

  SELECT tier INTO v_tier
  FROM public.subscriptions
  WHERE user_id = p_user_id
  AND status IN ('active', 'trialing')
  LIMIT 1;

  RETURN COALESCE(v_tier, 'free');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- 5. AI usage functions
-- ============================================================================

CREATE OR REPLACE FUNCTION public.check_ai_usage_limit(p_user_id UUID, p_feature_type TEXT)
RETURNS BOOLEAN
SET search_path = ''
AS $$
DECLARE
  v_tier TEXT;
  v_usage_count INTEGER;
  v_limit INTEGER;
BEGIN
  IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
    RAISE EXCEPTION 'Access denied: can only check own usage';
  END IF;

  v_tier := public.get_user_tier(p_user_id);

  CASE v_tier
    WHEN 'free' THEN v_limit := 3;
    WHEN 'premium' THEN v_limit := 50;
    WHEN 'pro' THEN RETURN TRUE;
    ELSE v_limit := 0;
  END CASE;

  SELECT COUNT(*) INTO v_usage_count
  FROM public.ai_usage
  WHERE user_id = p_user_id
  AND feature_type = p_feature_type
  AND created_at >= date_trunc('month', CURRENT_TIMESTAMP)
  AND success = TRUE;

  RETURN v_usage_count < v_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.get_remaining_ai_queries(p_user_id UUID)
RETURNS TABLE(
  tier TEXT,
  limit_value INTEGER,
  used INTEGER,
  remaining INTEGER
)
SET search_path = ''
AS $$
DECLARE
  v_tier TEXT;
  v_limit INTEGER;
  v_used INTEGER;
BEGIN
  v_tier := public.get_user_tier(p_user_id);

  CASE v_tier
    WHEN 'free' THEN v_limit := 3;
    WHEN 'premium' THEN v_limit := 50;
    WHEN 'pro' THEN v_limit := -1;
    ELSE v_limit := 0;
  END CASE;

  SELECT COUNT(*) INTO v_used
  FROM public.ai_usage
  WHERE user_id = p_user_id
  AND feature_type = 'event_creation'
  AND created_at >= date_trunc('month', CURRENT_TIMESTAMP)
  AND success = TRUE;

  RETURN QUERY SELECT
    v_tier,
    v_limit,
    v_used::INTEGER,
    CASE
      WHEN v_limit = -1 THEN -1
      ELSE GREATEST(0, v_limit - v_used)
    END::INTEGER;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.detect_abuse_patterns(p_user_id UUID)
RETURNS TABLE(
  is_suspicious BOOLEAN,
  reason TEXT,
  action TEXT
)
SET search_path = ''
AS $$
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
  FROM public.ai_usage
  WHERE user_id = p_user_id
  AND created_at >= NOW() - INTERVAL '1 hour';

  SELECT COUNT(*) INTO v_queries_last_day
  FROM public.ai_usage
  WHERE user_id = p_user_id
  AND created_at >= NOW() - INTERVAL '24 hours';

  SELECT
    CASE
      WHEN COUNT(*) = 0 THEN 0
      ELSE (COUNT(*) FILTER (WHERE success = FALSE)::DECIMAL / COUNT(*) * 100)
    END INTO v_failed_queries_percent
  FROM public.ai_usage
  WHERE user_id = p_user_id
  AND created_at >= NOW() - INTERVAL '7 days';

  SELECT NOW() - created_at INTO v_account_age
  FROM auth.users
  WHERE id = p_user_id;

  IF v_queries_last_hour > 30 THEN
    RETURN QUERY SELECT TRUE, 'Excessive queries per hour: ' || v_queries_last_hour::TEXT, 'rate_limit';
  END IF;

  IF v_queries_last_day > 100 AND public.get_user_tier(p_user_id) = 'free' THEN
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

-- ============================================================================
-- 6. Points function
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_user_total_points(p_user_id UUID)
RETURNS INTEGER
SET search_path = ''
AS $$
BEGIN
  IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
    RAISE EXCEPTION 'Access denied: can only query own points';
  END IF;

  RETURN (
    SELECT COALESCE(SUM(points), 0)::INTEGER
    FROM public.user_points
    WHERE user_id = p_user_id
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
