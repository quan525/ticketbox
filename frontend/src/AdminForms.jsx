import { useEffect, useState } from 'react';
import { AlertCircle, ArrowRight, Armchair, Check, ShieldCheck } from 'lucide-react';
import { Dialog } from './components.jsx';
import { request } from './api.js';
import { cinemaDate, venueDateKey, venueDayLabel, groupSeats, money, venueTime } from './model.js';
import { bookingState, labelStatus, localDateTime, permissionModules, hasPermission } from './adminModel.js';

export function RoomSeatPreview({ room, token, onClose, canManage, onChanged }) {
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [selected, setSelected] = useState(null);
  const [category, setCategory] = useState('VIP');
  const [rowLabel, setRowLabel] = useState('A');
  const [seatNumber, setSeatNumber] = useState('');
  const rows = Array.from({ length: room.seat_rows }, (_, index) => String.fromCharCode(65 + index));
  useEffect(() => {
    const controller = new AbortController();
    request(`/seats/room/${room.id}`, { token, signal: controller.signal })
      .then(result => { if (!controller.signal.aborted) setSeats(result); })
      .catch(failure => { if (!controller.signal.aborted) setLoadError(failure.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [room.id, token]);

  async function saveSeat(event, adding) {
    event.preventDefault();
    if (!canManage || busy || (!adding && !selected)) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const updated = adding
        ? await request(`/seats/room/${room.id}`, { token, method: 'POST', body: {
          row_label: rowLabel, seat_number: Number(seatNumber), seat_type: 'VIP',
        } })
        : await request(`/seats/${selected.id}`, { token, method: 'PUT', body: { seat_type: category } });
      setSeats(previous => adding ? [...previous, updated] : previous.map(seat => seat.id === updated.id ? updated : seat));
      setSelected(updated);
      setCategory(updated.seat_type);
      if (adding) setSeatNumber('');
      setNotice(`Seat ${updated.row_label}${updated.seat_number} saved as ${updated.seat_type}.`);
      onChanged?.();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  return <Dialog title={canManage ? 'Manage room seats' : 'Room layout'} onClose={onClose} wide>
    <div className="admin-dialog-heading">
      <span className="admin-kicker">ROOM / {String(room.id).padStart(2, '0')}</span>
      <h2>{room.name}</h2>
      <p>{loading ? room.seat_count : seats.length} seats · {room.seat_rows} rows</p>
    </div>
    {loading ? <p role="status">Loading the room…</p> : loadError ? <p className="inline-error" role="alert">{loadError}</p> : <>
      <div className="screen"><span>SCREEN</span></div>
      <div className="admin-room-map">
        {groupSeats(seats.map(seat => ({ seat }))).map(([row, values]) => (
          <div className="admin-seat-row" key={row}><span>{row}</span>
            {values.map(({ seat }) => {
              const label = `${row}${seat.seat_number} · ${seat.seat_type}`;
              const className = `admin-layout-seat ${seat.seat_type === 'VIP' ? 'vip' : ''}`;
              return canManage
                ? <button key={seat.id} type="button" className={className} title={label} aria-label={label}
                    aria-pressed={selected?.id === seat.id} disabled={busy}
                    onClick={() => { setSelected(seat); setCategory(seat.seat_type); setNotice(''); setError(''); }}>
                    {seat.seat_number}
                  </button>
                : <span key={seat.id} className={className} title={label}>{seat.seat_number}</span>;
            })}
          <span>{row}</span></div>
        ))}
      </div>
      <p className="admin-map-note"><Armchair size={15} />Standard <i />VIP</p>
      {canManage && <>
        <p className="admin-form-hint">Select a seat to change its category. New screenings use the updated room layout. Existing screenings keep their categories and prices.</p>
        {selected && <form className="admin-form admin-seat-form" onSubmit={event => saveSeat(event, false)}>
          <label>Seat {selected.row_label}{selected.seat_number} category
            <select value={category} onChange={event => setCategory(event.target.value)} disabled={busy}>
              <option value="STANDARD">Standard</option>
              <option value="VIP">VIP</option>
              <option value="COUPLE">Couple</option>
              <option value="NORMAL">Normal</option>
            </select>
          </label>
          <button className="primary-button" disabled={busy || category === selected.seat_type}>Save category</button>
        </form>}
        <form className="admin-form admin-seat-form" onSubmit={event => saveSeat(event, true)}>
          <label>Row<select value={rowLabel} onChange={event => setRowLabel(event.target.value)} disabled={busy}>
            {rows.map(row => <option key={row} value={row}>{row}</option>)}
          </select></label>
          <label>Seat number<input type="number" min="1" max="2147483647" step="1" required value={seatNumber}
            onChange={event => setSeatNumber(event.target.value)} disabled={busy} /></label>
          <button className="secondary-button" disabled={busy || !rows.length}>Add VIP seat</button>
        </form>
        {error && <p className="inline-error" role="alert">{error}</p>}
        {notice && <p role="status">{notice}</p>}
      </>}
    </>}
  </Dialog>;
}

const titles = { 'add-employee': 'Add employee', movie: 'Movie details', room: 'Create a room', show: 'Schedule a screening', payment: 'Review payment', 'cancel-payment': 'Reject payment request', 'cancel-booking': 'Cancel reservation', archive: 'Movie visibility', permissions: 'Account permissions', role: 'Change account role', booking: 'Booking details', customer: 'Customer details' };

export default function AdminEditor({ editor, data, session, onClose, onSave }) {
  const { kind, item } = editor;
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const [values, setValues] = useState(() => ({
    title: item?.title || '', author: item?.author || '', description: item?.description || '', duration_min: item?.duration_min || 110, language: item?.language || 'EN',
    name: '', rows: 6, seats_per_row: 10, seat_type: 'STANDARD',
    movie_id: data.movies.find(movie => !movie.archived)?.id || '', room_id: data.rooms.find(room => room.seat_count > 0)?.id || '', start_time: `${venueDateKey(tomorrow)}T18:30`,
    employee_id: data.users[0]?.id || '', transaction_id: '', verified: false, role: item?.role || 'USER', use_role_defaults: !item?.custom_permissions, permissions: item?.permissions || [],
  }));
  const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  const field = (name, label, props = {}) => <label>{label}<input name={name} value={values[name]} onChange={event => setValues(previous => ({ ...previous, [name]: event.target.value }))} {...props} /></label>;
  const select = (name, label, options) => <label>{label}<select name={name} value={values[name]} onChange={event => setValues(previous => ({ ...previous, [name]: event.target.value }))}>{options.map(([value, text]) => <option key={value} value={value}>{text}</option>)}</select></label>;
  const relatedBooking = kind.includes('payment') ? data.bookings.find(booking => booking.id === item.booking_id) : kind === 'booking' ? item : null;
  const customer = relatedBooking ? data.users.find(user => user.id === relatedBooking.user_id) : kind === 'customer' ? item : null;
  const selectedMovie = data.movies.find(movie => movie.id === Number(values.movie_id));
  const end = selectedMovie && values.start_time ? new Date(cinemaDate(values.start_time).getTime() + selectedMovie.duration_min * 60000) : null;
  const readOnly = ['booking', 'customer'].includes(kind);

  async function submit(event) {
    event.preventDefault(); if (busy) return;
    setBusy(true); setError('');
    try {
      let body;
      if (kind === 'movie') body = { title: values.title.trim(), author: values.author.trim(), description: values.description.trim(), duration_min: Number(values.duration_min), language: values.language.trim().toUpperCase() };
      if (kind === 'room') body = { name: values.name.trim(), rows: Number(values.rows), seats_per_row: Number(values.seats_per_row), seat_type: values.seat_type };
      if (kind === 'show') body = { movie_id: Number(values.movie_id), room_id: Number(values.room_id), start_time: values.start_time.length === 16 ? `${values.start_time}:00` : values.start_time };
      if (kind === 'payment') body = { transaction_id: values.transaction_id.trim() };
      if (kind === 'role') body = { role: values.role };
      if (kind === 'permissions') body = { use_role_defaults: values.use_role_defaults, permissions: values.permissions };
      if (kind === 'archive') body = { archived: !item.archived };
      if (kind === 'add-employee') await onSave('role', data.users.find(user => user.id === Number(values.employee_id)), { role: 'MODERATOR' });
      else await onSave(kind, item, body);
      onClose();
    } catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }

  return <Dialog title={titles[kind]} onClose={busy ? () => {} : onClose} wide={kind === 'movie'}><div className="admin-dialog-heading"><span className="admin-kicker">TICKETBOX OPERATIONS</span><h2>{kind === 'movie' ? item ? 'Edit this story.' : 'Add something worth watching.' : titles[kind]}</h2></div>
    <form className="admin-form" onSubmit={submit}>
      {kind === 'movie' && <><div className="admin-form-grid">{field('title', 'Movie title', { required: true, maxLength: 255 })}{field('author', 'Director', { required: true, maxLength: 255 })}{field('duration_min', 'Runtime (minutes)', { required: true, type: 'number', min: 1, max: 600 })}{field('language', 'Language code', { required: true, maxLength: 10, placeholder: 'EN, VI, JA…' })}</div><label>Synopsis<textarea name="description" rows={4} value={values.description} onChange={event => setValues(previous => ({ ...previous, description: event.target.value }))} /></label><p className="admin-form-hint">New movies appear under Coming soon until you schedule a screening.</p></>}
      {kind === 'room' && <>{field('name', 'Room name', { required: true, maxLength: 100, placeholder: 'Screen 02' })}<div className="admin-form-grid">{field('rows', 'Rows', { required: true, type: 'number', min: 1, max: 26 })}{field('seats_per_row', 'Seats per row', { required: true, type: 'number', min: 1, max: 30 })}</div>{select('seat_type', 'Seat category', [['STANDARD', 'Standard'], ['VIP', 'VIP']])}<div className="admin-info-banner"><Armchair size={19} /><span>{Number(values.rows) * Number(values.seats_per_row)} seats will be created with this room.</span></div></>}
      {kind === 'show' && <>{select('movie_id', 'Movie', data.movies.filter(movie => !movie.archived).map(movie => [movie.id, `${movie.title} · ${movie.duration_min} min`]))}{select('room_id', 'Room', data.rooms.filter(room => room.seat_count > 0).map(room => [room.id, `${room.name} · ${room.seat_count} seats`]))}{field('start_time', 'Start date and time (Vietnam time)', { required: true, type: 'datetime-local', min: localDateTime() })}<div className="admin-info-banner"><ClockNote /><span>{end ? `Ends ${venueDayLabel(end)} at ${venueTime(end)}. Seats and standard prices are created automatically.` : 'Choose a movie and start time.'}</span></div>{!values.room_id && <p className="inline-error">Create a room with seats first.</p>}<p className="admin-form-hint">The server checks room conflicts before saving. All seats must be created successfully for the screening to be published.</p></>}
      {kind === 'payment' && <><div className="admin-review-summary"><strong>{money(item.amount)}</strong><p>Payment #{item.id} · Booking #{item.booking_id}</p><p>{relatedBooking?.movie_title} · {customer?.username || 'Customer'}</p></div><p className="admin-warning"><AlertCircle size={18} />Only confirm after matching the actual bank transaction, amount, and reference. Viewing or scanning the sample QR is not payment evidence.</p>{field('transaction_id', 'Bank transaction ID', { required: true, maxLength: 100, placeholder: 'Verified transfer reference' })}<label className="admin-checkbox"><input type="checkbox" required checked={values.verified} onChange={event => setValues(previous => ({ ...previous, verified: event.target.checked }))} /><span>I have verified the received amount and transfer reference against the bank record.</span></label></>}
      {kind === 'cancel-payment' && <><p>Reject payment request <strong>#{item.id}</strong> for <strong>{money(item.amount)}</strong>?</p><p className="admin-form-hint">The reservation stays held until its existing expiry time so the customer can make another payment attempt.</p></>}
      {kind === 'cancel-booking' && <><p>Cancel reservation <strong>#{item.id}</strong> for <strong>{item.movie_title}</strong>?</p><p className="admin-form-hint">Seats {item.seats.join(', ')} will be released, and pending payment requests will be cancelled. Confirmed bookings cannot be cancelled here.</p></>}
      {kind === 'archive' && <><p>{item.archived ? 'Restore' : 'Archive'} <strong>{item.title}</strong>?</p><p className="admin-form-hint">{item.archived ? 'The movie will return to the customer catalog.' : 'The movie will be hidden from the customer catalog. Existing booking history stays intact. Movies with remaining screenings cannot be archived yet.'}</p></>}
      {kind === 'add-employee' && <>{select('employee_id', 'Existing customer account', data.users.map(user => [user.id, `${user.username}${user.email ? ` - ${user.email}` : ''}`]))}<p className="admin-warning"><ShieldCheck size={18} />This account will become an employee (Moderator), with full cinema module access by default. After adding the employee, use Permissions to restrict their modules. Only admins can manage employees.</p></>}
      {kind === 'permissions' && <><p>Set module access for <strong>{item.username}</strong>.</p><label className="admin-checkbox"><input type="checkbox" checked={values.use_role_defaults} onChange={event => setValues(previous => ({ ...previous, use_role_defaults: event.target.checked }))} /><span>Use role defaults ({item.role === 'MODERATOR' ? 'all cinema modules' : 'personal bookings only'})</span></label><p className="admin-form-hint">View allows reading that module. Manage also enables its actions and includes View. Unchecked modules stay hidden. Role changes reset these overrides.</p><div className="admin-permission-grid"><div className="admin-permission-header"><strong>Module</strong><span>View</span><span>Manage</span></div>{permissionModules.map(module => <div className="admin-permission-row" key={module.id}><div><strong>{module.name}</strong><small>{module.detail}</small></div>{[module.view, module.manage].map((key, index) => key ? <label key={key}><input type="checkbox" aria-label={`${index ? 'Manage' : 'View'} ${module.name}`} disabled={values.use_role_defaults} checked={values.use_role_defaults ? item.role === 'MODERATOR' : values.permissions.includes(key)} onChange={event => setValues(previous => { let permissions = event.target.checked ? [...previous.permissions, key] : previous.permissions.filter(value => value !== key); if (event.target.checked && index === 1 && !permissions.includes(module.view)) permissions.push(module.view); if (!event.target.checked && index === 0) permissions = permissions.filter(value => value !== module.manage); return { ...previous, permissions }; })} /></label> : <span className="admin-muted" key="none">—</span>)}</div>)}<div className="admin-permission-row"><div><strong>Employees & access</strong><small>Staff accounts, roles and permissions are administrator-only.</small></div><span>Admin</span><span>Admin</span></div></div></>}
      {kind === 'role' && <><p>Change the access level for <strong>{item.username}</strong>.</p>{select('role', 'Account role', [['USER', 'Customer'], ['MODERATOR', 'Employee (Moderator)'], ['ADMIN', 'Admin']])}<p className="admin-warning"><ShieldCheck size={18} />Admins can manage accounts and roles. Moderators can manage cinema operations and payments.</p></>}
      {kind === 'booking' && <><div className="admin-detail-grid"><span>Customer<strong>{customer?.username || `User #${item.user_id}`}</strong></span><span>Status<strong>{labelStatus(bookingState(item))}</strong></span><span>Movie<strong>{item.movie_title}</strong></span><span>Showtime<strong>{venueDayLabel(item.start_time)} · {venueTime(item.start_time)}</strong></span><span>Room & seats<strong>{item.room_name} · {item.seats.join(', ')}</strong></span><span>Total<strong>{money(item.total_price)}</strong></span></div><h3>Payment history</h3>{data.payments.filter(payment => payment.booking_id === item.id).map(payment => <div className="admin-history-row" key={payment.id}><span>#{payment.id} · {labelStatus(payment.status)}</span><strong>{money(payment.amount)}</strong></div>)}{!data.payments.some(payment => payment.booking_id === item.id) && <p className="admin-form-hint">No payment request has been created.</p>}</>}
      {kind === 'customer' && <><div className="admin-detail-grid"><span>Username<strong>{item.username}</strong></span><span>Role<strong>{labelStatus(item.role)}</strong></span><span>Email<strong>{item.email || 'Not provided'}</strong></span></div><h3>Booking history</h3>{!hasPermission(session.user, 'VIEW_BOOKINGS') && <p className="admin-form-hint">Bookings module access is required to view this history.</p>}{data.bookings.filter(booking => booking.user_id === item.id).map(booking => <div className="admin-history-row" key={booking.id}><span>#{booking.id} · {booking.movie_title}<small>{venueDayLabel(booking.start_time)} · {labelStatus(bookingState(booking))}</small></span><strong>{money(booking.total_price)}</strong></div>)}{hasPermission(session.user, 'VIEW_BOOKINGS') && !data.bookings.some(booking => booking.user_id === item.id) && <p className="admin-form-hint">No bookings yet.</p>}</>}
      {error && <p className="inline-error" role="alert">{error}</p>}
      <div className="admin-form-actions"><button type="button" className="secondary-button" disabled={busy} onClick={onClose}>{readOnly ? 'Close details' : 'Cancel'}</button>{!readOnly && <button className={`primary-button ${kind.startsWith('cancel-') ? 'admin-destructive' : ''}`} disabled={busy || (kind === 'show' && (!values.movie_id || !values.room_id))}>{busy ? 'Saving…' : ({ movie: 'Save movie', room: 'Create room & seats', show: 'Schedule screening', payment: 'Confirm verified payment', 'cancel-payment': 'Reject request', 'cancel-booking': 'Confirm cancellation', archive: item?.archived ? 'Restore movie' : 'Archive movie', 'add-employee': 'Add employee', permissions: 'Save permissions', role: 'Save role' })[kind]}{kind === 'payment' ? <Check size={16} /> : <ArrowRight size={16} />}</button>}</div>
    </form>
  </Dialog>;
}

function ClockNote() { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.6" /><path d="M12 6v6l4 2" stroke="currentColor" strokeWidth="1.6" /></svg>; }
