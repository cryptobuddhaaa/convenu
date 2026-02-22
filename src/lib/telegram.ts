/**
 * Centralized Telegram mini-app detection and helpers.
 * All components should import from here instead of duplicating detection logic.
 */

/** Detects if the app is running inside Telegram's WebView (mini app). */
export function isTelegramWebApp(): boolean {
  if (typeof window === 'undefined') return false;
  return !!(window as unknown as Record<string, unknown>).TelegramWebviewProxy
    || location.hash.includes('tgWebAppData');
}

/** Access Telegram WebApp API if available. */
function getTelegramWebApp(): { openTelegramLink?: (url: string) => void; openLink?: (url: string) => void } | undefined {
  const tg = (window as unknown as Record<string, { WebApp?: Record<string, unknown> }>).Telegram;
  return tg?.WebApp as { openTelegramLink?: (url: string) => void; openLink?: (url: string) => void } | undefined;
}

/**
 * Opens a Telegram deep link (t.me/...) using Telegram's native API if available,
 * otherwise falls back to window.open.
 */
export function openTelegramLink(url: string): void {
  const webapp = getTelegramWebApp();
  if (webapp?.openTelegramLink) {
    webapp.openTelegramLink(url);
  } else {
    window.open(url, '_blank');
  }
}

/**
 * Opens an external (non-Telegram) link using Telegram's native API if available,
 * otherwise falls back to window.open.
 */
export function openExternalLink(url: string): void {
  const webapp = getTelegramWebApp();
  if (webapp?.openLink) {
    webapp.openLink(url);
  } else {
    window.open(url, '_blank');
  }
}
