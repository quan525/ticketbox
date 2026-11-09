import { useEffect, useRef, useState } from 'react';
import { request } from './api.js';
import { renderGoogleButton } from './googleIdentity.js';

export default function GoogleSignIn({ onAuthenticated }) {
  const container = useRef(null);
  const onSuccess = useRef(onAuthenticated);
  onSuccess.current = onAuthenticated;
  const [state, setState] = useState('loading');
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let dispose;
    let pending = false;
    setState('loading'); setError('');
    async function signIn(credential) {
      if (controller.signal.aborted || pending) return;
      pending = true; setState('signing-in'); setError('');
      try {
        if (!credential) throw new Error('Google did not return a login. Please try again.');
        const { token } = await request('/auth/google', { method: 'POST', body: { credential }, signal: controller.signal });
        const user = await request('/auth/me', { token, signal: controller.signal });
        if (!controller.signal.aborted) onSuccess.current({ token, user });
      } catch (failure) { if (!controller.signal.aborted) setError(failure.message); }
      finally { pending = false; if (!controller.signal.aborted) setState('ready'); }
    }
    request('/auth/google/config', { signal: controller.signal }).then(async config => {
      if (controller.signal.aborted) return;
      if (!config.enabled || !config.client_id) { setState('unavailable'); return; }
      dispose = await renderGoogleButton(container.current, config.client_id, signIn, controller.signal);
      if (controller.signal.aborted) dispose?.(); else setState('ready');
    }).catch(failure => { if (!controller.signal.aborted) { setError(failure.message); setState('failed'); } });
    return () => { controller.abort(); dispose?.(); };
  }, [attempt]);
  return <div className="google-auth">
    <div ref={container} className={`google-button ${state === 'signing-in' ? 'google-busy' : ''}`} aria-busy={state === 'signing-in'} />
    {state === 'loading' && <p role="status">Loading Google sign-in…</p>}
    {state === 'signing-in' && <p role="status">Signing in with Google…</p>}
    {state === 'unavailable' && <><button className="secondary-button full-width" disabled>Continue with Google</button><p>Google sign-in is currently unavailable.</p></>}
    {error && <p className="inline-error" role="alert">{error} <button className="text-button" onClick={() => setAttempt(value => value + 1)}>Try again</button></p>}
    <div className="auth-divider"><span>or use your username</span></div>
  </div>;
}
