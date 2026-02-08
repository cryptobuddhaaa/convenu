# Google Calendar Integration Setup

This guide will help you set up Google Calendar integration to import Luma events.

## Prerequisites

- A Google Cloud Platform account
- Access to Google Cloud Console
- Your application deployed or running locally

## Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Note your project ID

## Step 2: Enable Google Calendar API

1. In the Google Cloud Console, navigate to **APIs & Services** → **Library**
2. Search for "Google Calendar API"
3. Click on it and click **Enable**

## Step 3: Create OAuth 2.0 Credentials

1. Navigate to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth client ID**
3. If prompted, configure the OAuth consent screen:
   - Choose **External** user type (unless you have a workspace)
   - Fill in the required fields:
     - App name: Your app name (e.g., "Shareable Itinerary")
     - User support email: Your email
     - Developer contact: Your email
   - Add scopes: `https://www.googleapis.com/auth/calendar.readonly`
   - Add test users (your Google account email)
   - Click **Save and Continue**

4. Back in Credentials, click **Create Credentials** → **OAuth client ID** again
5. Choose **Web application** as the application type
6. Configure:
   - **Name**: Give it a descriptive name (e.g., "Web Client")
   - **Authorized JavaScript origins**:
     - For local: `http://localhost:4321`
     - For production: `https://yourdomain.com`
   - **Authorized redirect URIs**:
     - For local: `http://localhost:4321/auth/google/callback`
     - For production: `https://yourdomain.com/auth/google/callback`
7. Click **Create**
8. Copy your **Client ID** and **Client Secret**

## Step 4: Configure Environment Variables

1. Copy `.env.example` to `.env` (if you haven't already)
2. Add your Google OAuth credentials:

```bash
# Google OAuth 2.0 Credentials
VITE_GOOGLE_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret-here
GOOGLE_REDIRECT_URI=http://localhost:4321/auth/google/callback
```

3. For production, update `GOOGLE_REDIRECT_URI` to your production domain

## Step 5: Deploy and Test

1. Restart your development server to load the new environment variables
2. Navigate to your itinerary page
3. Click **"Connect Google Calendar"**
4. Authorize the app to access your calendar
5. Click **"Import Luma Events"** to fetch and import events

## How It Works

### Privacy & Security

- **Only Luma events are imported**: The integration filters events to only import those organized by `calendar-invite@lu.ma`
- **Read-only access**: The app only requests read access to your calendar
- **No other events are accessed**: Events from other organizers are ignored and never sent to the app

### Event Filtering

The integration automatically:
1. Fetches all events from your Google Calendar within your itinerary date range
2. Filters to only events where the organizer email is `calendar-invite@lu.ma`
3. Extracts Luma event URLs from event descriptions
4. Converts events to the itinerary format
5. Allows you to preview and select which events to import

### Token Storage

- Access tokens are stored in `sessionStorage` (cleared when browser is closed)
- Tokens are never sent to external servers except Google's OAuth endpoints
- You can disconnect at any time by clicking "Disconnect"

## Troubleshooting

### "OAuth not configured" error
- Make sure you've set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in your `.env` file
- Restart your development server after adding credentials

### "Redirect URI mismatch" error
- Verify that the redirect URI in your `.env` matches exactly what's configured in Google Cloud Console
- Make sure to include the protocol (`http://` or `https://`)

### "Access denied" or "401 Unauthorized"
- Your access token may have expired
- Click "Disconnect" and then "Connect Google Calendar" again

### No events showing up
- Verify you have Luma events in your calendar for the selected date range
- Check that the events are organized by `calendar-invite@lu.ma`
- Try expanding the date range of your itinerary

## Production Deployment

When deploying to production:

1. Update your OAuth consent screen to "Published" status (if required)
2. Update `GOOGLE_REDIRECT_URI` to your production domain
3. Add your production domain to **Authorized JavaScript origins** and **Authorized redirect URIs** in Google Cloud Console
4. Ensure environment variables are set in your production environment
5. Test the OAuth flow in production

## Support

For issues with:
- Google OAuth setup: Check [Google's OAuth 2.0 documentation](https://developers.google.com/identity/protocols/oauth2)
- Google Calendar API: Check [Google Calendar API documentation](https://developers.google.com/calendar/api)
