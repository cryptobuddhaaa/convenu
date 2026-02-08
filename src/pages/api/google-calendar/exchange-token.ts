/**
 * API Endpoint: Exchange Google OAuth code for access token
 * POST /api/google-calendar/exchange-token
 */

import type { APIRoute } from 'astro';

export const POST: APIRoute = async ({ request }) => {
  try {
    const { code } = await request.json();

    if (!code) {
      return new Response(
        JSON.stringify({ error: 'Authorization code is required' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      );
    }

    const clientId = import.meta.env.GOOGLE_CLIENT_ID;
    const clientSecret = import.meta.env.GOOGLE_CLIENT_SECRET;
    const redirectUri = import.meta.env.GOOGLE_REDIRECT_URI || `${new URL(request.url).origin}/auth/google/callback`;

    if (!clientId || !clientSecret) {
      return new Response(
        JSON.stringify({ error: 'Google OAuth not configured. Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables.' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      );
    }

    // Exchange code for tokens
    const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        code,
        client_id: clientId,
        client_secret: clientSecret,
        redirect_uri: redirectUri,
        grant_type: 'authorization_code',
      }),
    });

    if (!tokenResponse.ok) {
      const error = await tokenResponse.text();
      console.error('Token exchange failed:', error);
      return new Response(
        JSON.stringify({ error: 'Failed to exchange authorization code' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      );
    }

    const tokens = await tokenResponse.json();

    return new Response(
      JSON.stringify({
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        expiresIn: tokens.expires_in,
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    );
  } catch (error) {
    console.error('Error in exchange-token:', error);
    return new Response(
      JSON.stringify({ error: 'Internal server error' }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    );
  }
};
