import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { request } from './api.js';
import { dayLabel, money, time } from './model.js';

export default function SepayEvents({ token }) {
  const [events, setEvents] = useState([]); const [error, setError] = useState(''); const [attempt, setAttempt] = useState(0);
  const [deliveries, setDeliveries] = useState([]);
  const [preview, setPreview] = useState('');
  const [previewError, setPreviewError] = useState(false);
  useEffect(() => {
    let active = true;
    QRCode.toDataURL('Ticketbox demo QR', { width: 240 })
      .then(image => { if (active) setPreview(image); })
      .catch(() => { if (active) setPreviewError(true); });
    return () => { active = false; };
  }, []);
  const [deliveryError, setDeliveryError] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      try { const next = await request('/payments/sepay/events', { token, signal: controller.signal }); if (!controller.signal.aborted) { setEvents(next); setError(''); } }
      catch (failure) { if (!controller.signal.aborted) setError(failure.message); }
    }
    async function loadDeliveries() {
      try {
        const next = await request('/payments/sepay/deliveries', { token, signal: controller.signal });
        if (!controller.signal.aborted) { setDeliveries(next); setDeliveryError(''); }
      } catch (failure) {
        if (!controller.signal.aborted) setDeliveryError(failure.message);
      }
    }
    function refresh() { load(); loadDeliveries(); }
    refresh();
    const timer = setInterval(refresh, 30000);
    return () => { controller.abort(); clearInterval(timer); };
  }, [token, attempt]);
  const review = events.filter(event => !['CONFIRMED', 'IGNORED_OUTGOING'].includes(event.outcome)).length;
  return <><section className="admin-panel admin-recent">
    <details>
      <summary className="admin-text-action">Test QR preview</summary>
      <p className="admin-form-hint">This QR encodes plain text for scanning practice.</p>
      {preview && <img src={preview} alt="Plain-text Ticketbox demo QR" width="240" />}
      {previewError && <p role="alert">Unable to generate the preview.</p>}
    </details>
  </section><section className="admin-panel admin-recent">
    <div className="admin-panel-heading">
      <div><h2>Webhook deliveries</h2><p className="admin-form-hint">Latest 100 SePay requests since the server started. Use the transaction activity below to check whether a payment was confirmed.</p></div>
      <button className="admin-text-action" onClick={() => setAttempt(value => value + 1)}>Refresh</button>
    </div>
    {deliveryError ? <p className="inline-error" role="alert">{deliveryError}</p> :
      <div className="admin-table-scroll"><table className="admin-table">
        <thead><tr><th>Received</th><th>Finished</th><th>HTTP status</th><th>Result</th><th>Webhook time</th><th>Payment processing</th></tr></thead>
        <tbody>{deliveries.length ? deliveries.map(delivery => <tr key={delivery.id}>
          <td>{new Date(delivery.received_at).toLocaleString()}</td>
          <td>{new Date(delivery.finished_at).toLocaleString()}</td>
          <td>{delivery.status}</td><td>{delivery.result}</td><td>{delivery.duration_ms} ms</td>
          <td>{delivery.payment_process_ms == null ? '—' : `${delivery.payment_process_ms} ms`}</td>
        </tr>) : <tr><td colSpan={6}>No deliveries received since startup.</td></tr>}</tbody>
      </table></div>}
  </section><section className="admin-panel admin-recent"><div className="admin-panel-heading"><div><span className="admin-kicker">SEPAY TRANSFERS</span><h2>Webhook activity</h2><p className="admin-form-hint">Latest 100 events · {review} requiring review. Late or mismatched transfers need reconciliation or a refund; they do not confirm seats.</p></div><button className="admin-text-action" onClick={() => setAttempt(value => value + 1)}>Refresh events</button></div>
    {error ? <p className="inline-error" role="alert">{error}</p> : <div className="admin-table-scroll"><table className="admin-table"><thead><tr>{['Received', 'SePay transaction', 'Payment', 'Reference', 'Amount', 'Result'].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{events.length ? events.map(event => <tr key={event.id}><td>{dayLabel(event.created_at)}<small>{time(event.created_at)}</small></td><td>#{event.transaction_id}<small>{event.mode === 'TEST' ? 'Test mode · no real charge' : 'Live'}</small></td><td>{event.payment_id ? `#${event.payment_id}` : 'Unmatched'}</td><td className="admin-reference">{event.reference || 'Not found'}</td><td>{money(event.amount)}</td><td><span className={`admin-status ${event.outcome === 'CONFIRMED' ? 'admin-status-confirmed' : 'admin-status-pending'}`}>{event.outcome.toLowerCase().replaceAll('_', ' ')}</span></td></tr>) : <tr><td colSpan={6}><div className="admin-table-empty">No SePay webhooks received yet.</div></td></tr>}</tbody></table></div>}
  </section></>;
}
