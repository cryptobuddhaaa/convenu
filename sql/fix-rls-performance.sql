-- Fix: RLS performance warnings (auth_rls_initplan + multiple_permissive_policies)
-- Run this in the Supabase SQL Editor (Dashboard > SQL Editor)
--
-- Changes:
-- 1. Wrap auth.uid() in (select auth.uid()) so it's evaluated once per query, not per row
-- 2. Drop duplicate SELECT policy on ai_usage
-- 3. Drop stale "Anyone can view shared itineraries" policy on itineraries table
--    (public read access is correctly on shared_itineraries table, not itineraries)

-- ============================================================================
-- ITINERARIES
-- ============================================================================

-- Drop stale policy: public read access should only be on shared_itineraries, not itineraries
DROP POLICY IF EXISTS "Anyone can view shared itineraries" ON public.itineraries;

DROP POLICY IF EXISTS "Users can view their own itineraries" ON public.itineraries;
CREATE POLICY "Users can view their own itineraries"
  ON public.itineraries FOR SELECT
  USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can create their own itineraries" ON public.itineraries;
CREATE POLICY "Users can create their own itineraries"
  ON public.itineraries FOR INSERT
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can update their own itineraries" ON public.itineraries;
CREATE POLICY "Users can update their own itineraries"
  ON public.itineraries FOR UPDATE
  USING ((select auth.uid()) = user_id)
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete their own itineraries" ON public.itineraries;
CREATE POLICY "Users can delete their own itineraries"
  ON public.itineraries FOR DELETE
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- CONTACTS
-- ============================================================================

DROP POLICY IF EXISTS "Users can read own contacts" ON public.contacts;
CREATE POLICY "Users can read own contacts"
  ON public.contacts FOR SELECT
  USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can insert own contacts" ON public.contacts;
CREATE POLICY "Users can insert own contacts"
  ON public.contacts FOR INSERT
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can update own contacts" ON public.contacts;
CREATE POLICY "Users can update own contacts"
  ON public.contacts FOR UPDATE
  USING ((select auth.uid()) = user_id)
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete own contacts" ON public.contacts;
CREATE POLICY "Users can delete own contacts"
  ON public.contacts FOR DELETE
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- SHARED_ITINERARIES
-- ============================================================================

-- "Anyone can view" stays as-is (USING (true)) — no auth.uid() call to fix

DROP POLICY IF EXISTS "Users can share their own itineraries" ON public.shared_itineraries;
CREATE POLICY "Users can share their own itineraries"
  ON public.shared_itineraries FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.itineraries
      WHERE itineraries.id = shared_itineraries.itinerary_id
      AND itineraries.user_id = (select auth.uid())
    )
  );

DROP POLICY IF EXISTS "Users can delete their own share links" ON public.shared_itineraries;
CREATE POLICY "Users can delete their own share links"
  ON public.shared_itineraries FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM public.itineraries
      WHERE itineraries.id = shared_itineraries.itinerary_id
      AND itineraries.user_id = (select auth.uid())
    )
  );

DROP POLICY IF EXISTS "Users can update their own share links" ON public.shared_itineraries;
CREATE POLICY "Users can update their own share links"
  ON public.shared_itineraries FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM public.itineraries
      WHERE itineraries.id = shared_itineraries.itinerary_id
      AND itineraries.user_id = (select auth.uid())
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.itineraries
      WHERE itineraries.id = shared_itineraries.itinerary_id
      AND itineraries.user_id = (select auth.uid())
    )
  );

-- ============================================================================
-- SUBSCRIPTIONS
-- ============================================================================

DROP POLICY IF EXISTS "Users can view their own subscription" ON public.subscriptions;
CREATE POLICY "Users can view their own subscription"
  ON public.subscriptions FOR SELECT
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- AI_USAGE — also fix duplicate SELECT policy
-- ============================================================================

-- Drop the duplicate/overlapping policy
DROP POLICY IF EXISTS "Users can manage own AI usage" ON public.ai_usage;

DROP POLICY IF EXISTS "Users can view their own usage" ON public.ai_usage;
CREATE POLICY "Users can view their own usage"
  ON public.ai_usage FOR SELECT
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- AI_CONVERSATIONS
-- ============================================================================

DROP POLICY IF EXISTS "Users can view their own conversations" ON public.ai_conversations;
CREATE POLICY "Users can view their own conversations"
  ON public.ai_conversations FOR SELECT
  USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can insert their own conversations" ON public.ai_conversations;
CREATE POLICY "Users can insert their own conversations"
  ON public.ai_conversations FOR INSERT
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can update their own conversations" ON public.ai_conversations;
CREATE POLICY "Users can update their own conversations"
  ON public.ai_conversations FOR UPDATE
  USING ((select auth.uid()) = user_id)
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete their own conversations" ON public.ai_conversations;
CREATE POLICY "Users can delete their own conversations"
  ON public.ai_conversations FOR DELETE
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- TELEGRAM_LINKS
-- ============================================================================

DROP POLICY IF EXISTS "Users can view own telegram link" ON public.telegram_links;
CREATE POLICY "Users can view own telegram link"
  ON public.telegram_links FOR SELECT
  USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete own telegram link" ON public.telegram_links;
CREATE POLICY "Users can delete own telegram link"
  ON public.telegram_links FOR DELETE
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- TELEGRAM_LINK_CODES
-- ============================================================================

DROP POLICY IF EXISTS "Users can insert own link codes" ON public.telegram_link_codes;
CREATE POLICY "Users can insert own link codes"
  ON public.telegram_link_codes FOR INSERT
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can view own link codes" ON public.telegram_link_codes;
CREATE POLICY "Users can view own link codes"
  ON public.telegram_link_codes FOR SELECT
  USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete own link codes" ON public.telegram_link_codes;
CREATE POLICY "Users can delete own link codes"
  ON public.telegram_link_codes FOR DELETE
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- USER_TAGS
-- ============================================================================

DROP POLICY IF EXISTS "Users can manage their own tags" ON public.user_tags;
CREATE POLICY "Users can manage their own tags"
  ON public.user_tags FOR ALL
  USING ((select auth.uid()) = user_id)
  WITH CHECK ((select auth.uid()) = user_id);

-- ============================================================================
-- CONTACT_NOTES
-- ============================================================================

DROP POLICY IF EXISTS "Users can manage their own contact notes" ON public.contact_notes;
CREATE POLICY "Users can manage their own contact notes"
  ON public.contact_notes FOR ALL
  USING ((select auth.uid()) = user_id)
  WITH CHECK ((select auth.uid()) = user_id);

-- ============================================================================
-- USER_WALLETS
-- ============================================================================

DROP POLICY IF EXISTS "Users can read own wallets" ON public.user_wallets;
CREATE POLICY "Users can read own wallets"
  ON public.user_wallets FOR SELECT
  USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can insert own wallets" ON public.user_wallets;
CREATE POLICY "Users can insert own wallets"
  ON public.user_wallets FOR INSERT
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can update own wallets" ON public.user_wallets;
CREATE POLICY "Users can update own wallets"
  ON public.user_wallets FOR UPDATE
  USING ((select auth.uid()) = user_id)
  WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete own wallets" ON public.user_wallets;
CREATE POLICY "Users can delete own wallets"
  ON public.user_wallets FOR DELETE
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- HANDSHAKES
-- ============================================================================

DROP POLICY IF EXISTS "Users can read own handshakes" ON public.handshakes;
CREATE POLICY "Users can read own handshakes"
  ON public.handshakes FOR SELECT
  USING (
    (select auth.uid()) = initiator_id
    OR (select auth.uid()) = receiver_id
  );

DROP POLICY IF EXISTS "Users can create handshakes" ON public.handshakes;
CREATE POLICY "Users can create handshakes"
  ON public.handshakes FOR INSERT
  WITH CHECK ((select auth.uid()) = initiator_id);

-- ============================================================================
-- USER_POINTS
-- ============================================================================

DROP POLICY IF EXISTS "Users can read own points" ON public.user_points;
CREATE POLICY "Users can read own points"
  ON public.user_points FOR SELECT
  USING ((select auth.uid()) = user_id);

-- ============================================================================
-- TRUST_SCORES
-- ============================================================================

DROP POLICY IF EXISTS "Users can read own trust score" ON public.trust_scores;
CREATE POLICY "Users can read own trust score"
  ON public.trust_scores FOR SELECT
  USING ((select auth.uid()) = user_id);
