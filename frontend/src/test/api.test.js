import { expect, it, vi } from 'vitest';
import { request } from '../api.js';
it('attaches the bearer token and serializes JSON', async () => {
  const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"id":1}', { status: 201 }));
  expect(await request('/payments', { token: 'abc', method: 'POST', body: { booking_id: 1 } })).toEqual({ id: 1 });
  expect(fetchMock).toHaveBeenCalledWith('/api/payments', expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer abc' }), body: '{"booking_id":1}' }));
});
it('preserves the API status and useful validation message', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"message":"Seats unavailable"}', { status: 409 }));
  await expect(request('/bookings')).rejects.toMatchObject({ status: 409, message: 'Seats unavailable' });
});
