vi.mock('../GoogleSignIn.jsx', () => ({ default: () => null }));
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App, { AuthDialog, MovieDialog, PaymentDialog } from '../App.jsx';
import { request } from '../api.js';
vi.mock('../api.js', async () => ({ ...(await vi.importActual('../api.js')), request: vi.fn() }));
const movie = { id: 1, title: 'The Last Orbit', duration_min: 120, language: 'EN', author: 'Alex Morgan' };
const show = { id: 1, movie, room: { name: 'Screen 01' }, start_time: '2099-05-01T18:30:00' };
const session = { token: 'test-token', user: { id: 7, username: 'cinemafan' } };
beforeEach(() => { request.mockReset(); localStorage.clear(); window.scrollTo = vi.fn(); });
describe('movie discovery', () => {
  it('loads the catalog, searches, and saves movies to the watchlist', async () => {
    request.mockImplementation(path => Promise.resolve(path === '/movies' ? [movie] : [show]));
    const user = userEvent.setup(); render(<App />);
    expect(await screen.findByRole('heading', { name: movie.title })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Save The Last Orbit/ }));
    await user.click(screen.getByRole('button', { name: /Watchlist/ }));
    expect(screen.getByRole('heading', { name: movie.title })).toBeInTheDocument();
    await user.type(screen.getByRole('searchbox'), 'missing');
    expect(screen.getByText('No movies in this scene.')).toBeInTheDocument();
  });
  it('shows a retry action when the API is unavailable', async () => {
    request.mockRejectedValue(new Error('offline')); render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent('couldn’t load');
    expect(screen.getByRole('button', { name: /Try again/ })).toBeInTheDocument();
  });
});
describe('accounts', () => {
  it('submits credentials and retrieves the authenticated account', async () => {
    request.mockResolvedValueOnce({ token: session.token }).mockResolvedValueOnce(session.user);
    const user = userEvent.setup(); const authenticated = vi.fn();
    render(<AuthDialog mode="login" setMode={vi.fn()} onClose={vi.fn()} onAuthenticated={authenticated} />);
    await user.type(screen.getByLabelText('Username'), 'cinemafan');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign in', exact: true }));
    await waitFor(() => expect(authenticated).toHaveBeenCalledWith(session));
  });
});
describe('booking flow', () => {
  const seats = [{ id: 11, price: 100000, status: 'AVAILABLE', seat: { row_label: 'A', seat_number: 1, seat_type: 'STANDARD' } }, { id: 12, price: 100000, status: 'BOOKED', seat: { row_label: 'A', seat_number: 2, seat_type: 'STANDARD' } }];
  it('disables unavailable seats and sends selected seat IDs without a client price', async () => {
    request.mockResolvedValueOnce(seats).mockResolvedValueOnce({ id: 99 });
    const user = userEvent.setup(); const booked = vi.fn();
    render(<MovieDialog movie={movie} shows={[show]} session={session} onClose={vi.fn()} onSignIn={vi.fn()} onBooked={booked} />);
    await user.click(screen.getByRole('button', { name: /18:30/ }));
    expect(await screen.findByRole('button', { name: /Seat A2/ })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: /Seat A1/ }));
    await user.click(screen.getByRole('button', { name: /Reserve seats/ }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/bookings/newbook', expect.objectContaining({ body: { userId: 7, showId: 1, showSeatIds: [11] } })));
    expect(booked).toHaveBeenCalledWith({ id: 99 });
  });
  it('requires sign-in before a guest can reserve seats', async () => {
    request.mockResolvedValue(seats); const user = userEvent.setup(); const signIn = vi.fn();
    render(<MovieDialog movie={movie} shows={[show]} session={null} onClose={vi.fn()} onSignIn={signIn} onBooked={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: /18:30/ }));
    await user.click(await screen.findByRole('button', { name: /Seat A1/ }));
    await user.click(screen.getByRole('button', { name: /Sign in to reserve/ }));
    expect(signIn).toHaveBeenCalledOnce(); expect(request).toHaveBeenCalledTimes(1);
  });
  it('does not claim a pending payment is confirmed', async () => {
    request.mockResolvedValue([{ id: 8, amount: 100000, status: 'PENDING' }]);
    render(<PaymentDialog booking={{ id: 99, movie_title: movie.title, total_price: 100000, expires_at: '2099-05-01T18:00:00', seats: ['A1'] }} session={session} onClose={vi.fn()} onChanged={vi.fn()} />);
    expect(await screen.findByText('Awaiting verification')).toBeInTheDocument();
    expect(screen.getByText(/confirmed after the cinema verifies/)).toBeInTheDocument();
    expect(screen.queryByText(/Payment confirmed/)).not.toBeInTheDocument();
  });
});
