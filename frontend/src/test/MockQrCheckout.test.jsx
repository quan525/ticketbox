import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, expect, it, vi } from 'vitest';
import MockQrCheckout from '../MockQrCheckout.jsx';
import { request } from '../api.js';
import QRCode from 'qrcode';
vi.mock('../api.js', () => ({ request: vi.fn() }));
vi.mock('qrcode', () => ({ default: { toDataURL: vi.fn() } }));
const checkout = { reference: 'MOCK-TICKETBOX-9', amount: 200000, qr_payload: 'ticketbox://mock-payment?payment_id=9' };
beforeEach(() => {
  request.mockReset(); QRCode.toDataURL.mockResolvedValue('data:image/png;base64,demo');
  request.mockImplementation(path => Promise.resolve(path.endsWith('/config') ? { enabled: true } : checkout));
});
it('renders an encoded demo QR with VND amount and clear no-transfer notice', async () => {
  render(<MockQrCheckout payment={{ id: 9 }} token="jwt" expired={false} onSettled={vi.fn()} />);
  expect(await screen.findByRole('img')).toHaveAttribute('src', 'data:image/png;base64,demo');
  expect(QRCode.toDataURL).toHaveBeenCalledWith(checkout.qr_payload, expect.any(Object));
  expect(screen.getByText(/invalid test bank and account/)).toBeInTheDocument();
  expect(screen.getByText(checkout.reference)).toBeInTheDocument();
});
it('posts a success simulation and uses the server response', async () => {
  const result = { id: 9, status: 'PAID' }; const settled = vi.fn(); const user = userEvent.setup();
  render(<MockQrCheckout payment={{ id: 9 }} token="jwt" expired={false} onSettled={settled} />);
  await screen.findByRole('img');
  request.mockResolvedValue(result);
  await user.click(screen.getByRole('button', { name: 'Simulate successful payment' }));
  expect(request).toHaveBeenCalledWith('/payments/9/mock-confirm', expect.objectContaining({ body: { outcome: 'SUCCESS' }, token: 'jwt' }));
  expect(settled).toHaveBeenCalledWith(result);
});
it('supports declined simulations', async () => {
  const user = userEvent.setup(); render(<MockQrCheckout payment={{ id: 9 }} token="jwt" expired={false} onSettled={vi.fn()} />);
  await screen.findByRole('img');
  await user.click(screen.getByRole('button', { name: 'Simulate declined payment' }));
  expect(request).toHaveBeenCalledWith('/payments/9/mock-confirm', expect.objectContaining({ body: { outcome: 'DECLINED' } }));
});
it('disables simulation when the reservation expires', async () => {
  render(<MockQrCheckout payment={{ id: 9 }} token="jwt" expired onSettled={vi.fn()} />);
  expect(await screen.findByRole('button', { name: 'Simulate successful payment' })).toBeDisabled();
});
it('explains unavailable QR checkout instead of silently hiding it', async () => {
  request.mockResolvedValue({ enabled: false });
  const { container } = render(<MockQrCheckout payment={{ id: 9 }} token="jwt" expired={false} onSettled={vi.fn()} />);
  await waitFor(() => expect(request).toHaveBeenCalled());
  expect(await screen.findByRole('alert')).toHaveTextContent('QR checkout is currently unavailable');
  expect(QRCode.toDataURL).not.toHaveBeenCalled();
});

it('shows loading feedback while fetching QR configuration', async () => {
  request.mockReturnValue(new Promise(() => {}));
  render(<MockQrCheckout payment={{ id: 9 }} token="jwt" expired={false} onSettled={vi.fn()} />);
  expect(screen.getByRole('status')).toHaveTextContent('Preparing your VietQR test code');
});

it('uses the supplied VietQR sample image and distinguishes it from the simulated reservation', async () => {
  const qr_image_url = 'https://img.vietqr.io/image/ICB-113366668888-compact.svg';
  request.mockImplementation(path => Promise.resolve(path.endsWith('/config') ? { enabled: true } : { ...checkout, qr_image_url }));
  render(<MockQrCheckout payment={{ id: 9 }} token="jwt" expired={false} onSettled={vi.fn()} />);
  expect(await screen.findByRole('img', { name: /VietQR sample preview/ })).toHaveAttribute('src', qr_image_url);
  expect(screen.getByText(/does not encode your reservation amount/)).toBeInTheDocument();
  expect(screen.getByText('Simulation amount')).toBeInTheDocument();
  expect(screen.queryByText('Test bank BIN')).not.toBeInTheDocument();
  expect(QRCode.toDataURL).not.toHaveBeenCalled();
});
