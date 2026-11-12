import { useEffect, useRef, useState } from 'react';
import QRCode from 'qrcode';
import { request } from './api.js';
import { money } from './model.js';

export default function MockQrCheckout({ payment, token, expired, onSettled }) {
  const [checkout, setCheckout] = useState(null);
  const [image, setImage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const inFlight = useRef(false);
  useEffect(() => {
    const controller = new AbortController();
    setCheckout(null); setImage(''); setError(''); setLoading(true);
    async function load() {
      try {
        const config = await request('/payments/mock/config', { token, signal: controller.signal });
        if (controller.signal.aborted) return;
        if (!config.enabled) { setError('QR checkout is currently unavailable. Please try again shortly.'); return; }
        const data = await request(`/payments/${payment.id}/mock-checkout`, { token, signal: controller.signal });
        const qr = await QRCode.toDataURL("Ticketbox demo " + data.reference, { width: 384, margin: 4, errorCorrectionLevel: 'H' });
        if (!controller.signal.aborted) { setCheckout(data); setImage(qr); }
      } catch (failure) { if (!controller.signal.aborted) setError(failure.message); }
      finally { if (!controller.signal.aborted) setLoading(false); }
    }
    load(); return () => controller.abort();
  }, [payment.id, token, attempt]);
  async function simulate(outcome) {
    if (inFlight.current || expired) return;
    inFlight.current = true; setBusy(true); setError('');
    try {
      const result = await request(`/payments/${payment.id}/mock-confirm`, { token, method: 'POST', body: { outcome } });
      onSettled(result);
    } catch (failure) { setError(failure.message); }
    finally { inFlight.current = false; setBusy(false); }
  }
  return <section className="mock-qr-checkout" aria-label="Demo checkout">
    <h3>Demo checkout</h3>
    <p>The QR contains plain text. Use the buttons to simulate the payment result.</p>
    {image && !expired && <img src={image} width="240" alt="Plain-text demo QR" />}
    {loading && <p role="status">Loading demo checkout…</p>}
    {checkout && <dl>
      <div><dt>Simulation amount</dt><dd>{money(checkout.amount)}</dd></div>
      <div><dt>Demo reference</dt><dd>{checkout.reference}</dd></div>
    </dl>}
    <div className="mock-qr-actions"><button className="primary-button" disabled={busy || expired || !checkout} onClick={() => simulate('SUCCESS')}>{busy ? 'Processing…' : 'Simulate successful payment'}</button><button className="secondary-button" disabled={busy || expired || !checkout} onClick={() => simulate('DECLINED')}>Simulate declined payment</button></div>
    {error && <p role="alert" className="inline-error">{error} <button className="text-button" disabled={busy} onClick={() => setAttempt(value => value + 1)}>Retry demo checkout</button></p>}
  </section>;
}
