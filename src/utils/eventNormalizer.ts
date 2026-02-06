/**
 * Event Normalizer
 * Handles conversion between database format (snake_case) and TypeScript format (camelCase)
 */

import type { ItineraryEvent } from '../models/types';

/**
 * Normalize event from database format (snake_case) to TypeScript format (camelCase)
 * Handles both formats for backward compatibility
 */
export function normalizeEvent(event: any): ItineraryEvent {
  return {
    id: event.id,
    title: event.title,
    // Handle both snake_case (from DB) and camelCase (from types)
    startTime: event.start_time || event.startTime,
    endTime: event.end_time || event.endTime,
    location: event.location,
    eventType: event.event_type || event.eventType,
    description: event.description,
    goals: event.goals,
    lumaEventUrl: event.luma_event_url || event.lumaEventUrl,
    notes: event.notes || [],
    isOrganized: event.is_organized || event.isOrganized,
    checklist: event.checklist
  };
}

/**
 * Convert event from TypeScript format (camelCase) to database format (snake_case)
 */
export function denormalizeEvent(event: Partial<ItineraryEvent>): any {
  return {
    id: event.id,
    title: event.title,
    start_time: event.startTime,
    end_time: event.endTime,
    location: event.location,
    event_type: event.eventType,
    description: event.description,
    goals: event.goals,
    luma_event_url: event.lumaEventUrl,
    notes: event.notes,
    is_organized: event.isOrganized,
    checklist: event.checklist
  };
}

/**
 * Normalize array of events
 */
export function normalizeEvents(events: any[]): ItineraryEvent[] {
  return events.map(normalizeEvent);
}

/**
 * Get event property value handling both formats
 * Useful for quick access without full normalization
 */
export function getEventProperty<T = any>(
  event: any,
  camelCaseKey: string,
  snakeCaseKey: string
): T | undefined {
  return event[snakeCaseKey] || event[camelCaseKey];
}

/**
 * Type guard to check if event has valid time properties
 */
export function hasValidTimes(event: any): boolean {
  const startTime = event.start_time || event.startTime;
  const endTime = event.end_time || event.endTime;

  if (!startTime || !endTime) return false;

  const start = new Date(startTime);
  const end = new Date(endTime);

  return !isNaN(start.getTime()) && !isNaN(end.getTime());
}
