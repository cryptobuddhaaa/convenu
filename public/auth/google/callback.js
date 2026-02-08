// Google OAuth Callback Script
const statusEl = document.getElementById('status');
const messageEl = document.getElementById('message');
const debugEl = document.getElementById('debug');

function showDebug(info) {
  debugEl.style.display = 'block';
  debugEl.innerHTML = '<pre>' + JSON.stringify(info, null, 2) + '</pre>';
}

// Extract authorization code from URL
const urlParams = new URLSearchParams(window.location.search);
const code = urlParams.get('code');
const error = urlParams.get('error');

console.log('OAuth callback loaded', { code: code ? 'exists' : 'missing', error });

if (error) {
  statusEl.textContent = 'Connection Failed';
  messageEl.innerHTML = '<div class="error">Authorization was denied or cancelled.</div>';
  showDebug({ error, params: Object.fromEntries(urlParams) });

  // Redirect back after 3 seconds
  setTimeout(() => {
    const returnPath = sessionStorage.getItem('google_oauth_return_path') || '/';
    window.location.href = returnPath;
  }, 3000);
} else if (code) {
  console.log('Exchanging code for token...');

  // Exchange code for token
  fetch('/api/google-calendar/exchange-token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ code }),
  })
    .then((response) => {
      console.log('Exchange response:', response.status, response.statusText);
      if (!response.ok) {
        return response.text().then(text => {
          throw new Error(`Token exchange failed: ${response.status} - ${text}`);
        });
      }
      return response.json();
    })
    .then((data) => {
      console.log('Token exchange successful!', data);

      // Store tokens
      sessionStorage.setItem('google_calendar_access_token', data.accessToken);
      if (data.refreshToken) {
        sessionStorage.setItem('google_calendar_refresh_token', data.refreshToken);
      }

      // Set a flag to trigger import
      sessionStorage.setItem('google_calendar_connected', 'true');

      console.log('Tokens stored in sessionStorage');

      statusEl.textContent = 'Connected Successfully!';
      messageEl.textContent = 'Redirecting...';

      // Redirect back to the page they came from
      setTimeout(() => {
        const returnPath = sessionStorage.getItem('google_oauth_return_path') || '/';
        sessionStorage.removeItem('google_oauth_return_path');
        console.log('Redirecting to:', returnPath);
        window.location.href = returnPath;
      }, 1000);
    })
    .catch((error) => {
      console.error('Error during token exchange:', error);
      statusEl.textContent = 'Connection Failed';
      messageEl.innerHTML = '<div class="error">Failed to complete connection. Please try again.</div>';
      showDebug({
        error: error.message,
        code: code ? 'provided' : 'missing'
      });

      setTimeout(() => {
        const returnPath = sessionStorage.getItem('google_oauth_return_path') || '/';
        window.location.href = returnPath;
      }, 5000);
    });
} else {
  statusEl.textContent = 'Error';
  messageEl.innerHTML = '<div class="error">No authorization code received.</div>';
  showDebug({ params: Object.fromEntries(urlParams) });

  setTimeout(() => {
    const returnPath = sessionStorage.getItem('google_oauth_return_path') || '/';
    window.location.href = returnPath;
  }, 3000);
}
