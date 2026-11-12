import { useEffect, useRef, useState } from 'react';
import { request } from './api.js';
import { money } from './model.js';
import MockQrCheckout from './MockQrCheckout.jsx';

export default function PaymentCheckout({ payment, token, expired, onSettled }) {
  const settled = useRef(onSettled);
  useEffect(() => { settled.current = onSettled; }, [onSettled]);
  const [config, setConfig] = useState(null);
  const [checkout, setCheckout] = useState(null);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const [pollError, setPollError] = useState('');
  const [imageError, setImageError] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    setConfig(null); setCheckout(null); setError(''); setImageError(false);
    async function load() {
      try {
        const next = await request('/payments/config', { token, signal: controller.signal });
        if (controller.signal.aborted) return;
        setConfig(next);
        if (!next.enabled) { setError('QR payments have not been configured yet. Please contact the cinema.'); return; }
        if (next.provider.startsWith('SEPAY')) {
          const details = await request(`/payments/${payment.id}/sepay-checkout`, { token, signal: controller.signal });
          if (!controller.signal.aborted) setCheckout(details);
        }
      } catch (failure) { if (!controller.signal.aborted) setError(failure.message); }
    }
    load(); return () => controller.abort();
  }, [payment.id, token, attempt]);
  // The browser only reads status. Confirmation comes from the authenticated server webhook.
  useEffect(() => {
    if (!checkout || expired) return;
    const controller = new AbortController(); let timer; let complete = false;
    async function poll() {
      try {
        const updated = await request(`/payments/${payment.id}`, { token, signal: controller.signal });
        if (controller.signal.aborted) return;
        setPollError('');
        if (updated.status !== 'PENDING') { complete = true; settled.current(updated); }
      } catch (failure) { if (!controller.signal.aborted) setPollError('Unable to refresh payment status. Reconnecting…'); }
      if (!controller.signal.aborted && !complete) timer = setTimeout(poll, 3000);
    }
    timer = setTimeout(poll, 3000);
    return () => { controller.abort(); clearTimeout(timer); };
  }, [checkout, expired, payment.id, token]);
  if (config?.enabled && config.provider === 'LOCAL_MOCK') {
    if (payment.checkout_provider?.startsWith('SEPAY')) return <p className="inline-error" role="alert">This SePay payment cannot use local demo confirmation. Contact the cinema.</p>;
    return <MockQrCheckout payment={payment} token={token} expired={expired} onSettled={onSettled} />;
  }
  return <section className="mock-qr-checkout" aria-label="SePay QR checkout">
    {config && <span className="status status-pending">SePay · {config.demo ? 'Test mode' : 'Bank transfer'}</span>}
    <h3>{config?.demo ? 'SePay test checkout' : 'Pay by QR transfer'}</h3>
    {!checkout && !error && <p role="status">Preparing your payment QR…</p>}
    {checkout && <>
      {checkout.demo ? <p>Do not transfer real money. In SePay Test mode, simulate an incoming transaction using the exact amount and reference below.</p> : <p>Scan with your banking app and transfer the exact amount. Keep the reference unchanged. Your reservation confirms automatically after SePay reports the transfer.</p>}
      {!expired && !checkout.demo && checkout.qr_image_url && <figure className="vietqr-card"><img className="vietqr-code-image" src={checkout.qr_image_url} width="384" height="384" alt={`Payment QR for ${checkout.reference}`} referrerPolicy="no-referrer" onError={() => setImageError(true)} /><figcaption>{checkout.demo ? 'SEPAY TEST MODE · DO NOT TRANSFER MONEY' : 'SEPAY · QR BANK TRANSFER'}</figcaption></figure>}
      <dl><div><dt>Amount</dt><dd>{money(checkout.amount)}</dd></div><div><dt>Transfer reference</dt><dd>{checkout.reference}</dd></div>{!checkout.demo && <><div><dt>Bank</dt><dd>{checkout.bank}</dd></div><div><dt>Account</dt><dd>{checkout.account_no}</dd></div><div><dt>Recipient</dt><dd>{checkout.account_name}</dd></div></>}</dl>
      <p role="status">{expired ? 'Reservation expired. Do not transfer. Contact the cinema if you already sent money.' : 'Waiting for SePay confirmation…'}</p>
      <p className="muted small">Late transfers and incorrect amounts need cinema review; they do not automatically reserve expired seats.</p>
    </>}
    {imageError && <p className="inline-error" role="alert">The QR image could not load. Retry checkout or use the exact transfer details above.</p>}
    {pollError && <p role="status">{pollError}</p>}
    {(error || imageError) && <p role="alert" className="inline-error">{error} <button className="text-button" onClick={() => setAttempt(value => value + 1)}>Retry checkout</button></p>}
  </section>;
}
