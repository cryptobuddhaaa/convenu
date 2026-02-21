import { useCallback, useMemo } from 'react';
import { ConnectionProvider, WalletProvider } from '@solana/wallet-adapter-react';
import { WalletModalProvider } from '@solana/wallet-adapter-react-ui';
import { WalletError, WalletConnectionError } from '@solana/wallet-adapter-base';
import { PhantomWalletAdapter } from '@solana/wallet-adapter-phantom';
import { SolflareWalletAdapter } from '@solana/wallet-adapter-solflare';
import { clusterApiUrl } from '@solana/web3.js';

// Register Mobile Wallet Adapter for Android MWA support (only on Android devices)
import {
  registerMwa,
  createDefaultWalletNotFoundHandler,
  createDefaultAuthorizationCache,
  createDefaultChainSelector,
} from '@solana-mobile/wallet-standard-mobile';

if (typeof navigator !== 'undefined' && /android/i.test(navigator.userAgent)) {
  try {
    registerMwa({
      appIdentity: {
        name: 'Shareable Itinerary',
        uri: window.location.origin,
      },
      authorizationCache: createDefaultAuthorizationCache(),
      chains: [import.meta.env.VITE_SOLANA_NETWORK === 'mainnet-beta' ? 'solana:mainnet' as const : 'solana:devnet' as const],
      chainSelector: createDefaultChainSelector(),
      onWalletNotFound: createDefaultWalletNotFoundHandler(),
    });
  } catch {
    // MWA not available on this device
  }
}

import '@solana/wallet-adapter-react-ui/styles.css';

interface SolanaWalletProviderProps {
  children: React.ReactNode;
}

export function SolanaWalletProvider({ children }: SolanaWalletProviderProps) {
  const network = (import.meta.env.VITE_SOLANA_NETWORK as 'devnet' | 'mainnet-beta') || 'devnet';
  const endpoint = import.meta.env.VITE_SOLANA_RPC_URL || clusterApiUrl(network);

  // PhantomWalletAdapter: fallback for Phantom's iOS in-app browser where
  // Wallet Standard auto-detection has a timing race. Deduplicates automatically.
  //
  // SolflareWalletAdapter: EXCLUDED inside Solflare's own in-app browser because
  // its connect() imports @solflare-wallet/sdk which injects a fullscreen iframe
  // (z-index 99999, position: fixed, 100% width/height) from connect.solflare.com.
  // Inside Solflare's browser this iframe never resolves and blocks all touch events.
  // Solflare's browser injects window.solflare natively, which Wallet Standard detects.
  const isSolflareInApp = typeof window !== 'undefined' &&
    !!(window as unknown as Record<string, unknown>).solflare;
  const wallets = useMemo(
    () => isSolflareInApp
      ? [new PhantomWalletAdapter()]
      : [new PhantomWalletAdapter(), new SolflareWalletAdapter()],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  const onError = useCallback((error: WalletError) => {
    // Silently ignore autoConnect / silent-connect failures — the wallet adapter
    // retries on the next user-initiated connect anyway.
    if (error instanceof WalletConnectionError) {
      console.warn('[Wallet] Connection attempt failed:', error.message);
      return;
    }
    // Surface all other wallet errors normally
    console.error('[Wallet]', error.name, error.message);
  }, []);

  return (
    <ConnectionProvider endpoint={endpoint}>
      <WalletProvider wallets={wallets} autoConnect onError={onError}>
        <WalletModalProvider>
          {children}
        </WalletModalProvider>
      </WalletProvider>
    </ConnectionProvider>
  );
}
