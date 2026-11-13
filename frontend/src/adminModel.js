import { dateKey, time } from './model.js';

export const permissionModules = [
  { id: 'movies', name: 'Movies', view: 'VIEW_MOVIES', manage: 'MANAGE_MOVIES', detail: 'View the catalog; add, edit, archive and restore movies.' },
  { id: 'shows', name: 'Showtimes', view: 'VIEW_SHOWS', manage: 'MANAGE_SHOWS', detail: 'View screenings; schedule shows and generate seats.' },
  { id: 'rooms', name: 'Rooms & seats', view: 'VIEW_ROOMS', manage: 'MANAGE_ROOMS', detail: 'View layouts; create and update rooms and physical seats.' },
  { id: 'bookings', name: 'Bookings', view: 'VIEW_BOOKINGS', manage: 'MANAGE_BOOKINGS', detail: 'View all reservations; create or cancel customer bookings.' },
  { id: 'payments', name: 'Payments', view: 'VIEW_PAYMENTS', manage: 'MANAGE_PAYMENTS', detail: 'View payment records; create, verify or reject customer payments.' },
  { id: 'customers', name: 'Customers', view: 'VIEW_CUSTOMERS', detail: 'View customer profiles. Booking history also requires Bookings access.' },
];
export const hasPermission = (user, permission) => user?.role === 'ADMIN' ||
  (Array.isArray(user?.permissions) ? user.permissions.includes(permission) : user?.role === 'MODERATOR');
export const isOperator = user => user?.role === 'ADMIN' || permissionModules.some(module => hasPermission(user, module.view));
export const isEmployee = user => ['ADMIN', 'MODERATOR'].includes(user?.role) || isOperator(user);
export const canReviewPayment = (payment, data) => Array.isArray(data.reviewable_payment_ids)
  ? data.reviewable_payment_ids.includes(payment.id) : canReview(payment, data.bookings);
export const isDemoPayment = payment => payment.transaction_id?.startsWith('MOCK-') || payment.transaction_id?.startsWith('SEPAY-TEST-') || false;
export function bookingState(booking, now = Date.now()) {
  return booking.status === 'PENDING' && booking.expires_at && new Date(booking.expires_at).getTime() <= now ? 'EXPIRED' : booking.status;
}
export const canReview = (payment, bookings, now = Date.now()) => payment.status === 'PENDING' &&
  bookings.some(booking => booking.id === payment.booking_id && bookingState(booking, now) === 'PENDING' && new Date(booking.expires_at).getTime() > now);
export function adminMetrics(data, now = new Date()) {
  const today = dateKey(now);
  const paid = data.payments.filter(payment => payment.status === 'PAID');
  const verified = paid.filter(payment => !isDemoPayment(payment));
  return {
    todayShows: data.shows.filter(show => dateKey(show.start_time) === today).length,
    activeHolds: data.bookings.filter(booking => bookingState(booking, now.getTime()) === 'PENDING').length,
    verifiedRevenue: verified.reduce((sum, payment) => sum + Number(payment.amount), 0),
    pendingReview: data.payments.filter(payment => canReviewPayment(payment, data)).length,
    demoPayments: paid.length - verified.length,
  };
}
export const matchesSearch = (query, ...values) => !query.trim() || values.some(value => String(value || '').toLowerCase().includes(query.trim().toLowerCase()));
export const labelStatus = value => ({ PENDING: 'Pending', CONFIRMED: 'Confirmed', CANCELLED: 'Cancelled', EXPIRED: 'Expired', PAID: 'Paid', AVAILABLE: 'Available', ADMIN: 'Admin', MODERATOR: 'Employee', USER: 'Customer' })[value] || value;
export function localDateTime(date = new Date()) {
  return `${dateKey(date)}T${time(date)}`;
}
