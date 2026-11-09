export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function request(path, { token, body, method = 'GET', signal } = {}) {
  const response = await fetch(`/api${path}`, {
    method,
    signal,
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
  if (session) sessionStorage.setItem('ticketbox-session', JSON.stringify(session));
  else sessionStorage.removeItem('ticketbox-session');
}
