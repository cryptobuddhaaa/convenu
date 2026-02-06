import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // Log API key status for debugging (first 10 chars only)
  console.log('Claude API Key loaded:', env.CLAUDE_API_KEY ? `${env.CLAUDE_API_KEY.substring(0, 15)}...` : 'NOT FOUND');

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api/claude': {
          target: 'https://api.anthropic.com',
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path.replace(/^\/api\/claude/, '/v1/messages'),
          configure: (proxy, _options) => {
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log('Proxying request to Claude API...');

              // Get the body from the request to re-send it
              if (req.body) {
                const bodyData = JSON.stringify(req.body);
                proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
                proxyReq.write(bodyData);
              }

              // Add required headers for Claude API
              proxyReq.setHeader('x-api-key', env.CLAUDE_API_KEY || '');
              proxyReq.setHeader('anthropic-version', '2023-06-01');
              proxyReq.setHeader('Content-Type', 'application/json');

              // Remove browser-specific headers that might cause issues
              proxyReq.removeHeader('origin');
              proxyReq.removeHeader('referer');
            });

            proxy.on('proxyRes', (proxyRes, req, res) => {
              console.log('Received response from Claude API:', proxyRes.statusCode);
            });

            proxy.on('error', (err, req, res) => {
              console.error('Proxy error:', err);
            });
          },
        },
      },
    },
  }
})
