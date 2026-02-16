import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import type { User, Session } from '@supabase/supabase-js';
import { supabase } from '../lib/supabase';
import { toast } from '../components/Toast';

interface AuthContextType {
  user: User | null;
  session: Session | null;
  loading: boolean;
  isTelegramMiniApp: boolean;
  signInWithGoogle: () => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  session: null,
  loading: true,
  isTelegramMiniApp: false,
  signInWithGoogle: async () => {},
  signOut: async () => {},
});

export const useAuth = () => useContext(AuthContext);

/** Check if we're running inside a Telegram Mini App */
function getTelegramInitData(): string | null {
  try {
    const tg = window.Telegram?.WebApp;
    if (tg && tg.initData && tg.initData.length > 0) {
      return tg.initData;
    }
  } catch {
    // Not in Telegram context
  }
  return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [isTelegramMiniApp] = useState(() => getTelegramInitData() !== null);

  useEffect(() => {
    let cancelled = false;

    async function initAuth() {
      // First, check for existing Supabase session
      const { data: { session: existingSession } } = await supabase.auth.getSession();

      if (existingSession) {
        if (!cancelled) {
          setSession(existingSession);
          setUser(existingSession.user);
          setLoading(false);
        }
        return;
      }

      // No existing session — check if we're in a Telegram Mini App
      const initData = getTelegramInitData();
      if (initData) {
        try {
          const response = await fetch('/api/auth/telegram', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ initData }),
          });

          if (!response.ok) {
            const errData = await response.json();
            console.error('Telegram auth failed:', errData);
            toast.error('Telegram authentication failed');
            if (!cancelled) setLoading(false);
            return;
          }

          const { token_hash } = await response.json();

          // Verify the OTP token to establish a real Supabase session
          const { error: otpError } = await supabase.auth.verifyOtp({
            token_hash,
            type: 'magiclink',
          });

          if (otpError) {
            console.error('OTP verification failed:', otpError);
            toast.error('Failed to establish session');
          }
          // onAuthStateChange will pick up the new session
        } catch (err) {
          console.error('Telegram auth error:', err);
          toast.error('Telegram authentication failed');
        }
      }

      if (!cancelled) {
        setLoading(false);
      }
    }

    initAuth();

    // Listen for auth changes (covers both Google OAuth redirect and Telegram OTP verification)
    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      if (!cancelled) {
        setSession(session);
        setUser(session?.user ?? null);
        setLoading(false);
      }
    });

    return () => {
      cancelled = true;
      subscription.unsubscribe();
    };
  }, []);

  const signInWithGoogle = async () => {
    const { error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: window.location.origin,
      },
    });
    if (error) {
      console.error('Error signing in with Google:', error.message);
      toast.error('Failed to sign in with Google. Please try again.');
    }
  };

  const signOut = async () => {
    const { error } = await supabase.auth.signOut();
    if (error) {
      console.error('Error signing out:', error.message);
      toast.error('Failed to sign out. Please try again.');
    }
    // If in Telegram Mini App, close the app on sign out
    if (isTelegramMiniApp) {
      try {
        window.Telegram?.WebApp.close();
      } catch {
        // Ignore if close fails
      }
    }
  };

  const value = {
    user,
    session,
    loading,
    isTelegramMiniApp,
    signInWithGoogle,
    signOut,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
