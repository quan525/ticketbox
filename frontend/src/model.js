export const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
export const time = value => value ? new Date(value).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }) : 'Time to be announced';
export const dayLabel = value => value ? new Date(value).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' }) : 'Date to be announced';
export const dateKey = value => {
  if (!value) return '';
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

export function upcomingShows(shows, now = Date.now()) {
  return shows.filter(show => show.start_time && new Date(show.start_time).getTime() > now)
    .sort((a, b) => new Date(a.start_time) - new Date(b.start_time));
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
