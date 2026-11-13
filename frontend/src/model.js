export const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
export const cinemaZone = 'Asia/Ho_Chi_Minh';
export const viewerZone = new Intl.DateTimeFormat().resolvedOptions().timeZone;
const dayOptions = { weekday: 'short', day: 'numeric', month: 'short' };
const dateOptions = { year: 'numeric', month: '2-digit', day: '2-digit' };

export function cinemaDate(value) {
  const localTime = typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?$/.test(value);
  return new Date(localTime ? value + '+07:00' : value);
}

export const time = (value, timeZone = viewerZone) => value ? cinemaDate(value).toLocaleTimeString('en-GB', { timeZone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }) : 'Time to be announced';
export const dayLabel = (value, timeZone = viewerZone) => value ? cinemaDate(value).toLocaleDateString('en-GB', { ...dayOptions, timeZone }) : 'Date to be announced';
const bookingDateFormat = new Intl.DateTimeFormat('en-GB', {
  timeZone: viewerZone,
  dateStyle: 'long',
  timeStyle: 'short',
});

const venueBookingDateFormat = new Intl.DateTimeFormat('en-GB', {
  timeZone: cinemaZone,
  dateStyle: 'long',
  timeStyle: 'short',
});

export function bookedAtLabel(value, { venue = false } = {}) {
  if (!value) return 'Unavailable';
  const bookedAt = new Date(value);
  if (Number.isNaN(bookedAt.getTime())) return 'Unavailable';
  return (venue ? venueBookingDateFormat : bookingDateFormat).format(bookedAt);
}

export const calendarDayLabel = date => new Date(date + 'T00:00:00Z').toLocaleDateString('en-GB', { ...dayOptions, timeZone: 'UTC' });

export const dateKey = (value, timeZone = viewerZone) => {
  if (!value) return '';
  return cinemaDate(value).toLocaleDateString('sv-SE', { ...dateOptions, timeZone });
};

export const venueDateKey = value => dateKey(value, cinemaZone);
export const venueTime = value => time(value, cinemaZone);
export const venueDayLabel = value => dayLabel(value, cinemaZone);

export function upcomingShows(shows, now = Date.now()) {
  return shows.filter(show => show.start_time && cinemaDate(show.start_time).getTime() > now)
    .sort((a, b) => cinemaDate(a.start_time) - cinemaDate(b.start_time));
}

export function filterMovies(movies, shows, { query = '', language = 'all', date = 'all', tab = 'showing', saved = [] } = {}) {
  const search = query.trim().toLowerCase();
  return movies.filter(movie => {
    const movieShows = shows.filter(show => show.movie.id === movie.id);
    return (!search || `${movie.title} ${movie.author} ${movie.description || ''}`.toLowerCase().includes(search)) &&
      (language === 'all' || movie.language === language) &&
      (date === 'all' || movieShows.some(show => dateKey(show.start_time) === date)) &&
      (tab !== 'showing' || movieShows.length > 0) &&
      (tab !== 'upcoming' || movieShows.length === 0) &&
      (tab !== 'saved' || saved.includes(movie.id));
  });
}

export function groupSeats(seats) {
  const rows = new Map();
  [...seats].sort((a, b) => a.seat.row_label.localeCompare(b.seat.row_label, undefined, { numeric: true }) || a.seat.seat_number - b.seat.seat_number)
    .forEach(seat => {
      const row = seat.seat.row_label;
      rows.set(row, [...(rows.get(row) || []), seat]);
    });
  return [...rows.entries()];
}

export const seatLabel = showSeat => `${showSeat.seat.row_label}${showSeat.seat.seat_number}`;
export const selectionTotal = (seats, selected) => seats.filter(seat => selected.includes(seat.id)).reduce((sum, seat) => sum + Number(seat.price), 0);

export function secondsRemaining(expiresAt, now = Date.now()) {
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - now) / 1000));
}
