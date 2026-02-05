import { create } from 'zustand';
import type { Contact } from '../models/types';
import { supabase } from '../lib/supabase';

interface ContactsState {
  contacts: Contact[];
  loading: boolean;
  initialized: boolean;

  // Computed
  getContactsByItinerary: (itineraryId: string) => Contact[];
  getContactsByEvent: (eventId: string) => Contact[];

  // Actions
  initialize: (userId: string) => Promise<void>;
  addContact: (contactData: {
    itineraryId: string;
    eventId: string;
    eventTitle: string;
    dateMet: string;
    firstName: string;
    lastName: string;
    projectCompany?: string;
    telegramHandle?: string;
    email?: string;
  }) => Promise<void>;
  updateContact: (contactId: string, updates: Partial<Contact>) => Promise<void>;
  deleteContact: (contactId: string) => Promise<void>;
  reset: () => void;
}

export const useContacts = create<ContactsState>()((set, get) => ({
  contacts: [],
  loading: false,
  initialized: false,

  getContactsByItinerary: (itineraryId: string) => {
    const state = get();
    return state.contacts.filter((c) => c.itineraryId === itineraryId);
  },

  getContactsByEvent: (eventId: string) => {
    const state = get();
    return state.contacts.filter((c) => c.eventId === eventId);
  },

  initialize: async (userId: string) => {
    set({ loading: true });
    try {
      const { data, error } = await supabase
        .from('contacts')
        .select('*')
        .eq('user_id', userId)
        .order('created_at', { ascending: false });

      if (error) throw error;

      const contacts: Contact[] = (data || []).map((row) => ({
        id: row.id,
        itineraryId: row.itinerary_id,
        eventId: row.event_id,
        userId: row.user_id,
        firstName: row.first_name,
        lastName: row.last_name,
        projectCompany: row.project_company,
        telegramHandle: row.telegram_handle,
        email: row.email,
        eventTitle: row.event_title,
        dateMet: row.date_met,
        createdAt: row.created_at,
        updatedAt: row.updated_at,
      }));

      set({
        contacts,
        initialized: true,
      });
    } catch (error) {
      console.error('Error loading contacts:', error);
    } finally {
      set({ loading: false });
    }
  },

  addContact: async (contactData) => {
    set({ loading: true });
    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) throw new Error('Not authenticated');

      const { data, error } = await supabase
        .from('contacts')
        .insert({
          itinerary_id: contactData.itineraryId,
          event_id: contactData.eventId,
          user_id: user.id,
          first_name: contactData.firstName,
          last_name: contactData.lastName,
          project_company: contactData.projectCompany,
          telegram_handle: contactData.telegramHandle,
          email: contactData.email,
          event_title: contactData.eventTitle,
          date_met: contactData.dateMet,
        })
        .select()
        .single();

      if (error) throw error;

      const newContact: Contact = {
        id: data.id,
        itineraryId: data.itinerary_id,
        eventId: data.event_id,
        userId: data.user_id,
        firstName: data.first_name,
        lastName: data.last_name,
        projectCompany: data.project_company,
        telegramHandle: data.telegram_handle,
        email: data.email,
        eventTitle: data.event_title,
        dateMet: data.date_met,
        createdAt: data.created_at,
        updatedAt: data.updated_at,
      };

      set((state) => ({
        contacts: [newContact, ...state.contacts],
      }));
    } catch (error) {
      console.error('Error adding contact:', error);
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  updateContact: async (contactId: string, updates: Partial<Contact>) => {
    set({ loading: true });
    try {
      const updateData: Record<string, unknown> = {};
      if (updates.firstName !== undefined) updateData.first_name = updates.firstName;
      if (updates.lastName !== undefined) updateData.last_name = updates.lastName;
      if (updates.projectCompany !== undefined) updateData.project_company = updates.projectCompany;
      if (updates.telegramHandle !== undefined) updateData.telegram_handle = updates.telegramHandle;
      if (updates.email !== undefined) updateData.email = updates.email;

      const { data, error } = await supabase
        .from('contacts')
        .update(updateData)
        .eq('id', contactId)
        .select()
        .single();

      if (error) throw error;

      const updatedContact: Contact = {
        id: data.id,
        itineraryId: data.itinerary_id,
        eventId: data.event_id,
        userId: data.user_id,
        firstName: data.first_name,
        lastName: data.last_name,
        projectCompany: data.project_company,
        telegramHandle: data.telegram_handle,
        email: data.email,
        eventTitle: data.event_title,
        dateMet: data.date_met,
        createdAt: data.created_at,
        updatedAt: data.updated_at,
      };

      set((state) => ({
        contacts: state.contacts.map((c) =>
          c.id === contactId ? updatedContact : c
        ),
      }));
    } catch (error) {
      console.error('Error updating contact:', error);
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  deleteContact: async (contactId: string) => {
    set({ loading: true });
    try {
      const { error } = await supabase
        .from('contacts')
        .delete()
        .eq('id', contactId);

      if (error) throw error;

      set((state) => ({
        contacts: state.contacts.filter((c) => c.id !== contactId),
      }));
    } catch (error) {
      console.error('Error deleting contact:', error);
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  reset: () => {
    set({
      contacts: [],
      loading: false,
      initialized: false,
    });
  },
}));
