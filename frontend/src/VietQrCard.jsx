export default function VietQrCard({ image, reference, sample = false, paymentDetailsIncluded = false, onImageError }) {
  if (paymentDetailsIncluded) return <figure className="vietqr-card vietqr-card--sample" aria-label="SePay reservation QR">
    <img className="vietqr-sample-image" src={image} alt={`SePay QR with reservation amount and reference ${reference}`} referrerPolicy="no-referrer" onError={onImageError} />
    <figcaption>RESERVATION AMOUNT & REFERENCE INCLUDED</figcaption>
  </figure>;
  if (sample) return <figure className="vietqr-card vietqr-card--sample" aria-label="Uploaded SePay QR preview">
    <img className="vietqr-sample-image" src={image} alt={`Uploaded SePay QR preview for ${reference}`} referrerPolicy="no-referrer" onError={onImageError} />
    <figcaption>LOCAL DEMO · NO BANK CONFIRMATION</figcaption>
    <p className="vietqr-sample-caption">Your uploaded SePay QR image is fixed.<br />The demo amount and reference below are not inserted into this image.</p>
  </figure>;
  return <figure className="vietqr-card" aria-label="VietQR test payment card">
    <div className="vietqr-wordmark" aria-label="VietQR"><img src="./vietqr-mark.svg" alt="" aria-hidden="true" /><span className="vietqr-wordmark-viet">iet</span><span className="vietqr-wordmark-qr">QR</span></div>
    <div className="vietqr-code-frame">
      <img className="vietqr-code-image" src={image} width="384" height="384" alt={`VietQR test code for ${reference}`} />
      <span className="vietqr-centre-mark" aria-hidden="true"><img src="./vietqr-mark.svg" alt="" /></span>
    </div>
    <div className="vietqr-card-footer" aria-hidden="true"><span className="napas-wordmark">napas<span>2<span>4</span><i>7</i></span></span><span className="vietqr-footer-divider" /><span className="vietqr-test-bank"><svg viewBox="0 0 28 28" fill="none"><path d="m3 9 11-6 11 6H3Zm3 3v9m8-9v9m8-9v9M3 25h22" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" /></svg><span>TEST BANK<small>DEMO RECIPIENT</small></span></span></div>
    <figcaption>TEST DATA · NO REAL PAYMENT</figcaption>
  </figure>;
}
