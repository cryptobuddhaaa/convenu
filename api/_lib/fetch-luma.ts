/**
 * Luma event fetcher handler.
 * Routed via /api/calendar?action=fetch-luma
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { requireAuth } from './auth.js';

interface LumaEventData {
  title: string;
  startTime?: string;
  endTime?: string;
  location: {
    name: string;
    address?: string;
  };
  description?: string;
}

/**
 * Decode HTML entities like &amp;, &lt;, &gt;, &quot;, etc.
 */
function decodeHtmlEntities(text: string): string {
  const entities: Record<string, string> = {
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&quot;': '"',
    '&#39;': "'",
    '&apos;': "'",
    '&nbsp;': ' ',
  };

  let decoded = text;

  // Replace named entities
  for (const [entity, char] of Object.entries(entities)) {
    decoded = decoded.replace(new RegExp(entity, 'g'), char);
  }

  // Replace numeric entities (&#123; or &#x1A;)
  decoded = decoded.replace(/&#(\d+);/g, (_, dec) => String.fromCharCode(parseInt(dec, 10)));
  decoded = decoded.replace(/&#x([0-9a-fA-F]+);/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)));

  return decoded;
}

function parseEventHtml(html: string): LumaEventData | null {
  try {
    // PRIORITY 1: Extract Next.js data (most reliable source)
    const nextDataMatch = html.match(/<script\s+id="__NEXT_DATA__"[^>]*type="application\/json">(.+?)<\/script>/s);

    if (nextDataMatch) {
      try {
        const nextData = JSON.parse(nextDataMatch[1]);

        // Try multiple paths where event data might be located
        const event = nextData?.props?.pageProps?.initialData?.data?.event ||
                     nextData?.props?.pageProps?.initialData?.event;

        if (event) {
          const locationInfo = event.geo_address_info || {};
          const isLocationHidden = locationInfo.mode === 'obfuscated' ||
                                  event.geo_address_visibility === 'guests-only';

          let locationName = '';
          let locationAddress = '';

          if (isLocationHidden) {
            const cityState = locationInfo.city_state || locationInfo.city || '';
            const region = locationInfo.region || '';

            if (cityState || region) {
              locationName = `${cityState || region} (exact location hidden - guests only)`;
            } else if (event.coordinate) {
              locationName = `${event.coordinate.latitude}, ${event.coordinate.longitude} (approximate location)`;
            } else {
              locationName = 'Location hidden (guests only)';
            }
          } else {
            locationName = decodeHtmlEntities(locationInfo.address || locationInfo.full_address || '');
            locationAddress = decodeHtmlEntities(locationInfo.full_address || '');
          }

          return {
            title: decodeHtmlEntities(event.name || '').replace(' | Luma', '').replace('· Luma', '').trim(),
            startTime: event.start_at,
            endTime: event.end_at,
            location: {
              name: locationName,
              address: locationAddress,
            },
            description: undefined,
          };
        }
      } catch (e) {
        console.error('Failed to parse Next.js data:', e);
      }
    }

    // PRIORITY 2: Extract Open Graph meta tags (fallback)
    const titleMatch = html.match(/<meta\s+property="og:title"\s+content="([^"]+)"/i);
    const descMatch = html.match(/<meta\s+property="og:description"\s+content="([^"]+)"/i);

    // Extract ALL JSON-LD structured data blocks
    const jsonLdMatches = html.matchAll(/<script\s+type="application\/ld\+json"[^>]*>(.+?)<\/script>/gs);

    let eventData: LumaEventData | null = null;

    for (const match of jsonLdMatches) {
      try {
        const jsonData = JSON.parse(match[1]);

        const eventSchema = Array.isArray(jsonData)
          ? jsonData.find((item: Record<string, unknown>) => item['@type'] === 'Event')
          : (jsonData['@type'] === 'Event' ? jsonData : null);

        if (eventSchema) {
          let locationName = '';
          let locationAddress = '';

          if (eventSchema.location) {
            if (typeof eventSchema.location === 'string') {
              locationName = eventSchema.location;
            } else if (eventSchema.location.name) {
              locationName = eventSchema.location.name;
              locationAddress = eventSchema.location.address?.streetAddress ||
                              eventSchema.location.address?.addressLocality ||
                              (typeof eventSchema.location.address === 'string' ? eventSchema.location.address : '');
            }
          }

          eventData = {
            title: decodeHtmlEntities(eventSchema.name || (titleMatch ? titleMatch[1].replace(' | Luma', '').trim() : '')),
            startTime: eventSchema.startDate || undefined,
            endTime: eventSchema.endDate || undefined,
            location: {
              name: decodeHtmlEntities(locationName),
              address: decodeHtmlEntities(locationAddress),
            },
            description: eventSchema.description ? decodeHtmlEntities(eventSchema.description) : (descMatch ? decodeHtmlEntities(descMatch[1]) : undefined),
          };

          break;
        }
      } catch (e) {
        console.error('Failed to parse JSON-LD block:', e);
        continue;
      }
    }

    // Fallback to Open Graph data if JSON-LD parsing failed
    if (!eventData && titleMatch) {
      const title = decodeHtmlEntities(titleMatch[1].replace(' | Luma', '').replace('· Luma', '').trim());
      const description = descMatch ? decodeHtmlEntities(descMatch[1]) : '';

      let startTime: string | undefined;
      let endTime: string | undefined;
      let locationName = '';

      if (description) {
        const dateTimeMatch = description.match(/Date:\s*([^,]+,\s*\d{4}),\s*(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm))\s*[—\-–]\s*(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm))/i);

        if (dateTimeMatch) {
          const datePart = dateTimeMatch[1].trim();
          const startTimePart = dateTimeMatch[2].trim();
          const endTimePart = dateTimeMatch[3].trim();

          const cleanDate = datePart.replace(/(\d+)(st|nd|rd|th)/, '$1');

          try {
            const startDateTime = new Date(`${cleanDate} ${startTimePart}`);
            const endDateTime = new Date(`${cleanDate} ${endTimePart}`);

            if (!isNaN(startDateTime.getTime())) {
              startTime = startDateTime.toISOString();
            }
            if (!isNaN(endDateTime.getTime())) {
              endTime = endDateTime.toISOString();
            }
          } catch (e) {
            console.error('Failed to parse date/time:', e);
          }
        }

        const locationMatch = description.match(/Location:\s*([^\n]+)/i);
        if (locationMatch) {
          locationName = locationMatch[1].trim();
        }
      }

      eventData = {
        title,
        startTime,
        endTime,
        location: {
          name: locationName,
        },
        description,
      };
    }

    return eventData;
  } catch (error) {
    console.error('Error parsing event HTML:', error);
    return null;
  }
}

export async function handleFetchLuma(req: VercelRequest, res: VercelResponse) {
  // Only allow GET requests
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const authUser = await requireAuth(req, res);
    if (!authUser) return;

    const { url } = req.query;

    // Validate the URL parameter
    if (!url || typeof url !== 'string') {
      return res.status(400).json({ error: 'Missing or invalid url parameter' });
    }

    // Validate it's actually a Luma URL using proper URL parsing
    let parsedUrl: URL;
    try {
      parsedUrl = new URL(url);
    } catch {
      return res.status(400).json({ error: 'Invalid URL' });
    }

    const hostname = parsedUrl.hostname.toLowerCase();
    if (hostname !== 'lu.ma' && !hostname.endsWith('.lu.ma') &&
        hostname !== 'luma.com' && !hostname.endsWith('.luma.com')) {
      return res.status(400).json({ error: 'URL must be from lu.ma or luma.com' });
    }

    if (parsedUrl.protocol !== 'https:') {
      return res.status(400).json({ error: 'URL must use HTTPS' });
    }

    // Fetch the Luma page server-side (no CORS restrictions)
    const response = await fetch(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (compatible; ItineraryBot/1.0)',
      },
    });

    if (!response.ok) {
      return res.status(502).json({
        error: 'Failed to fetch event data from Luma'
      });
    }

    const html = await response.text();

    // Parse the HTML to extract event data
    const eventData = parseEventHtml(html);

    if (!eventData) {
      return res.status(404).json({
        error: 'Could not extract event data from page'
      });
    }

    return res.status(200).json(eventData);
  } catch (error) {
    console.error('Error fetching Luma event:', error);
    return res.status(500).json({
      error: 'Failed to fetch event data'
    });
  }
}
