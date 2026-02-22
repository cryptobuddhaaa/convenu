import type { Itinerary } from '../models/types';

function escapeHtml(text: string): string {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function formatTime(dateStr: string): string {
  return new Date(dateStr).toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
}

const eventTypeLabels: Record<string, string> = {
  meeting: 'Meeting',
  travel: 'Travel',
  meal: 'Meal',
  buffer: 'Buffer',
  accommodation: 'Accommodation',
  activity: 'Activity',
  'side-event': 'Side Event',
  'main-conference': 'Main Conference',
};

export function printItinerary(itinerary: Itinerary): void {
  const html = generatePrintHtml(itinerary);
  const printWindow = window.open('', '_blank');
  if (!printWindow) return;
  printWindow.document.write(html);
  printWindow.document.close();
  // Auto-print after content loads
  printWindow.onload = () => {
    printWindow.print();
  };
}

function generatePrintHtml(itinerary: Itinerary): string {
  const title = escapeHtml(itinerary.title);
  const location = escapeHtml(itinerary.location);
  const dateRange = `${formatDate(itinerary.startDate)} — ${formatDate(itinerary.endDate)}`;

  let daysHtml = '';
  for (const day of itinerary.days) {
    const sortedEvents = [...day.events].sort(
      (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
    );

    let eventsHtml = '';
    if (sortedEvents.length === 0) {
      eventsHtml = '<p class="no-events">No events scheduled</p>';
    } else {
      for (const event of sortedEvents) {
        const typeLabel = eventTypeLabels[event.eventType] || event.eventType;
        const locationName = event.location?.name ? escapeHtml(event.location.name) : '';
        const locationAddr = event.location?.address ? escapeHtml(event.location.address) : '';
        const locationStr = locationAddr ? `${locationName} (${locationAddr})` : locationName;

        let eventExtras = '';
        if (event.goals && event.goals.length > 0) {
          eventExtras += `<div class="event-goals">Goals: ${escapeHtml(event.goals.join(', '))}</div>`;
        }
        if (event.description) {
          eventExtras += `<div class="event-desc">${escapeHtml(event.description)}</div>`;
        }
        if (event.lumaEventUrl) {
          eventExtras += `<div class="event-link">Event: ${escapeHtml(event.lumaEventUrl)}</div>`;
        }
        if (event.location?.mapsUrl) {
          eventExtras += `<div class="event-link">Map: ${escapeHtml(event.location.mapsUrl)}</div>`;
        }

        eventsHtml += `
          <div class="event">
            <div class="event-header">
              <span class="event-time">${formatTime(event.startTime)} – ${formatTime(event.endTime)}</span>
              <span class="event-type">${typeLabel}</span>
            </div>
            <div class="event-title">${escapeHtml(event.title)}</div>
            ${locationStr ? `<div class="event-location">${locationStr}</div>` : ''}
            ${eventExtras}
          </div>
        `;
      }
    }

    const dayGoals = day.goals.length > 0
      ? `<div class="day-goals">Goals: ${escapeHtml(day.goals.join(', '))}</div>`
      : '';

    daysHtml += `
      <div class="day">
        <div class="day-header">
          <h2>Day ${day.dayNumber}: ${formatDate(day.date)}</h2>
          ${dayGoals}
        </div>
        <div class="events">${eventsHtml}</div>
      </div>
    `;
  }

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>${title} — Itinerary</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      color: #1e293b;
      padding: 24px;
      max-width: 800px;
      margin: 0 auto;
      font-size: 11pt;
      line-height: 1.5;
    }
    .header {
      text-align: center;
      padding-bottom: 16px;
      border-bottom: 2px solid #1e293b;
      margin-bottom: 24px;
    }
    .header h1 { font-size: 22pt; margin-bottom: 4px; }
    .header .location { font-size: 13pt; color: #475569; }
    .header .dates { font-size: 10pt; color: #64748b; margin-top: 4px; }
    .header .created-by { font-size: 9pt; color: #94a3b8; margin-top: 8px; }

    .day {
      page-break-inside: avoid;
      margin-bottom: 20px;
    }
    .day-header {
      background: #f1f5f9;
      padding: 8px 12px;
      border-left: 4px solid #3b82f6;
      margin-bottom: 8px;
    }
    .day-header h2 { font-size: 13pt; }
    .day-goals { font-size: 9pt; color: #64748b; margin-top: 2px; }

    .events { padding-left: 4px; }
    .event {
      padding: 6px 0 6px 12px;
      border-left: 2px solid #e2e8f0;
      margin-bottom: 4px;
    }
    .event-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 9pt;
    }
    .event-time { color: #3b82f6; font-weight: 600; }
    .event-type {
      background: #e2e8f0;
      padding: 1px 6px;
      border-radius: 3px;
      font-size: 8pt;
      color: #475569;
    }
    .event-title { font-weight: 600; font-size: 11pt; margin: 2px 0; }
    .event-location { font-size: 9pt; color: #64748b; }
    .event-goals { font-size: 9pt; color: #64748b; font-style: italic; }
    .event-desc { font-size: 9pt; color: #475569; margin-top: 2px; }
    .event-link { font-size: 8pt; color: #94a3b8; word-break: break-all; }
    .no-events { font-size: 10pt; color: #94a3b8; font-style: italic; padding: 8px 12px; }

    .footer {
      margin-top: 24px;
      padding-top: 12px;
      border-top: 1px solid #e2e8f0;
      text-align: center;
      font-size: 8pt;
      color: #94a3b8;
    }

    @media print {
      body { padding: 0; }
      .day { page-break-inside: avoid; }
      .no-print { display: none; }
    }

    .print-btn {
      position: fixed;
      top: 16px;
      right: 16px;
      background: #3b82f6;
      color: white;
      border: none;
      padding: 8px 20px;
      border-radius: 6px;
      font-size: 12pt;
      cursor: pointer;
    }
    .print-btn:hover { background: #2563eb; }
  </style>
</head>
<body>
  <button class="print-btn no-print" onclick="window.print()">Print / Save PDF</button>

  <div class="header">
    <h1>${title}</h1>
    <div class="location">${location}</div>
    <div class="dates">${dateRange}</div>
    ${itinerary.createdByName ? `<div class="created-by">Created by ${escapeHtml(itinerary.createdByName)}</div>` : ''}
  </div>

  ${daysHtml}

  <div class="footer">
    Generated on ${new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
  </div>
</body>
</html>`;
}
