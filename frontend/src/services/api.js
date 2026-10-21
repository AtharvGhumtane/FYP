// Centralized API Client

export const DEFAULT_BASE_URL = 'http://localhost:8080';

export async function executeApiCall({
  baseUrl = DEFAULT_BASE_URL,
  method = 'GET',
  path = '',
  headers = {},
  body = null,
  token = null,
}) {
  const cleanBase = baseUrl.replace(/\/+$/, '');
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  const url = `${cleanBase}${cleanPath}`;

  const requestHeaders = {
    'Accept': 'application/json',
    ...headers,
  };

  if (token) {
    requestHeaders['Authorization'] = `Bearer ${token}`;
  }

  const options = {
    method: method.toUpperCase(),
    headers: requestHeaders,
  };

  if (body && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(options.method)) {
    if (typeof body === 'object') {
      requestHeaders['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    } else {
      options.body = body;
    }
  }

  const startTime = performance.now();
  let status = 0;
  let statusText = '';
  let responseData = null;
  let responseHeaders = {};
  let error = null;

  try {
    const res = await fetch(url, options);
    status = res.status;
    statusText = res.statusText;

    res.headers.forEach((value, key) => {
      responseHeaders[key] = value;
    });

    const text = await res.text();
    try {
      responseData = text ? JSON.parse(text) : null;
    } catch {
      responseData = text;
    }
  } catch (err) {
    error = err.message || 'Network request failed';
  }

  const endTime = performance.now();
  const durationMs = Math.round(endTime - startTime);

  return {
    url,
    method: options.method,
    status,
    statusText,
    durationMs,
    headers: responseHeaders,
    data: responseData,
    error,
    ok: status >= 200 && status < 300,
  };
}
