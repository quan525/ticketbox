import { beforeEach, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import GoogleSignIn from '../GoogleSignIn.jsx';
import { request } from '../api.js';
import { renderGoogleButton } from '../googleIdentity.js';
vi.mock('../api.js', () => ({ request: vi.fn() }));
vi.mock('../googleIdentity.js', () => ({ renderGoogleButton: vi.fn() }));
beforeEach(() => { vi.resetAllMocks(); renderGoogleButton.mockResolvedValue(vi.fn()); });
it('does not load Google when the client ID is unconfigured', async () => {
  request.mockResolvedValue({ enabled: false, client_id: '' });
  render(<GoogleSignIn onAuthenticated={vi.fn()} />);
  expect(await screen.findByRole('button', { name: 'Continue with Google' })).toBeDisabled();
  expect(renderGoogleButton).not.toHaveBeenCalled();
});
it('exchanges the Google credential for a Ticketbox session', async () => {
  request.mockResolvedValueOnce({ enabled: true, client_id: 'web-client' }).mockResolvedValueOnce({ token: 'app-token' }).mockResolvedValueOnce({ id: 7, username: 'google-user' });
  const onAuthenticated = vi.fn(); render(<GoogleSignIn onAuthenticated={onAuthenticated} />);
  await waitFor(() => expect(renderGoogleButton).toHaveBeenCalled());
  await act(async () => { await renderGoogleButton.mock.calls[0][2]('google-credential'); });
  expect(request).toHaveBeenCalledWith('/auth/google', expect.objectContaining({ method: 'POST', body: { credential: 'google-credential' } }));
  expect(onAuthenticated).toHaveBeenCalledWith({ token: 'app-token', user: { id: 7, username: 'google-user' } });
});
it('shows a verification error without signing in', async () => {
  request.mockResolvedValueOnce({ enabled: true, client_id: 'web-client' }).mockRejectedValueOnce(new Error('Unable to verify Google login'));
  const onAuthenticated = vi.fn(); render(<GoogleSignIn onAuthenticated={onAuthenticated} />);
  await waitFor(() => expect(renderGoogleButton).toHaveBeenCalled());
  await act(async () => { await renderGoogleButton.mock.calls[0][2]('bad-credential'); });
  expect(await screen.findByRole('alert')).toHaveTextContent('Unable to verify');
  expect(onAuthenticated).not.toHaveBeenCalled();
});
