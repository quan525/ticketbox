let scriptPromise;
let configuredClient;
let receiveCredential;

function loadGoogle() {
  if (window.google?.accounts?.id) return Promise.resolve(window.google.accounts.id);
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      const fail = () => { clearTimeout(timer); script.remove(); scriptPromise = undefined; reject(new Error('Google sign-in could not load. Please try again.')); };
      const timer = setTimeout(fail, 15000);
      script.onerror = fail;
      script.onload = () => {
        clearTimeout(timer);
        if (window.google?.accounts?.id) resolve(window.google.accounts.id);
        else fail();
      };
      document.head.append(script);
    });
  }
  return scriptPromise;
}

export async function renderGoogleButton(element, clientId, callback, signal) {
  const google = await loadGoogle();
  if (signal.aborted) return () => {};
  receiveCredential = callback;
  if (configuredClient !== clientId) {
    google.initialize({ client_id: clientId, auto_select: false, ux_mode: 'popup', callback: response => receiveCredential?.(response.credential) });
    configuredClient = clientId;
  }
  google.renderButton(element, { theme: 'outline', size: 'large', text: 'continue_with', shape: 'rectangular', width: Math.min(360, element.clientWidth || 300) });
  return () => { if (receiveCredential === callback) receiveCredential = undefined; element.replaceChildren(); };
}
