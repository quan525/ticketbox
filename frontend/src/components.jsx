import { useEffect, useRef } from 'react';
import { X, ArrowUpRight, Bookmark, Check, Film, Ticket } from 'lucide-react';
import { bookedAtLabel, money, time, dayLabel, seatLabel, secondsRemaining, viewerZone } from './model.js';

const artwork = {
  'The Last Orbit': 'orbit',
  'Midnight in Kyoto': 'kyoto',
  'Wild Current': 'current',
  'A Little More Time': 'time',
  'Concrete Dreams': 'concrete',
  'Beyond the Pines': 'pines',
  'The Quiet Season': 'season',
  'After the Applause': 'applause',
};

export function Poster({ movie, className = '' }) {
  const art = artwork[movie.title];
  return art ? <img className={`poster-image ${className}`} src={`./posters/${art}.svg`} alt={`${movie.title} illustrated poster`} /> :
    <div className={`poster-fallback ${className}`}><Film size={44} strokeWidth={1} /><span>{movie.title}</span><small>A TICKETBOX SCREENING</small></div>;
}

export function Dialog({ title, children, onClose, wide = false }) {
  const ref = useRef(null);
  useEffect(() => {
    const previous = document.activeElement;
    const oldOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const node = ref.current;
    const focusable = () => [...node.querySelectorAll('button, input, select, textarea, a[href], [tabindex="0"]')].filter(el => !el.disabled && !el.hidden);
    (focusable()[0] || node).focus();
    const handleKey = event => {
      if (event.key === 'Escape') onClose();
      if (event.key === 'Tab') {
        const items = focusable();
        const first = items[0]; const last = items.at(-1);
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }
    };
    node.addEventListener('keydown', handleKey);
    return () => { document.body.style.overflow = oldOverflow; node.removeEventListener('keydown', handleKey); previous?.focus?.(); };
  }, [onClose]);
  return <div className="dialog-backdrop" onClick={event => { if (event.target === event.currentTarget) onClose(); }}>
    <section ref={ref} className={`dialog ${wide ? 'dialog-wide' : ''}`} role="dialog" aria-modal="true" aria-label={title} tabIndex={-1}>
      <div className="dialog-top"><span className="eyebrow">TICKETBOX / {title}</span><button className="icon-button" aria-label="Close dialog" onClick={onClose}><X size={21} /></button></div>
      {children}
    </section>
  </div>;
}

export function MovieCard({ movie, shows, saved, onSave, onSelect }) {
  return <article className="movie-card">
    <div className="poster-wrap">
      <button className="poster-button" aria-label={`View showtimes for ${movie.title}`} onClick={() => onSelect(movie)}><Poster movie={movie} /><span className="poster-hover">Find your seat <ArrowUpRight size={20} /></span></button>
      <span className="movie-tag">{shows.length ? 'NOW SHOWING' : 'COMING SOON'}</span>
      <button className={`save-button ${saved ? 'is-saved' : ''}`} aria-label={`${saved ? 'Remove' : 'Save'} ${movie.title}${saved ? ' from' : ' to'} watchlist`} aria-pressed={saved} onClick={() => onSave(movie.id)}><Bookmark size={16} fill={saved ? 'currentColor' : 'none'} /></button>
    </div>
    <div className="movie-details"><h3>{movie.title}</h3><p>{movie.language} <span>·</span> {movie.duration_min} min <span>·</span> {movie.author}</p></div>
    <button className="card-action" onClick={() => onSelect(movie)}>{shows.length ? 'View showtimes' : 'Movie details'} <ArrowUpRight size={17} /></button>
  </article>;
}

export function BookingCard({ booking, onCancel, onResume, busy }) {
  const expired = booking.status === 'EXPIRED' || (booking.status === 'PENDING' && secondsRemaining(booking.expires_at) === 0);
  const status = expired ? 'EXPIRED' : booking.status;
  return <article className="booking-card">
    <div className="booking-icon"><Ticket size={28} /></div>
    <div className="booking-card-info"><span className="eyebrow">BOOKING #{String(booking.id).padStart(5, '0')}</span><h3>{booking.movie_title}</h3><p>{dayLabel(booking.start_time)} at {time(booking.start_time)} · {booking.room_name}</p><p>Seats {booking.seats?.join(', ') || '—'}</p><p>Booked at ({viewerZone}): {bookedAtLabel(booking.created_at)}</p></div>
    <div className="booking-card-end"><span className={`status status-${status.toLowerCase()}`}>{status === 'PENDING' ? 'Awaiting payment' : status.toLowerCase()}</span><strong>{money(booking.total_price)}</strong>{status === 'PENDING' && <div className="booking-actions"><button className="text-button" disabled={busy} onClick={() => onResume(booking)}>Payment options</button><button className="text-button muted" disabled={busy} onClick={() => onCancel(booking)}>Cancel booking</button></div>}</div>
  </article>;
}

export function StepLabel({ number, children, active, done }) {
  return <span className={`step-label ${active ? 'active' : ''} ${done ? 'done' : ''}`}><i>{done ? <Check size={12} /> : number}</i>{children}</span>;
}

export function SeatSummary({ seats, selected }) {
  return <span>{seats.filter(seat => selected.includes(seat.id)).map(seatLabel).join(', ') || 'Choose your seats'}</span>;
}
