import { useState, useEffect } from 'react';
import { shareService } from '../services/shareService';
import type { ShareVisibility } from '../services/shareService';
import type { Itinerary } from '../models/types';

interface ShareDialogProps {
  itinerary: Itinerary;
  onClose: () => void;
}

export default function ShareDialog({ itinerary, onClose }: ShareDialogProps) {
  const [copied, setCopied] = useState(false);
  const [shareUrl, setShareUrl] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [showVisibility, setShowVisibility] = useState(false);
  const [hiddenDays, setHiddenDays] = useState<Set<string>>(new Set());
  const [hiddenEvents, setHiddenEvents] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  // Load existing visibility and generate URL
  useEffect(() => {
    const init = async () => {
      try {
        setLoading(true);
        // Load existing visibility config
        const existing = await shareService.getShareVisibility(itinerary.id);
        if (existing) {
          setHiddenDays(new Set(existing.hiddenDays || []));
          setHiddenEvents(new Set(existing.hiddenEvents || []));
        }
        const url = await shareService.generateShareUrl(itinerary);
        setShareUrl(url);
        setError('');
      } catch (err) {
        setError('Failed to generate share URL. Please try again.');
        console.error('Share URL generation error:', err);
      } finally {
        setLoading(false);
      }
    };

    init();
  }, [itinerary]);

  const handleCopy = async () => {
    const success = await shareService.copyToClipboard(shareUrl);
    if (success) {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const toggleDay = (date: string) => {
    setHiddenDays((prev) => {
      const next = new Set(prev);
      if (next.has(date)) {
        next.delete(date);
      } else {
        next.add(date);
      }
      return next;
    });
  };

  const toggleEvent = (eventId: string) => {
    setHiddenEvents((prev) => {
      const next = new Set(prev);
      if (next.has(eventId)) {
        next.delete(eventId);
      } else {
        next.add(eventId);
      }
      return next;
    });
  };

  const saveVisibility = async () => {
    setSaving(true);
    try {
      const visibility: ShareVisibility = {
        hiddenDays: [...hiddenDays],
        hiddenEvents: [...hiddenEvents],
      };
      const url = await shareService.generateShareUrl(itinerary, visibility);
      setShareUrl(url);
      setShowVisibility(false);
    } catch (err) {
      console.error('Failed to save visibility:', err);
    } finally {
      setSaving(false);
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
  };

  const visibleDayCount = itinerary.days.filter((d) => !hiddenDays.has(d.date)).length;
  const totalEvents = itinerary.days.reduce((sum, d) => sum + d.events.length, 0);
  const visibleEventCount = itinerary.days.reduce(
    (sum, d) => sum + (hiddenDays.has(d.date) ? 0 : d.events.filter((e) => !hiddenEvents.has(e.id)).length),
    0
  );

  return (
    <div className="fixed inset-0 bg-gray-500 bg-opacity-75 flex items-center justify-center p-4 z-50">
      <div className="bg-slate-800 rounded-lg max-w-2xl w-full p-6 max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h3 className="text-lg font-semibold text-white">Share Itinerary</h3>
            <p className="text-sm text-slate-300 mt-1">{itinerary.title}</p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-200"
            aria-label="Close"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="space-y-4">
          {loading && (
            <div className="flex items-center justify-center py-8">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              <p className="ml-3 text-slate-300">Generating share link...</p>
            </div>
          )}

          {error && !loading && (
            <div className="bg-red-50 border border-red-200 rounded-md p-4">
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}

          {!error && !loading && (
          <>
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Shareable URL
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={shareUrl}
                  readOnly
                  className="flex-1 rounded-md border-slate-600 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-3 py-2 border bg-slate-700 text-white"
                />
                <button
                  onClick={handleCopy}
                  className="inline-flex items-center px-3 py-1.5 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-slate-800 focus:ring-offset-2 focus:ring-blue-500"
                >
                  {copied ? (
                    <>
                      <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                      Copied!
                    </>
                  ) : (
                    <>
                      <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                      </svg>
                      Copy
                    </>
                  )}
                </button>
              </div>
              <p className="text-sm text-slate-400 mt-2">
                Sharing {visibleDayCount} of {itinerary.days.length} day{itinerary.days.length !== 1 ? 's' : ''}, {visibleEventCount} of {totalEvents} event{totalEvents !== 1 ? 's' : ''}
              </p>
            </div>

            {/* Visibility controls */}
            <div className="border border-slate-700 rounded-lg">
              <button
                onClick={() => setShowVisibility(!showVisibility)}
                className="w-full flex items-center justify-between p-3 text-sm font-medium text-slate-300 hover:bg-slate-700/50 rounded-lg transition-colors"
              >
                <div className="flex items-center gap-2">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  Choose what to share
                </div>
                <svg
                  className={`w-4 h-4 transition-transform ${showVisibility ? 'rotate-180' : ''}`}
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {showVisibility && (
                <div className="border-t border-slate-700 p-3 space-y-3">
                  <p className="text-xs text-slate-400">
                    Uncheck days or events to hide them from the shared view.
                  </p>

                  {itinerary.days.map((day) => {
                    const dayHidden = hiddenDays.has(day.date);
                    return (
                      <div key={day.date} className="space-y-1">
                        <label className="flex items-center gap-2 cursor-pointer group">
                          <input
                            type="checkbox"
                            checked={!dayHidden}
                            onChange={() => toggleDay(day.date)}
                            className="rounded border-slate-500 bg-slate-700 text-blue-500 focus:ring-blue-500 focus:ring-offset-0"
                          />
                          <span className={`text-sm font-medium ${dayHidden ? 'text-slate-500 line-through' : 'text-slate-200'}`}>
                            Day {day.dayNumber}: {formatDate(day.date)}
                          </span>
                          <span className="text-xs text-slate-500">
                            ({day.events.length} event{day.events.length !== 1 ? 's' : ''})
                          </span>
                        </label>

                        {!dayHidden && day.events.length > 0 && (
                          <div className="ml-6 space-y-1">
                            {[...day.events]
                              .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
                              .map((event) => {
                                const eventHidden = hiddenEvents.has(event.id);
                                return (
                                  <label key={event.id} className="flex items-center gap-2 cursor-pointer">
                                    <input
                                      type="checkbox"
                                      checked={!eventHidden}
                                      onChange={() => toggleEvent(event.id)}
                                      className="rounded border-slate-500 bg-slate-700 text-blue-500 focus:ring-blue-500 focus:ring-offset-0"
                                    />
                                    <span className={`text-xs ${eventHidden ? 'text-slate-500 line-through' : 'text-slate-300'}`}>
                                      {formatTime(event.startTime)} — {event.title}
                                    </span>
                                  </label>
                                );
                              })}
                          </div>
                        )}
                      </div>
                    );
                  })}

                  <button
                    onClick={saveVisibility}
                    disabled={saving}
                    className="w-full mt-2 inline-flex items-center justify-center px-3 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-offset-slate-800 focus:ring-offset-2 focus:ring-blue-500"
                  >
                    {saving ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent mr-2"></div>
                        Updating...
                      </>
                    ) : (
                      'Update shared link'
                    )}
                  </button>
                </div>
              )}
            </div>
          </>
          )}

          <div className="flex justify-end pt-4">
            <button
              onClick={onClose}
              className="px-3 py-1.5 border border-slate-600 rounded-md shadow-sm text-sm font-medium text-slate-300 bg-slate-800 hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-offset-slate-800 focus:ring-offset-2 focus:ring-blue-500"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
