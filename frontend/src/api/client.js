const API_URL = import.meta.env?.VITE_API_URL || 'http://localhost:8080/api'

let authorization = ''

function encodeBasicCredentials(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  let binary = ''
  bytes.forEach(byte => { binary += String.fromCharCode(byte) })
  return `Basic ${btoa(binary)}`
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(authorization ? { Authorization: authorization } : {}),
        ...options.headers,
      },
    })
  } catch {
    throw new ApiError('Cannot reach the laundry server. Start the Spring Boot backend and try again.', 0)
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    const fallback = response.status === 401
      ? 'Incorrect username or password.'
      : `Request failed with status ${response.status}`
    throw new ApiError(error.message || fallback, response.status)
  }

  if (response.status === 204) return null
  return response.json()
}

function orderStatusQuery(statuses) {
  const search = new URLSearchParams()
  statuses.forEach(status => search.append('status', status))
  return search.toString()
}

function newRequestId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export const laundryApi = {
  apiUrl: API_URL,
  setCredentials(username, password) {
    authorization = encodeBasicCredentials(username, password)
  },
  clearCredentials() {
    authorization = ''
  },
  newRequestId,
  health: () => request('/health'),
  verifyLogin: () => request('/dashboard/summary'),
  dashboard: () => request('/dashboard/summary'),
  customers: query => request(`/customers?q=${encodeURIComponent(query || '')}`),
  createCustomer: customer => request('/customers', { method: 'POST', body: JSON.stringify(customer) }),
  services: () => request('/catalog/services'),
  orders: statuses => request(`/orders?${orderStatusQuery(statuses)}`),
  openOrders: customerId => request(`/orders/customer/${customerId}/open`),
  createOrder: (order, clientRequestId = newRequestId()) => request('/orders', {
    method: 'POST',
    headers: { 'Idempotency-Key': clientRequestId },
    body: JSON.stringify({ ...order, clientRequestId }),
  }),
  finaliseOrder: orderNumber => request(`/orders/${orderNumber}/finalize`, { method: 'POST' }),
  paymentSummary: orderNumber => request(`/payments/order/${encodeURIComponent(orderNumber)}`),
  recordPayment: (payment, clientRequestId = newRequestId()) => request('/payments', {
    method: 'POST',
    headers: { 'Idempotency-Key': clientRequestId },
    body: JSON.stringify({ ...payment, clientRequestId }),
  }),
  invoiceDocument: orderNumber => request(`/documents/orders/${encodeURIComponent(orderNumber)}`),
  markTagsPrinted: orderNumber => request(`/documents/orders/${encodeURIComponent(orderNumber)}/tags/printed`, { method: 'POST' }),
  queueWhatsappInvoice: orderNumber => request(`/notifications/whatsapp/invoice/${encodeURIComponent(orderNumber)}/queue`, { method: 'POST' }),
  updateStatus: (orderNumber, status) => request(`/orders/${orderNumber}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  }),
  salesReport: (from, to) => request(`/reports/sales?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`),
}
