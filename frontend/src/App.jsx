import { useState, useEffect, useCallback, useMemo, useId } from 'react';
import { ArrowRight, ArrowUpRight, Bookmark, CalendarDays, Check, ChevronDown, CircleHelp, Clapperboard, Clock3, Eye, EyeOff, Film, LogOut, MapPin, Search, ShieldCheck, Sparkles, Ticket, UserRound, X } from 'lucide-react';
import { request, readSession, revokeSession, saveSession, updateSessionUser } from './api.js';
import GoogleSignIn from './GoogleSignIn.jsx';
import PaymentCheckout from './PaymentCheckout.jsx';
import AdminWorkspace from './AdminWorkspace.jsx';
import { isOperator, isDemoPayment } from './adminModel.js';
import { dateKey, dayLabel, filterMovies, groupSeats, money, seatLabel, secondsRemaining, selectionTotal, time, upcomingShows } from './model.js';
import { BookingCard, Dialog, MovieCard, Poster, SeatSummary, StepLabel } from './components.jsx';

function App() {
  const [movies, setMovies] = useState([]);
  const [shows, setShows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [catalogError, setCatalogError] = useState('');
  const [session, setSession] = useState(readSession);
  const [view, setView] = useState(() => window.location.hash === '#admin' ? 'admin' : 'movies');
  const [tab, setTab] = useState('showing');
  const [query, setQuery] = useState('');
  const [language, setLanguage] = useState('all');
  const [date, setDate] = useState('all');
  const [saved, setSaved] = useState(() => { try { return JSON.parse(localStorage.getItem('ticketbox-watchlist')) || []; } catch { return []; } });
  const [movie, setMovie] = useState(null);
  const [auth, setAuth] = useState(null);
  const [help, setHelp] = useState(false);
  const [toast, setToast] = useState('');
  const [bookings, setBookings] = useState([]);
  const [bookingsLoading, setBookingsLoading] = useState(false);
  const [bookingError, setBookingError] = useState('');
  const [paymentBooking, setPaymentBooking] = useState(null);
  const [busy, setBusy] = useState(false);

  const notify = useCallback(message => setToast(message), []);
  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(''), 5000); return () => clearTimeout(timer); }, [toast]);

  const loadCatalog = useCallback(async () => {
    setLoading(true); setCatalogError('');
    try { const [movieData, showData] = await Promise.all([request('/movies'), request('/shows')]); setMovies(movieData); setShows(showData); }
    catch { setCatalogError('We couldn’t load the movies. Check that Ticketbox is running, then try again.'); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { loadCatalog(); }, [loadCatalog]);

  const signOut = useCallback(() => {
    const previous = readSession();
    window.history.replaceState(null, '', window.location.pathname + window.location.search);
    window.google?.accounts?.id?.disableAutoSelect();
    saveSession(null);
    setSession(null);
    setBookings([]);
    setPaymentBooking(null);
    setView('movies');
    revokeSession(previous).catch(() => notify('Signed out here. The server could not revoke the refresh session.'));
  }, [notify]);
  useEffect(() => {
    const onRefresh = event => setSession(event.detail);
    window.addEventListener('ticketbox-session-refreshed', onRefresh);
    return () => window.removeEventListener('ticketbox-session-refreshed', onRefresh);
  }, []);
  useEffect(() => {
    if (!session?.token) return;
    const controller = new AbortController();
    request('/auth/me', { token: session.token, signal: controller.signal }).then(user => {
      if (controller.signal.aborted) return;
      const next = updateSessionUser(user);
      if (next) setSession(next);
    }).catch(error => { if (!controller.signal.aborted && (error.status === 401 || error.status === 403)) { signOut(); notify('Your session ended. Please sign in again.'); } });
    return () => controller.abort();
  }, [session?.token, signOut, notify]);

  const loadBookings = useCallback(async () => {
    if (!session?.user) return;
    setBookingsLoading(true); setBookingError('');
    try { setBookings(await request(`/bookings/user/${session.user.id}`, { token: session.token })); }
    catch (error) { setBookingError(error.message); }
    finally { setBookingsLoading(false); }
  }, [session]);
  useEffect(() => { if (view === 'bookings') loadBookings(); }, [view, loadBookings]);

  const availableShows = useMemo(() => upcomingShows(shows), [shows]);
  const shownMovies = filterMovies(movies, availableShows, { query, language, date, tab: view === 'saved' ? 'saved' : tab, saved });
  const dates = [...new Set(availableShows.map(show => dateKey(show.start_time)))];
  const showingCount = movies.filter(item => availableShows.some(show => show.movie.id === item.id)).length;
  const upcomingCount = movies.length - showingCount;

  function toggleSave(id) {
    const next = saved.includes(id) ? saved.filter(item => item !== id) : [...saved, id];
    setSaved(next); localStorage.setItem('ticketbox-watchlist', JSON.stringify(next));
  }

  function navigate(next) {
    setView(next); setQuery(''); setDate('all'); setLanguage('all');
    window.history.replaceState(null, '', next === 'admin' ? '#admin' : window.location.pathname + window.location.search);
    if (next === 'movies') setTab('showing');
    window.scrollTo?.({ top: 0, behavior: 'smooth' });
  }

  function onAuthenticated(next) {
    setSession(next); saveSession(next); setAuth(null); notify(`You're in. Welcome, ${next.user.username}!`);
    if (isOperator(next.user)) navigate('admin');
  }

  async function cancelBooking(booking) {
    setBusy(true);
    try { await request(`/bookings/${booking.id}/cancel`, { method: 'PATCH', token: session.token }); await loadBookings(); notify('Booking cancelled. Your seats have been released.'); }
    catch (error) { notify(error.message); }
    finally { setBusy(false); }
  }

  const closeMovie = useCallback(() => setMovie(null), []);
  const closeAuth = useCallback(() => setAuth(null), []);
  const closeHelp = useCallback(() => setHelp(false), []);
  const closePayment = useCallback(() => setPaymentBooking(null), []);

  if (view === 'admin') return <><AdminWorkspace session={session} onExit={() => navigate('movies')} onSignOut={signOut} onSignIn={() => setAuth('login')} onCatalogChanged={loadCatalog} />{auth && <AuthDialog mode={auth} setMode={setAuth} onClose={closeAuth} onAuthenticated={onAuthenticated} />}</>;

  return <div className="app-shell">
    <aside className="sidebar">
      <button className="brand" aria-label="Ticketbox home" onClick={() => navigate('movies')}><span className="brand-symbol"><Ticket size={26} strokeWidth={2.3} /></span>ticketbox<span className="brand-dot">.</span></button>
      <span className="nav-caption">YOUR NEXT GREAT NIGHT</span>
      <nav aria-label="Main navigation">
        {isOperator(session?.user) && <button className="nav-item" onClick={() => navigate('admin')}><ShieldCheck size={19} />Admin workspace</button>}
        <button className={`nav-item ${view === 'movies' ? 'selected' : ''}`} onClick={() => navigate('movies')}><Clapperboard size={19} />Discover movies <ArrowUpRight size={15} className="nav-arrow" /></button>
        <button className={`nav-item ${view === 'bookings' ? 'selected' : ''}`} onClick={() => navigate('bookings')}><Ticket size={19} />My bookings</button>
        <button className={`nav-item ${view === 'saved' ? 'selected' : ''}`} onClick={() => navigate('saved')}><Bookmark size={18} />Watchlist <span className="nav-count">{saved.length}</span></button>
      </nav>
      <div className="sidebar-note"><span className="note-spark">✳</span><h3>Less scrolling.<br />More cinema.</h3><p>A good story is even better on the big screen.</p><button onClick={() => { navigate('movies'); document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth' }); }}>Find your next favorite <ArrowUpRight size={16} /></button></div>
      <div className="sidebar-bottom"><button className="nav-item" aria-label="Help" onClick={() => setHelp(true)}><CircleHelp size={18} /><span>A little help?</span></button><div className="sidebar-footnote"><span className="live-dot" />Made for movie people.</div></div>
    </aside>

    <div className="workspace">
      <header className="topbar"><div className="breadcrumb">The cinema is calling <span>✳</span></div><label className="search-field"><Search size={18} /><input type="search" aria-label="Search movies" placeholder="Find a movie, a director, a story..." value={query} onChange={event => { setQuery(event.target.value); if (view === 'bookings') setView('movies'); }} /><span className="search-hint">⌕</span></label><div className="topbar-actions"><span className="location"><MapPin size={16} />Ho Chi Minh City</span>{session ? <div className="account"><span className="avatar">{session.user?.username?.slice(0, 1).toUpperCase() || <UserRound size={16} />}</span><span className="account-name">{session.user?.username}</span><button className="icon-button" aria-label="Sign out" onClick={signOut}><LogOut size={17} /></button></div> : <button className="sign-in-button" onClick={() => setAuth('login')}><UserRound size={16} />Sign in</button>}</div></header>

      <main>
        {view === 'movies' && <>
          <div className="page-intro"><div><span className="eyebrow">GOOD STORIES START HERE</span><h1>Let’s go to the movies<span className="lime-period">.</span></h1></div><span className="intro-note"><Sparkles size={15} /> A little out of the ordinary.</span></div>
          <section className="hero" aria-label="Discover your next movie"><div className="hero-copy"><span className="hero-kicker"><span className="live-dot" />LIGHTS DOWN. WORLD OFF.</span><h2>A little escape.<br /><em>A big screen.</em></h2><p>New worlds. Unforgettable stories.<br />Your next favorite film is waiting.</p><button className="primary-button" onClick={() => document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth' })}>Find my next movie <ArrowUpRight size={18} /></button><span className="hero-bottom">FOR THE LOVE OF THE BIG SCREEN <span>↗</span></span></div><div className="hero-art" aria-hidden="true"><div className="orbit-line orbit-one" /><div className="orbit-line orbit-two" /><div className="orbit-line orbit-three" /><div className="hero-planet" /><span className="hero-star star-one">✦</span><span className="hero-star star-two">✧</span><span className="hero-star star-three">+</span><div className="hero-stamp">ONE TICKET.<br /><strong>ENDLESS<br />POSSIBILITIES.</strong><span>TAKE A SEAT ↗</span></div><span className="art-caption">SOME STORIES DESERVE<br />A BIGGER CANVAS.</span></div><span className="hero-index">01 / THE CINEMA EDIT</span></section>
        </>}

        {view !== 'bookings' ? <section id="catalog" className="catalog-section">
          <div className="section-heading"><div><span className="eyebrow">{view === 'saved' ? 'SAVED FOR A GOOD NIGHT' : 'THE LINEUP'}</span><h2>{view === 'saved' ? 'Your watchlist' : 'Find your kind of story'}<span className="result-count">{shownMovies.length}</span></h2></div><div className="catalog-selects"><label><CalendarDays size={16} /><select aria-label="Filter by date" value={date} onChange={event => setDate(event.target.value)}><option value="all">All dates</option>{dates.map(value => <option key={value} value={value}>{dayLabel(`${value}T12:00:00`)}</option>)}</select><ChevronDown size={13} /></label><label><Film size={16} /><select aria-label="Filter by language" value={language} onChange={event => setLanguage(event.target.value)}><option value="all">All languages</option>{[...new Set(movies.map(item => item.language))].sort().map(value => <option key={value} value={value}>{value}</option>)}</select><ChevronDown size={13} /></label></div></div>
          <div className="catalog-toolbar">{view === 'movies' ? <div className="tabs" role="tablist" aria-label="Movie availability"><button role="tab" aria-selected={tab === 'showing'} className={tab === 'showing' ? 'active' : ''} onClick={() => setTab('showing')}>Now showing <span>{showingCount}</span></button><button role="tab" aria-selected={tab === 'upcoming'} className={tab === 'upcoming' ? 'active' : ''} onClick={() => setTab('upcoming')}>Coming soon <span>{upcomingCount}</span></button></div> : <p className="muted">A few stories to look forward to.</p>}<span className="catalog-note">A seat for every story.</span></div>
          {loading ? <div className="movie-grid" aria-label="Loading movies" aria-busy="true">{[1, 2, 3, 4].map(value => <div key={value} className="movie-skeleton"><div /><span /><span /></div>)}</div> : catalogError ? <div className="empty-state" role="alert"><Film size={36} /><h3>The intermission ran a little long.</h3><p>{catalogError}</p><button className="primary-button" onClick={loadCatalog}>Try again <ArrowRight size={17} /></button></div> : shownMovies.length ? <div className="movie-grid">{shownMovies.map(item => <MovieCard key={item.id} movie={item} shows={availableShows.filter(show => show.movie.id === item.id)} saved={saved.includes(item.id)} onSave={toggleSave} onSelect={setMovie} />)}</div> : <div className="empty-state"><Bookmark size={34} /><h3>{view === 'saved' && !query ? 'Your next favorites belong here.' : 'No movies in this scene.'}</h3><p>{view === 'saved' && !saved.length ? 'Tap the bookmark on a movie to save it for later.' : 'Try a different search, language, or date.'}</p><button className="text-button" onClick={() => { setQuery(''); setDate('all'); setLanguage('all'); if (view === 'saved' && !saved.length) navigate('movies'); }}>Explore the lineup <ArrowRight size={16} /></button></div>}
        </section> : <section className="bookings-section"><span className="eyebrow">GOOD NIGHTS, ALL IN ONE PLACE</span><h1>My bookings<span className="lime-period">.</span></h1><p className="page-description">Your plans for the big screen.</p>{!session ? <div className="empty-state"><Ticket size={36} /><h3>Your movie nights live here.</h3><p>Sign in to see your reservations and payment details.</p><button className="primary-button" onClick={() => setAuth('login')}>Sign in <ArrowRight size={17} /></button></div> : bookingsLoading ? <p className="loading-message" role="status">Finding your tickets…</p> : bookingError ? <div className="empty-state" role="alert"><p>{bookingError}</p><button className="primary-button" onClick={loadBookings}>Try again</button></div> : bookings.length ? <div className="booking-list">{[...bookings].reverse().map(booking => <BookingCard key={booking.id} booking={booking} onCancel={cancelBooking} onResume={setPaymentBooking} busy={busy} />)}</div> : <div className="empty-state"><Ticket size={36} /><h3>Your first movie night awaits.</h3><p>Find a film, choose your seats, and make a night of it.</p><button className="primary-button" onClick={() => navigate('movies')}>Discover movies <ArrowUpRight size={17} /></button></div>}</section>}

        <footer className="main-footer"><span>Small plans. Big-screen memories.</span><span>TICKETBOX <span className="footer-asterisk">✳</span> SEE YOU AT THE MOVIES</span></footer>
      </main>
    </div>
    {movie && <MovieDialog movie={movie} shows={availableShows.filter(show => show.movie.id === movie.id)} session={session} onClose={closeMovie} onSignIn={() => setAuth('login')} onBooked={booking => { setMovie(null); setPaymentBooking(booking); setView('bookings'); loadCatalog(); }} />}
    {auth && <AuthDialog mode={auth} setMode={setAuth} onClose={closeAuth} onAuthenticated={onAuthenticated} />}
    {paymentBooking && <PaymentDialog booking={paymentBooking} session={session} onClose={closePayment} onChanged={() => { loadBookings(); loadCatalog(); }} />}
    {help && <Dialog title="A little help" onClose={closeHelp}><h2>Your movie night, made easy.</h2><div className="help-list"><p><strong>1. Find your film.</strong><br />Browse the lineup and pick a showtime that works for you.</p><p><strong>2. Make yourself comfortable.</strong><br />Sign in, choose your seats, and reserve them for 10 minutes.</p><p><strong>3. Check your payment.</strong><br />Create a payment request under My bookings. Your booking is confirmed once the cinema verifies payment.</p><p><strong>Change of plans?</strong><br />Cancel a pending booking from My bookings to release your seats.</p></div><button className="primary-button" onClick={closeHelp}>Got it <Check size={16} /></button></Dialog>}
    {toast && <div className="toast" role="status"><Check size={17} />{toast}<button className="icon-button" aria-label="Dismiss notification" onClick={() => setToast('')}><X size={16} /></button></div>}
  </div>;
}

export function AuthDialog({ mode, setMode, onClose, onAuthenticated }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const passwordId = useId();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const signup = mode === 'signup';

  function switchMode() {
    setUsername('');
    setPassword('');
    setPasswordVisible(false);
    setError('');
    setMode(signup ? 'login' : 'signup');
  }
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const { token } = await request(`/auth/${signup ? 'signup' : 'login'}`, { method: 'POST', body: { username: username.trim(), password } });
      const user = await request('/auth/me', { token });
      onAuthenticated({ token, user });
    } catch (error) { setError(error.message); }
    finally { setBusy(false); }
  }
  return (
    <Dialog title={signup ? 'Create an account' : 'Welcome back'} onClose={onClose}>
      <div className="dialog-emblem"><Ticket size={28} /></div>
      <h2>{signup ? 'Your next great night starts here.' : 'Good to see you again.'}</h2>
      <p className="muted">{signup ? 'A few details, and you’re one step closer to the big screen.' : 'Sign in to save your seat and find your bookings.'}</p>
      <GoogleSignIn onAuthenticated={onAuthenticated} />
      <form className="auth-form" onSubmit={submit}>
        <label>
          Username
          <input autoComplete="username" required minLength={signup ? 3 : 1} maxLength={30} value={username} onChange={event => setUsername(event.target.value)} placeholder="Your username" />
        </label>
        <div className="auth-password-field">
          <label htmlFor={passwordId}>Password</label>
          <div className="auth-password-input">
            <input id={passwordId} type={passwordVisible ? 'text' : 'password'} autoComplete={signup ? 'new-password' : 'current-password'} required minLength={signup ? 8 : 1} maxLength={64} value={password} onChange={event => setPassword(event.target.value)} placeholder={signup ? 'At least 8 characters' : 'Your password'} />
            <button
              type="button"
              className="icon-button"
              aria-label={passwordVisible ? 'Hide password' : 'Show password'}
              aria-controls={passwordId}
              onClick={() => setPasswordVisible(visible => !visible)}
            >
              {passwordVisible ? <EyeOff size={17} aria-hidden="true" /> : <Eye size={17} aria-hidden="true" />}
            </button>
          </div>
        </div>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <button className="primary-button" disabled={busy}>{busy ? 'One moment…' : signup ? 'Create account' : 'Sign in'}<ArrowRight size={17} /></button>
      </form>
      <p className="auth-switch">
        {signup ? 'Already part of the audience?' : 'New around here?'}{' '}
        <button type="button" disabled={busy} onClick={switchMode}>{signup ? 'Sign in' : 'Create an account'}</button>
      </p>
    </Dialog>
  );
}

export function MovieDialog({ movie, shows, session, onClose, onSignIn, onBooked }) {
  const [show, setShow] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selected, setSelected] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [seatDate, setSeatDate] = useState(shows[0] ? dateKey(shows[0].start_time) : '');
  const dates = [...new Set(shows.map(item => dateKey(item.start_time)))];

  useEffect(() => {
    if (!show) return;
    const controller = new AbortController();
    setLoading(true); setSelected([]); setError(''); setSeats([]);
    request(`/show-seats/show/${show.id}`, { signal: controller.signal }).then(setSeats).catch(error => { if (error.name !== 'AbortError') setError(error.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [show]);

  async function reserve() {
    if (!session) { onSignIn(); return; }
    setBusy(true); setError('');
    try {
      const booking = await request('/bookings/newbook', { token: session.token, method: 'POST', body: { userId: session.user.id, showId: show.id, showSeatIds: selected } });
      onBooked(booking);
    } catch (error) {
      setError(error.message);
      if (error.status === 409) { try { setSeats(await request(`/show-seats/show/${show.id}`)); setSelected([]); } catch { /* Keep the original reservation error visible. */ } }
      if (error.status === 401) onSignIn();
    } finally { setBusy(false); }
  }

  return <Dialog title="Make it a movie night" onClose={onClose} wide><div className="movie-dialog-header"><div className="dialog-poster"><Poster movie={movie} /></div><div><span className="eyebrow">{shows.length ? 'NOW SHOWING / TICKETBOX SAIGON' : 'COMING TO THE BIG SCREEN'}</span><h2>{movie.title}</h2><p className="movie-meta">{movie.language} <span>·</span> {movie.duration_min} minutes <span>·</span> Directed by {movie.author}</p><p className="movie-description">{movie.description || 'Some stories are better together. Experience this film on the big screen at Ticketbox.'}</p></div></div>{shows.length ? <><div className="booking-steps"><StepLabel number="1" active={!show} done={!!show}>Choose a showtime</StepLabel><span /><StepLabel number="2" active={!!show}>Pick your seats</StepLabel></div><div className="showtime-dates">{dates.map(value => <button key={value} className={seatDate === value ? 'active' : ''} onClick={() => { setSeatDate(value); setShow(null); setSeats([]); setSelected([]); setError(''); }}>{dayLabel(`${value}T12:00:00`)}</button>)}</div><div className="showtime-list">{shows.filter(item => dateKey(item.start_time) === seatDate).map(item => <button key={item.id} aria-pressed={show?.id === item.id} className={show?.id === item.id ? 'active' : ''} onClick={() => setShow(item)}><strong>{time(item.start_time)}</strong><span>{item.room.name} · 2D</span></button>)}</div>{show && <section className="seat-picker" aria-label="Choose your seats">{loading ? <p className="loading-message" role="status">Setting the scene…</p> : seats.length ? <><div className="screen"><span>THE BIG SCREEN</span></div><div className="seat-map">{groupSeats(seats).map(([row, rowSeats]) => <div className="seat-row" key={row}><span className="row-label">{row}</span><div>{rowSeats.map((seat, index) => <button key={seat.id} aria-label={`Seat ${seatLabel(seat)}, ${money(seat.price)}${seat.status !== 'AVAILABLE' ? ', unavailable' : ''}`} aria-pressed={selected.includes(seat.id)} disabled={seat.status !== 'AVAILABLE' || busy} className={`seat ${seat.seat.seat_type === 'VIP' ? 'vip' : ''} ${selected.includes(seat.id) ? 'selected' : ''} ${index === Math.floor(rowSeats.length / 2) ? 'aisle' : ''}`} onClick={() => setSelected(previous => previous.includes(seat.id) ? previous.filter(id => id !== seat.id) : [...previous, seat.id])}>{selected.includes(seat.id) ? <Check size={13} /> : seat.seat.seat_number}</button>)}</div><span className="row-label">{row}</span></div>)}</div><div className="seat-legend"><span><i />Available</span><span><i className="vip" />VIP</span><span><i className="selected" />Your pick</span><span><i className="unavailable" />Unavailable</span></div><div className="selection-bar"><div><span className="eyebrow">YOUR SEATS</span><p><SeatSummary seats={seats} selected={selected} /></p></div><div><span className="eyebrow">TOTAL</span><strong>{money(selectionTotal(seats, selected))}</strong></div><button className="primary-button" disabled={!selected.length || busy} onClick={reserve}>{busy ? 'Reserving…' : session ? 'Reserve seats' : 'Sign in to reserve'}<ArrowRight size={17} /></button></div><p className="reservation-note"><ShieldCheck size={14} />Your seats are held for 10 minutes while you arrange payment.</p></> : !error && <p className="muted">Seats haven’t opened for this show yet. Please try another time.</p>}</section>}{error && <p className="inline-error" role="alert">{error}</p>}</> : <div className="coming-soon-note"><Sparkles size={21} /><div><h3>Good things are on their way.</h3><p>Showtimes will appear here when booking opens.</p></div></div>}</Dialog>;
}

export function PaymentDialog({ booking, session, onClose, onChanged }) {
  const [remaining, setRemaining] = useState(() => secondsRemaining(booking.expires_at));
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const load = useCallback(async () => {
    setError('');
    try { setPayments(await request(`/payments/booking/${booking.id}`, { token: session.token })); }
    catch (error) { setError(error.message); }
    finally { setLoading(false); }
  }, [booking.id, session.token]);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { const timer = setInterval(() => setRemaining(secondsRemaining(booking.expires_at)), 1000); return () => clearInterval(timer); }, [booking.expires_at]);
  const active = payments.find(payment => ['PENDING', 'PAID'].includes(payment.status));
  const paid = active?.status === 'PAID';
  async function createPayment() {
    setBusy(true); setError('');
    try { const payment = await request('/payments', { token: session.token, method: 'POST', body: { booking_id: booking.id, method: 'BANK_TRANSFER' } }); setPayments(previous => [payment, ...previous]); onChanged(); }
    catch (error) { setError(error.message); }
    finally { setBusy(false); }
  }
  return <Dialog title="Your reservation" onClose={onClose}><div className="dialog-emblem"><Check size={29} /></div><h2>{paid ? 'You’re going to the movies.' : remaining ? 'Good choice. Great seats.' : 'This reservation has expired.'}</h2><p className="muted">{paid ? (isDemoPayment(active) ? 'Demo payment successful. Your demo booking is confirmed; no money was transferred.' : 'Your payment is verified and your booking is confirmed.') : remaining ? 'Your seats are reserved. Here’s what’s next.' : 'Choose another show or seats to book again. If you already transferred money, contact the cinema with your payment reference.'}</p><div className="ticket-summary"><div><span className="eyebrow">BOOKING #{String(booking.id).padStart(5, '0')}</span><h3>{booking.movie_title}</h3><p>{dayLabel(booking.start_time)} · {time(booking.start_time)}</p><p>{booking.room_name} · Seats {booking.seats?.join(', ')}</p></div><div className="ticket-total"><span>Total</span><strong>{money(booking.total_price)}</strong></div></div>{!paid && remaining > 0 && <div className="hold-countdown"><Clock3 size={17} />Seats held for <strong>{Math.floor(remaining / 60)}:{String(remaining % 60).padStart(2, '0')}</strong></div>}{loading ? <p role="status">Checking payment details…</p> : paid ? <p className="success-note"><ShieldCheck size={18} />{isDemoPayment(active) ? 'Demo confirmed. No real charge.' : 'Payment confirmed. See you at the movies!'}</p> : remaining > 0 && (active ? <div className="payment-notice"><PaymentCheckout payment={active} token={session.token} expired={remaining === 0} onSettled={result => { setPayments(previous => previous.map(payment => payment.id === result.id ? result : payment)); onChanged(); }} /><button className="text-button" onClick={async () => { setLoading(true); await load(); onChanged(); }}>Refresh payment status <ArrowRight size={16} /></button></div> : <div className="payment-form"><h3>QR payment</h3><p className="muted small">Generate a payment QR for your reservation. The checkout shows whether you are using SePay, SePay Test mode, or the local demo.</p><button className="primary-button" disabled={busy || !!error} onClick={createPayment}>{busy ? 'Generating QR…' : 'Show payment QR'}<ArrowRight size={17} /></button></div>)}{error && <p className="inline-error" role="alert">{error} <button className="text-button" onClick={load}>Retry</button></p>}<button className="secondary-button full-width" onClick={onClose}>Back to my bookings</button></Dialog>;
}

export default App;
