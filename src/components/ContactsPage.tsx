import ContactsList from './ContactsList';

export default function ContactsPage() {
  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900">My Contacts</h2>
        <p className="text-sm text-gray-600 mt-1">
          People you've met at events across all your itineraries
        </p>
      </div>
      <ContactsList />
    </div>
  );
}
