/**
 * API Endpoint: Fetch Luma events from Google Calendar
 * POST /api/google-calendar/luma-events
 *
 * Filters Google Calendar events to only return those organized by Luma
 * (organizer email: calendar-invite@lu.ma)
 */

import type { APIRoute } from 'astro';

const LUMA_ORGANIZER_EMAIL = 'calendar-invite@lu.ma';

export const POST: APIRoute = async ({ request }) => {
  try {
    const { accessToken, timeMin, timeMax } = await request.json();

    if (!accessToken) {
      return new Response(
        JSON.stringify({ error: 'Access token is required' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      );
    }

    // Build Google Calendar API URL
    const calendarApiUrl = new URL('https://www.googleapis.com/calendar/v3/calendars/primary/events');

    // Add query parameters
    calendarApiUrl.searchParams.append('maxResults', '2500'); // Get many events
    calendarApiUrl.searchParams.append('singleEvents', 'true');
    calendarApiUrl.searchParams.append('orderBy', 'startTime');

    if (timeMin) {
      calendarApiUrl.searchParams.append('timeMin', timeMin);
    }
    if (timeMax) {
      calendarApiUrl.searchParams.append('timeMax', timeMax);
    }

    // Fetch events from Google Calendar
    const calendarResponse = await fetch(calendarApiUrl.toString(), {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Accept': 'application/json',
      },
    });

    if (!calendarResponse.ok) {
      const error = await calendarResponse.text();
      console.error('Google Calendar API error:', error);

      if (calendarResponse.status === 401) {
        return new Response(
          JSON.stringify({ error: 'Access token expired or invalid. Please reconnect.' }),
          { status: 401, headers: { 'Content-Type': 'application/json' } }
        );
      }

      return new Response(
        JSON.stringify({ error: 'Failed to fetch calendar events' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      );
    }

    const data = await calendarResponse.json();
    const allEvents = data.items || [];

    // Log first few organizer emails for debugging
    console.log('Sample organizer emails:', allEvents.slice(0, 5).map((e: any) => ({
      title: e.summary,
      organizer: e.organizer?.email
    })));

    // Filter to only Luma events (organizer email matches)
    const lumaEvents = allEvents.filter((event: any) => {
      const organizerEmail = event.organizer?.email?.toLowerCase();
      const isLuma = organizerEmail === LUMA_ORGANIZER_EMAIL.toLowerCase();

      // Also try matching if organizer email contains 'lu.ma'
      const isLumaVariant = organizerEmail?.includes('lu.ma');

      return isLuma || isLumaVariant;
    });

    console.log(`Found ${allEvents.length} total events, ${lumaEvents.length} Luma events`);
    console.log('Luma event titles:', lumaEvents.map((e: any) => e.summary));

    return new Response(
      JSON.stringify(lumaEvents),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    );
  } catch (error) {
    console.error('Error in luma-events:', error);
    return new Response(
      JSON.stringify({ error: 'Internal server error' }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    );
  }
};
