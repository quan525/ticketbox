export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

let sessionGeneration = 0;
let pendingRefresh;

function sessionLock(operation) {
  return globalThis.navigator?.locks
    ? navigator.locks.request('ticketbox-auth-cookie', operation)
    : operation();
}

function refreshAccess(session, generation) {
  if (pendingRefresh?.generation === generation) return pendingRefresh.promise;
  const promise = sessionLock(async () => {
    if (generation !== sessionGeneration) throw new ApiError('Your session changed.', 401);
    const current = readSession();
    if (current?.token && current.token !== session.token) return current.token;
    const { token } = await send('/auth/refresh', {
      method: 'POST', body: { username: session.user.username },
    });
    if (generation !== sessionGeneration) throw new ApiError('Your session changed.', 401);
    const next = { ...readSession(), token };
    sessionStorage.setItem('ticketbox-session', JSON.stringify(next));
    window.dispatchEvent(new CustomEvent('ticketbox-session-refreshed', { detail: next }));
    return token;
  }).finally(() => {
    if (pendingRefresh?.promise === promise) pendingRefresh = undefined;
  });
  pendingRefresh = { generation, promise };
  return promise;
}

export async function request(path, options = {}) {
  const session = options.token ? readSession() : null;
  const generation = sessionGeneration;
  try {
    return await send(path, options);
  } catch (failure) {
    if (failure.status !== 401 || !session?.user?.username || session.token !== options.token
        || generation !== sessionGeneration || options.signal?.aborted) throw failure;
    const token = await refreshAccess(session, generation);
    options.signal?.throwIfAborted();
    if (generation !== sessionGeneration) throw new ApiError('Your session changed.', 401);
    return send(path, { ...options, token });
  }
}

export function revokeSession(session) {
  if (!session?.user?.username) return Promise.resolve();
  return sessionLock(() => send('/auth/logout', {
    method: 'POST', body: { username: session.user.username },
  }));
}

async function send(path, { token, body, method = 'GET', signal } = {}) {
  const response = await fetch(`/api${path}`, {
    method,
    signal,
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  });
  const raw = await response.text();
  let data;
  try { data = raw ? JSON.parse(raw) : null; } catch { data = raw; }
  if (!response.ok) {
    const message = typeof data === 'string' ? data : data?.message ||
      (data && typeof data === 'object' ? Object.values(data).filter(value => typeof value === 'string').join('. ') : '');
    throw new ApiError(message || (response.status === 401 ? 'Please sign in to continue.' : 'Something went wrong. Please try again.'), response.status);
  }
  return data;
}

export function readSession() {
  try { return JSON.parse(sessionStorage.getItem('ticketbox-session')) || null; }
  catch { return null; }
}

export function saveSession(session) {
  sessionGeneration++;
  if (session) sessionStorage.setItem('ticketbox-session', JSON.stringify(session));
  else sessionStorage.removeItem('ticketbox-session');
}

export function updateSessionUser(user) {
  const current = readSession();
  if (!current || current.user?.id !== user.id) return null;
  const next = { ...current, user };
  sessionStorage.setItem('ticketbox-session', JSON.stringify(next));
  return next;
}
