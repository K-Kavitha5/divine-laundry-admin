import { useCallback, useEffect, useMemo, useState } from 'react'
import { customers as mockCustomers, metrics as mockMetrics, orders as mockOrders, services as mockServices } from './data/mockData.js'
import { laundryApi } from './api/client.js'
import { downloadInvoiceImage, printGarmentTags, printInvoice } from './printDocuments.js'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })
const demoMode = String(import.meta.env?.VITE_USE_MOCKS || '').toLowerCase() === 'true'
const openStatuses = ['DRAFT', 'RECEIVED', 'WASHING', 'IRONING', 'CLEANED', 'READY', 'REWORK']

const catalogStyle = {
  'Laundry by KG': ['◉', 'mint'], Ironing: ['♙', 'rose'],
  'Dry Clean': ['♜', 'sand'], 'Shoe Cleaning': ['◒', 'sky'], 'Sofa Cleaning': ['▰', 'violet'],
}

function normalizeService(service) {
  const [icon, tint] = catalogStyle[service.category] || ['◇', 'mint']
  return { ...service, rate: Number(service.rate), icon, tint }
}

function normalizeCustomer(customer) {
  return { ...customer, openOrders: Number(customer.openOrders || 0), due: Number(customer.due || 0) }
}

function deliveryLabel(deliveryAt) {
  if (!deliveryAt) return 'Not scheduled'
  const date = new Date(deliveryAt)
  const dateText = date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', timeZone: 'Asia/Kolkata' })
  const timeText = date.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit', timeZone: 'Asia/Kolkata' })
  return `${date.getTime() < Date.now() ? 'Overdue' : dateText} · ${timeText}`
}

function normalizeOrder(order) {
  const pieces = (order.items || []).reduce((sum, item) => sum + Number(item.pieces || 0), 0)
  const first = order.items?.[0]
  const service = first
    ? `${first.serviceName}${order.items.length > 1 ? ` +${order.items.length - 1}` : ''} · ${pieces} pcs`
    : 'No service items'
  return {
    id: order.orderNumber,
    customer: order.customerName,
    service,
    delivery: deliveryLabel(order.deliveryAt),
    total: Number(order.total),
    workStatus: order.status,
    payment: order.paymentStatus,
    staff: 'Admin',
  }
}

function dashboardMetrics(summary) {
  return [
    { label: "Today's orders", value: String(summary.todayOrders || 0), change: 'Live', tone: 'green', icon: '＋' },
    { label: 'In process', value: String(summary.inProcess || 0), change: 'Active orders', tone: 'blue', icon: '◌' },
    { label: 'Ready', value: String(summary.ready || 0), change: 'For delivery', tone: 'violet', icon: '✓' },
    { label: 'Overdue', value: String(summary.overdue || 0), change: 'Needs attention', tone: 'red', icon: '!' },
    { label: 'Unpaid', value: money.format(Number(summary.unpaidAmount || 0)), change: `${summary.unpaidBills || 0} bills`, tone: 'amber', icon: '₹' },
    { label: "Today's sales", value: money.format(Number(summary.todaySales || 0)), change: 'Live total', tone: 'dark', icon: '↗' },
  ]
}

const navItems = [
  ['dashboard', 'Dashboard', '⌂'],
  ['new-order', 'New order', '＋'],
  ['in-process', 'In process', '◌'],
  ['ready', 'Ready', '✓'],
  ['customers', 'Customers', '♙'],
  ['pickups', 'Pickup & delivery', '▱'],
  ['payments', 'Payments', '₹'],
  ['reports', 'Reports', '↗'],
]

function Login({ onLogin, apiUrl }) {
  const [username, setUsername] = useState(demoMode ? 'admin' : '')
  const [password, setPassword] = useState(demoMode ? 'ChangeMe123!' : '')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const submit = async event => {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      await onLogin({ username: username.trim(), password })
    } catch (loginError) {
      setError(loginError.message)
    } finally {
      setSubmitting(false)
    }
  }
  return (
    <main className="login-page">
      <section className="login-story">
        <div className="brand brand-light"><span className="brand-mark">D</span><span>Divine Laundry</span></div>
        <div className="story-copy">
          <span className="eyebrow">ADMIN OPERATIONS</span>
          <h1>Every garment.<br />Tracked with care.</h1>
          <p>A faster, safer workspace for orders, billing, garment tags, pickups and customer communication.</p>
          <div className="story-stats">
            <div><strong>1</strong><span>order</span></div><i></i><div><strong>1</strong><span>invoice</span></div><i></i><div><strong>0</strong><span>duplicates</span></div>
          </div>
        </div>
        <small>Private system · Authorised staff only</small>
      </section>
      <section className="login-panel">
        <form className="login-card" onSubmit={submit}>
          <div className="mobile-brand brand"><span className="brand-mark">D</span><span>Divine Laundry</span></div>
          <span className="eyebrow green">WELCOME BACK</span>
          <h2>Sign in to your workspace</h2>
          <p>Use your owner or staff account.</p>
          <label>Username<input required value={username} onChange={event => setUsername(event.target.value)} autoComplete="username" /></label>
          <label>Password<input required type="password" value={password} onChange={event => setPassword(event.target.value)} autoComplete="current-password" /></label>
          <div className="login-options"><span>Credentials stay only in this browser tab.</span><button type="button" className="text-button">Need help?</button></div>
          {error && <div className="form-error" role="alert">! {error}</div>}
          <button className="primary wide" type="submit" disabled={submitting}>{submitting ? 'Connecting…' : 'Sign in securely'} <span>→</span></button>
          <div className="demo-note"><span>i</span>{demoMode ? 'Sample-data mode is enabled for UI review.' : `Connecting to ${apiUrl}`}</div>
        </form>
      </section>
    </main>
  )
}

function Sidebar({ page, setPage, readyCount = 0 }) {
  return (
    <aside className="sidebar">
      <div className="brand"><span className="brand-mark">D</span><span><b>Divine</b><small>Laundry Admin</small></span></div>
      <nav>
        <span className="nav-label">WORKSPACE</span>
        {navItems.map(([id, label, icon]) => (
          <button key={id} className={page === id ? 'active' : ''} onClick={() => setPage(id)}>
            <span className="nav-icon">{icon}</span><span>{label}</span>{id === 'ready' && <em>{readyCount}</em>}
          </button>
        ))}
        <span className="nav-label space">MANAGE</span>
        <button><span className="nav-icon">□</span><span>Services & prices</span></button>
        <button><span className="nav-icon">⚙</span><span>Settings</span></button>
      </nav>
      <div className="sidebar-help"><span>?</span><div><b>Need help?</b><small>Setup & support</small></div><button>→</button></div>
    </aside>
  )
}

function Topbar({ title, onLogout, setPage }) {
  const [quickOpen, setQuickOpen] = useState(false)
  const quickActions = [
    ['Schedule pickup', 'pickups'], ['Create customer', 'customers'], ['Create staff'],
    ['Create package'], ['Create service'], ['Create batch invoice'], ['Give refund'], ['Recurring pickup', 'pickups'],
  ]
  return (
    <header className="topbar">
      <div><span className="crumb">Divine Laundry /</span><strong>{title}</strong></div>
      <div className="top-actions">
        <label className="global-search"><span>⌕</span><input placeholder="Order ID, phone or customer" /></label>
        <div className="quick-wrap"><button className="quick-trigger" onClick={() => setQuickOpen(open => !open)}>＋ Quick actions <span>⌄</span></button>{quickOpen && <div className="quick-menu">{quickActions.map(([label, target]) => <button key={label} onClick={() => { if (target) setPage(target); setQuickOpen(false) }}>{label}<span>→</span></button>)}</div>}</div>
        <button className="round">◔</button><button className="round has-dot">♢</button>
        <button className="profile" onClick={onLogout}><span>DK</span><div><b>Divine Laundry</b><small>Owner</small></div><i>⌄</i></button>
      </div>
    </header>
  )
}

function MetricCard({ metric }) {
  return <article className={`metric ${metric.tone}`}><div className="metric-top"><span>{metric.icon}</span><small>•••</small></div><p>{metric.label}</p><div><strong>{metric.value}</strong><em>{metric.change}</em></div></article>
}

function StatusPill({ children }) {
  return <span className={`status ${String(children).toLowerCase().replaceAll(' ', '-')}`}>{children}</span>
}

function OrderTable({ rows = [], compact = false }) {
  return (
    <div className="table-wrap">
      <table className={compact ? 'compact' : ''}>
        <thead><tr><th>Order</th><th>Customer</th><th>Service</th><th>Delivery</th><th>Status</th><th>Payment</th><th>Total</th><th></th></tr></thead>
        <tbody>{rows.map(order => <tr key={order.id}>
          <td><button className="order-link">{order.id}</button><small>{order.staff}</small></td>
          <td><strong>{order.customer}</strong></td><td>{order.service}</td>
          <td className={order.delivery.startsWith('Overdue') ? 'danger-text' : ''}>{order.delivery}</td>
          <td><StatusPill>{order.workStatus}</StatusPill></td><td><StatusPill>{order.payment}</StatusPill></td>
          <td><strong>{money.format(order.total)}</strong></td><td><button className="more">•••</button></td>
        </tr>)}</tbody>
      </table>
    </div>
  )
}

function Dashboard({ setPage, metrics, orders }) {
  const statusCount = status => orders.filter(order => order.workStatus === status).length
  const overdueCount = orders.filter(order => order.delivery.startsWith('Overdue')).length
  const onTimePercent = orders.length ? Math.round((orders.length - overdueCount) / orders.length * 100) : 100
  return (
    <div className="page-content">
      <section className="page-heading"><div><span className="eyebrow green">TUESDAY, 1 SEPTEMBER</span><h1>Good evening, Barani</h1><p>Here is what needs your attention today.</p></div><button className="primary" onClick={() => setPage('new-order')}>＋ Create new order</button></section>
      <section className="metrics-grid">{metrics.map(metric => <MetricCard key={metric.label} metric={metric} />)}</section>
      <section className="dashboard-grid">
        <article className="panel pipeline-panel">
          <div className="panel-head"><div><h3>Order pipeline</h3><p>Current operational workload</p></div><button className="ghost">This week⌄</button></div>
          <div className="pipeline">
            {[['Received', statusCount('RECEIVED'), 24], ['Washing', statusCount('WASHING'), 52], ['Ironing', statusCount('IRONING'), 68], ['Cleaned', statusCount('CLEANED'), 82], ['Ready', statusCount('READY'), 100]].map(([label, count, width]) => <div key={label}><span>{label}<b>{count}</b></span><i><em style={{ width: `${count ? width : 0}%` }}></em></i></div>)}
          </div>
          <div className="pipeline-note"><span>✓</span><div><b>{onTimePercent}% currently on schedule</b><small>{overdueCount} order{overdueCount === 1 ? '' : 's'} need attention</small></div><button onClick={() => setPage('in-process')}>View orders →</button></div>
        </article>
        <article className="panel delivery-panel">
          <div className="panel-head"><div><h3>Upcoming deliveries</h3><p>Next four hours</p></div><button className="icon-button">↗</button></div>
          {orders.slice(0, 4).map((order, index) => <div className="delivery-item" key={order.id}><span className={`time-dot t${index}`}></span><div><b>{order.customer}</b><small>{order.service}</small></div><strong>{order.delivery.split('·')[1] || 'Not set'}</strong></div>)}
        </article>
      </section>
      <section className="panel orders-panel"><div className="panel-head"><div><h3>Recent orders</h3><p>Live status across the outlet</p></div><button className="ghost" onClick={() => setPage('in-process')}>View all orders →</button></div><OrderTable rows={orders} compact /></section>
    </div>
  )
}

function NewOrder({ services, customers, onCreateOrder, checkOpenOrders, createdBy }) {
  const categories = useMemo(() => [...new Set(services.map(service => service.category))], [services])
  const [category, setCategory] = useState(categories[0] || '')
  const [serviceGroup, setServiceGroup] = useState('All')
  const [selectedCustomerId, setSelectedCustomerId] = useState(customers[0]?.id || '')
  const [cart, setCart] = useState([])
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [openOrderCount, setOpenOrderCount] = useState(0)
  const [clientRequestId, setClientRequestId] = useState(() => laundryApi.newRequestId())
  const [deliveryDate, setDeliveryDate] = useState('')
  const [deliveryTime, setDeliveryTime] = useState('19:00')
  const [notes, setNotes] = useState('')
  const [showCustom, setShowCustom] = useState(false)
  const [customItem, setCustomItem] = useState({ name: '', rate: '', quantity: 1, pieces: 1, unit: 'PIECE', hsn: '', noPrint: false, noCount: false })
  const selectedCustomer = customers.find(customer => String(customer.id) === String(selectedCustomerId))

  useEffect(() => {
    if (!category && categories[0]) setCategory(categories[0])
  }, [category, categories])

  useEffect(() => {
    if (!selectedCustomerId && customers[0]) setSelectedCustomerId(customers[0].id)
  }, [customers, selectedCustomerId])

  useEffect(() => {
    let active = true
    if (!selectedCustomerId) return undefined
    checkOpenOrders(selectedCustomerId)
      .then(result => { if (active) setOpenOrderCount(result.length) })
      .catch(() => { if (active) setOpenOrderCount(selectedCustomer?.openOrders || 0) })
    return () => { active = false }
  }, [checkOpenOrders, selectedCustomer?.openOrders, selectedCustomerId])

  const addService = service => setCart(current => {
    const found = current.find(item => item.id === service.id)
    const nextQuantity = Number(service.quantity ?? 1)
    const nextPieces = Number(service.pieces ?? nextQuantity)
    if (found) {
      const incrementedQuantity = Number(found.quantity || 0) + 1
      const incrementedPieces = service.unit === 'PIECE' ? incrementedQuantity : Number(found.pieces || 0) + 1
      return current.map(item => item.id === service.id ? { ...item, quantity: incrementedQuantity, pieces: incrementedPieces } : item)
    }
    return [...current, { ...service, quantity: nextQuantity, pieces: service.unit === 'PIECE' ? nextQuantity : nextPieces }]
  })
  const updateItem = (id, key, value) => setCart(current => current.map(item => {
    if (item.id !== id) return item
    const numericValue = Number(value)
    const safeValue = Number.isFinite(numericValue) ? Math.max(0, numericValue) : 0
    if (item.unit === 'PIECE' && (key === 'quantity' || key === 'pieces')) {
      const nextQuantity = Math.round(safeValue)
      return { ...item, quantity: nextQuantity, pieces: nextQuantity }
    }
    return { ...item, [key]: safeValue }
  }))
  const removeItem = id => setCart(current => current.filter(item => item.id !== id))
  const subtotal = useMemo(() => cart.reduce((sum, item) => sum + item.rate * item.quantity, 0), [cart])
  const rounded = Math.round(subtotal)
  const pieces = cart.reduce((sum, item) => sum + item.pieces, 0)
  const serviceGroups = [...new Set(services.filter(service => service.category === category).map(service => service.group).filter(Boolean))]
  const visibleServices = services.filter(item => item.category === category && (serviceGroup === 'All' || item.group === serviceGroup))
  const addCustomItem = event => {
    event.preventDefault()
    if (!customItem.name.trim() || Number(customItem.rate) <= 0 || Number(customItem.quantity) <= 0) return
    addService({ id: `custom-${Date.now()}`, name: customItem.name.trim(), category: 'Custom', unit: customItem.unit, rate: Number(customItem.rate), icon: '＋', tint: 'violet', quantity: Number(customItem.quantity), pieces: customItem.noCount ? 0 : Number(customItem.pieces), noPrint: customItem.noPrint, hsn: customItem.hsn })
    setCustomItem({ name: '', rate: '', quantity: 1, pieces: 1, unit: 'PIECE', hsn: '', noPrint: false, noCount: false })
    setShowCustom(false)
  }
  const resetBill = () => {
    setCart([])
    setNotes('')
    setNotice('')
    setError('')
    setClientRequestId(laundryApi.newRequestId())
  }
  const saveOrder = async finalise => {
    setNotice('')
    setError('')
    if (!selectedCustomer) return setError('Select a customer before saving the order.')
    if (cart.length === 0) return setError('Add at least one service to the bill.')
    if (cart.some(item => String(item.id).startsWith('custom-'))) {
      return setError('Custom items must first be added to Services & prices before a live order can be saved.')
    }
    setSaving(true)
    try {
      const deliveryAt = deliveryDate
        ? new Date(`${deliveryDate}T${deliveryTime || '19:00'}:00+05:30`).toISOString()
        : null
      const result = await onCreateOrder({
        customerId: Number(selectedCustomer.id), deliveryAt, notes, createdBy,
        discount: 0, tax: 0,
        items: cart.map(item => {
          const numberQuantity = Number(item.quantity || 0)
          const numberPieces = Number(item.pieces || 0)
          const normalizedQuantity = item.unit === 'PIECE' ? Math.round(numberQuantity) : numberQuantity
          const normalizedPieces = item.unit === 'PIECE' ? normalizedQuantity : numberPieces
          return {
            serviceId: Number(item.id), billableQuantity: normalizedQuantity,
            pieceCount: normalizedPieces, noPrint: Boolean(item.noPrint),
          }
        }),
      }, clientRequestId, finalise, true)
      const invoiceText = result.invoiceNumber ? `Invoice ${result.invoiceNumber}` : `Order ${result.orderNumber}`
      const whatsappText = !finalise ? ''
        : result.whatsappStatus === 'SENT' ? ' Invoice and UPI QR were sent automatically on WhatsApp.'
          : result.whatsappStatus === 'WAITING_FOR_PROVIDER' ? ' WhatsApp delivery is queued until the business provider settings are completed.'
            : result.whatsappStatus === 'FAILED' ? ` WhatsApp send failed: ${result.whatsappError || 'check the provider settings.'}`
              : ' WhatsApp delivery was started.'
      setNotice(`${invoiceText} saved once.${whatsappText}`)
      setCart([])
      setNotes('')
      setClientRequestId(laundryApi.newRequestId())
    } catch (saveError) {
      setError(saveError.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="pos-page">
      <section className="catalog-pane">
        <div className="pos-heading"><div><span className="eyebrow green">NEW ORDER</span><h1>Create laundry order</h1><p>Select services and capture physical garment counts.</p></div><div className="order-mode"><button className="selected">Walk-in</button><button>Pickup</button><button>Delivery</button></div></div>
        <div className="catalog-tools"><label><span>⌕</span><input placeholder="Search service or scan barcode" /></label><button>▦ Scan</button></div>
        <div className="category-tabs">{categories.map(item => <button key={item} onClick={() => { setCategory(item); setServiceGroup('All') }} className={category === item ? 'active' : ''}>{item}</button>)}</div>
        {serviceGroups.length > 0 && <div className="group-tabs">{['All', ...serviceGroups].map(group => <button key={group} onClick={() => setServiceGroup(group)} className={serviceGroup === group ? 'active' : ''}>{group}</button>)}</div>}
        <div className="catalog-result"><span>{serviceGroup === 'All' ? category : `${category} · ${serviceGroup}`}</span><b>{visibleServices.length} services</b></div>
        <div className="service-grid">{visibleServices.map(service => <button type="button" className="service-card" onClick={() => addService(service)} key={service.id}><span className={`service-icon ${service.tint}`}>{service.icon}</span><div><b>{service.name}</b><small>{money.format(service.rate)} / {service.unit === 'KG' ? 'kg' : 'piece'}</small></div><em>＋</em></button>)}</div>
        {visibleServices.length === 0 && <div className="catalog-empty">No active services found. Check the API connection or service catalog.</div>}
        <button className="custom-service" onClick={() => setShowCustom(open => !open)}>＋ Add a custom item</button>
        {showCustom && <form className="custom-item-form" onSubmit={addCustomItem}><div className="custom-form-head"><div><b>Custom item</b><small>Add an uncommon garment without changing the main catalog.</small></div><button type="button" onClick={() => setShowCustom(false)}>×</button></div><div className="custom-fields"><label>Item name<input required value={customItem.name} onChange={event => setCustomItem(item => ({ ...item, name: event.target.value }))} placeholder="Item name" /></label><label>Pricing unit<select value={customItem.unit} onChange={event => setCustomItem(item => ({ ...item, unit: event.target.value }))}><option value="PIECE">Per piece</option><option value="KG">Per kg</option></select></label><label>Quantity<input required min="0.001" step="0.001" type="number" value={customItem.quantity} onChange={event => setCustomItem(item => ({ ...item, quantity: event.target.value }))} /></label><label>Physical pieces<input min="0" type="number" value={customItem.pieces} onChange={event => setCustomItem(item => ({ ...item, pieces: event.target.value }))} /></label><label>Rate<input required min="0.01" step="0.01" type="number" value={customItem.rate} onChange={event => setCustomItem(item => ({ ...item, rate: event.target.value }))} placeholder="₹ 0.00" /></label><label>HSN/SAC<input value={customItem.hsn} onChange={event => setCustomItem(item => ({ ...item, hsn: event.target.value }))} placeholder="Optional" /></label></div><div className="custom-options"><label><input type="checkbox" /> Include tax</label><label><input type="checkbox" checked={customItem.noPrint} onChange={event => setCustomItem(item => ({ ...item, noPrint: event.target.checked }))} /> No tag print</label><label><input type="checkbox" checked={customItem.noCount} onChange={event => setCustomItem(item => ({ ...item, noCount: event.target.checked }))} /> No piece count</label><button className="primary" type="submit">Add to bill</button></div></form>}
      </section>
      <aside className="cart-pane">
        <div className="cart-head"><div><span className="eyebrow green">CURRENT BILL</span><h2>Order summary</h2></div><button type="button" className="ghost" onClick={resetBill}>Clear</button></div>
        <label className="customer-select">Customer<select value={selectedCustomerId} onChange={event => setSelectedCustomerId(event.target.value)}><option value="">Select customer</option>{customers.map(customer => <option value={customer.id} key={customer.id}>{customer.name} · {customer.phone}</option>)}</select></label>
        {openOrderCount > 0 && <div className="open-order-warning"><span>!</span><div><b>{openOrderCount} open order{openOrderCount > 1 ? 's' : ''} found</b><small>A separate bill is allowed, but review the existing order first.</small></div><button type="button">Review</button></div>}
        <div className="cart-items">{cart.map(item => <article className="cart-item" key={item.id}><div className="cart-item-top"><div><b>{item.name}</b><small>{money.format(item.rate)} / {item.unit.toLowerCase()}</small></div><button onClick={() => removeItem(item.id)}>×</button></div><div className="item-inputs"><label>{item.unit === 'KG' ? 'Weight (kg)' : 'Quantity'}<input type="number" step={item.unit === 'KG' ? '.01' : '1'} value={item.quantity} onChange={event => updateItem(item.id, 'quantity', event.target.value)} /></label><label>Physical pieces<input type="number" value={item.pieces} onChange={event => updateItem(item.id, 'pieces', event.target.value)} /></label><strong>{money.format(item.rate * item.quantity)}</strong></div></article>)}</div>
        <div className="schedule-row"><label>Delivery date<input type="date" value={deliveryDate} onChange={event => setDeliveryDate(event.target.value)} /></label><label>Time<input type="time" value={deliveryTime} onChange={event => setDeliveryTime(event.target.value)} /></label></div>
        <label className="notes-field">Order notes<input value={notes} onChange={event => setNotes(event.target.value)} placeholder="Stains, fabric care, customer request..." /></label>
        <div className="auto-whatsapp"><span>✓</span><span><b>Automatic WhatsApp invoice + UPI QR</b><small>Finalising the invoice sends one payment-ready image through the official provider.</small></span></div>
        <div className="bill-totals"><div><span>Subtotal</span><b>{money.format(subtotal)}</b></div><div><span>Physical pieces</span><b>{pieces}</b></div><div><span>Round off</span><b>{money.format(rounded - subtotal)}</b></div><div className="grand-total"><span>Total</span><strong>{money.format(rounded)}</strong></div></div>
        {notice && <div className="success-message">✓ {notice}</div>}
        {error && <div className="form-error" role="alert">! {error}</div>}
        <div className="cart-actions"><button type="button" className="secondary" disabled={saving} onClick={() => saveOrder(false)}>{saving ? 'Saving…' : 'Save order'}</button><button type="button" className="primary" disabled={saving} onClick={() => saveOrder(true)}>{saving ? 'Saving…' : 'Create invoice →'}</button></div>
      </aside>
    </div>
  )
}

function Customers({ customers, onCreateCustomer }) {
  const [query, setQuery] = useState('')
  const [showAdd, setShowAdd] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ name: '', phone: '', area: '', addressLine: '' })
  const filtered = customers.filter(customer => `${customer.name} ${customer.phone} ${customer.area}`.toLowerCase().includes(query.toLowerCase()))
  const submit = async event => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await onCreateCustomer(form)
      setForm({ name: '', phone: '', area: '', addressLine: '' })
      setShowAdd(false)
    } catch (createError) {
      setError(createError.message)
    } finally {
      setSaving(false)
    }
  }
  return <div className="page-content"><section className="page-heading"><div><span className="eyebrow green">CUSTOMERS</span><h1>Customer records</h1><p>One profile, every order and payment.</p></div><button className="primary" onClick={() => setShowAdd(open => !open)}>＋ Add customer</button></section>
    {showAdd && <form className="panel customer-create-form" onSubmit={submit}><div><h3>Add customer</h3><p>The phone number is unique and prevents duplicate customer profiles.</p></div><label>Name<input required value={form.name} onChange={event => setForm(value => ({ ...value, name: event.target.value }))} /></label><label>Phone<input required value={form.phone} onChange={event => setForm(value => ({ ...value, phone: event.target.value }))} /></label><label>Area<input value={form.area} onChange={event => setForm(value => ({ ...value, area: event.target.value }))} /></label><label>Address<input value={form.addressLine} onChange={event => setForm(value => ({ ...value, addressLine: event.target.value }))} /></label><button type="button" className="ghost" onClick={() => setShowAdd(false)}>Cancel</button><button className="primary" disabled={saving}>{saving ? 'Saving…' : 'Save customer'}</button>{error && <div className="form-error" role="alert">! {error}</div>}</form>}
    <section className="panel customer-panel"><div className="list-tools"><label className="global-search"><span>⌕</span><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search name, phone or area" /></label><button className="ghost">All customers⌄</button></div><div className="customer-list">{filtered.map(customer => <article key={customer.id}><span className="avatar">{customer.name.split(' ').map(part => part[0]).slice(0,2).join('')}</span><div><b>{customer.name}</b><small>{customer.phone} · {customer.area || 'Area not set'}</small></div><div><span>Open orders</span><b>{customer.openOrders}</b></div><div><span>Balance due</span><b className={customer.due ? 'danger-text' : ''}>{money.format(customer.due)}</b></div><button className="ghost">View profile →</button></article>)}{filtered.length === 0 && <div className="list-empty">No customers found.</div>}</div></section></div>
}

function OrdersPage({ mode, orders }) {
  const isReady = mode === 'ready'
  const visible = orders.filter(order => isReady ? order.workStatus === 'READY' : order.workStatus !== 'READY')
  const totalFor = rows => money.format(rows.reduce((sum, order) => sum + order.total, 0))
  const overdue = visible.filter(order => order.delivery.startsWith('Overdue'))
  const scheduled = visible.filter(order => !order.delivery.startsWith('Overdue'))
  const summary = [
    [isReady ? 'Scheduled' : 'Active', scheduled.length, totalFor(scheduled)],
    ['Unpaid', visible.filter(order => order.payment !== 'PAID').length, totalFor(visible.filter(order => order.payment !== 'PAID'))],
    ['Overdue', overdue.length, totalFor(overdue)],
    [isReady ? 'All ready' : 'All in process', visible.length, totalFor(visible)],
  ]
  return <div className="page-content"><section className="page-heading"><div><span className="eyebrow green">OPERATIONS</span><h1>{isReady ? 'Ready for delivery' : 'In-process orders'}</h1><p>{isReady ? 'Collect payment, print receipt and hand over safely.' : 'Track washing, ironing and cleaning progress.'}</p></div><button className="primary">Batch update⌄</button></section><section className="status-summary">{summary.map(([label, value, amount], index) => <article className={index === 2 ? 'alert' : ''} key={label}><span>{index === 2 ? '!' : isReady ? '✓' : '◌'}</span><div><small>{label}</small><strong>{value}</strong><em>{amount}</em></div></article>)}</section><section className="panel orders-panel"><div className="list-tools"><label className="global-search"><span>⌕</span><input placeholder="Filter loaded orders" /></label><div><button className="ghost">Payment status⌄</button><button className="ghost">Staff⌄</button><button className="ghost">Export ↗</button></div></div><OrderTable rows={visible} /></section></div>
}

function Payments({ orders, createdBy, onRecordPayment, onLoadDocument, onQueueWhatsapp, onMarkTagsPrinted }) {
  const [orderNumber, setOrderNumber] = useState('')
  const [documentData, setDocumentData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [paymentRequestId, setPaymentRequestId] = useState(() => laundryApi.newRequestId())
  const [payment, setPayment] = useState({ mode: 'CASH', transactionReference: '', amount: '' })

  const loadDocument = useCallback(async selected => {
    if (!selected) return
    setLoading(true)
    setError('')
    setNotice('')
    try {
      const result = await onLoadDocument(selected)
      setDocumentData(result)
      setPayment(value => ({ ...value, amount: Number(result.balance) > 0 ? String(result.balance) : '' }))
    } catch (loadError) {
      setDocumentData(null)
      setError(loadError.message)
    } finally {
      setLoading(false)
    }
  }, [onLoadDocument])

  useEffect(() => {
    if (!orderNumber && orders[0]) {
      setOrderNumber(orders[0].id)
      loadDocument(orders[0].id)
    }
  }, [loadDocument, orderNumber, orders])

  const recordPayment = async event => {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await onRecordPayment({
        orderNumber,
        mode: payment.mode,
        transactionReference: payment.transactionReference,
        amount: Number(payment.amount),
        paidAt: new Date().toISOString(),
        createdBy,
      }, paymentRequestId)
      setPaymentRequestId(laundryApi.newRequestId())
      setPayment(value => ({ ...value, transactionReference: '' }))
      await loadDocument(orderNumber)
      setNotice('Payment recorded once. The updated paid amount and balance are automatically sent on WhatsApp when the provider is configured.')
    } catch (paymentError) {
      setError(paymentError.message)
    } finally {
      setSaving(false)
    }
  }

  const runDocumentAction = async action => {
    setError('')
    try {
      if (!documentData) throw new Error('Load an invoiced order first.')
      action(documentData)
      return true
    } catch (actionError) {
      setError(actionError.message)
      return false
    }
  }

  const printTags = async () => {
    const opened = await runDocumentAction(printGarmentTags)
    if (opened && documentData?.tags.length) await onMarkTagsPrinted(orderNumber).catch(() => {})
  }

  const queueWhatsapp = async () => {
    setError('')
    setNotice('')
    try {
      const result = await onQueueWhatsapp(orderNumber)
      setNotice(result.status === 'WAITING_FOR_PROVIDER'
        ? 'Invoice + UPI QR are queued. Sending starts after the official WhatsApp settings are completed.'
        : result.status === 'FAILED'
          ? `WhatsApp send failed: ${result.lastError || 'check the provider settings.'}`
          : result.status === 'SENT'
            ? 'Invoice and payment QR were sent on WhatsApp.'
            : `WhatsApp status: ${result.status}`)
    } catch (queueError) {
      setError(queueError.message)
    }
  }

  return <div className="page-content"><section className="page-heading"><div><span className="eyebrow green">COLLECTIONS & DOCUMENTS</span><h1>Payments and invoice</h1><p>Record Cash/UPI/Card payments, print receipts and create garment tags.</p></div></section>
    <section className="panel payment-workspace"><div className="payment-picker"><label>Order number<input list="payment-order-list" value={orderNumber} onChange={event => setOrderNumber(event.target.value)} placeholder="SO-2026-000001" /><datalist id="payment-order-list">{orders.map(order => <option key={order.id} value={order.id}>{order.customer} · {money.format(order.total)}</option>)}</datalist></label><button className="ghost" disabled={!orderNumber || loading} onClick={() => loadDocument(orderNumber)}>{loading ? 'Loading…' : 'Refresh'}</button></div>
      {error && <div className="form-error" role="alert">! {error}</div>}{notice && <div className="success-message">✓ {notice}</div>}
      {documentData && <><div className="payment-summary"><article><span>Customer</span><strong>{documentData.customer.name}</strong><small>{documentData.customer.phone}</small></article><article><span>Invoice</span><strong>{documentData.invoiceNumber}</strong><small>{documentData.orderNumber}</small></article><article><span>Total</span><strong>{money.format(Number(documentData.total))}</strong><small>{documentData.items.length} service lines</small></article><article><span>Paid</span><strong className="paid-text">{money.format(Number(documentData.amountPaid))}</strong><small>{documentData.paymentStatus}</small></article><article><span>Balance</span><strong className={Number(documentData.balance) > 0 ? 'danger-text' : 'paid-text'}>{money.format(Number(documentData.balance))}</strong><small>{documentData.payments.length} payments</small></article></div>
        <div className="document-actions"><button className="ghost" onClick={() => runDocumentAction(printInvoice)}>Print invoice</button><button className="ghost" onClick={printTags}>Print {documentData.tags.length} garment tags</button><button className="secondary" onClick={() => runDocumentAction(downloadInvoiceImage)}>Download invoice image</button><button className="primary" onClick={queueWhatsapp}>Send / retry WhatsApp</button></div>
        <div className="payment-columns"><form className="payment-form" onSubmit={recordPayment}><div><h3>Record payment</h3><p>The request ID is reused during retries to prevent duplicate payment entries.</p></div><label>Mode<select value={payment.mode} onChange={event => setPayment(value => ({ ...value, mode: event.target.value }))}><option value="CASH">Cash</option><option value="UPI">UPI</option><option value="CARD">Card</option><option value="BANK_TRANSFER">Bank transfer</option></select></label><label>Transaction reference<input value={payment.transactionReference} onChange={event => setPayment(value => ({ ...value, transactionReference: event.target.value }))} placeholder="Optional for cash" /></label><label>Amount<input required min="0.01" max={documentData.balance} step="0.01" type="number" value={payment.amount} onChange={event => setPayment(value => ({ ...value, amount: event.target.value }))} /></label><button className="primary" disabled={saving || Number(documentData.balance) <= 0}>{saving ? 'Saving…' : Number(documentData.balance) <= 0 ? 'Fully paid' : 'Record payment'}</button></form>
          <section className="payment-history"><h3>Payment history</h3>{documentData.payments.length === 0 && <div className="list-empty">No payment recorded.</div>}{documentData.payments.map(row => <article key={row.paymentNumber}><div><b>{row.paymentNumber}</b><small>{new Date(row.paidAt).toLocaleString('en-IN')} · {row.mode}</small></div><strong>{money.format(Number(row.amount))}</strong></article>)}</section></div></>}
    </section></div>
}

function reportRange(period) {
  const now = new Date()
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const start = new Date(end)
  if (period === 'week') {
    const day = start.getDay() || 7
    start.setDate(start.getDate() - day + 1)
  } else if (period === 'month') {
    start.setDate(1)
  }
  const iso = value => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
  return { from: iso(start), to: iso(end) }
}

function Reports({ onLoadReport }) {
  const [period, setPeriod] = useState('month')
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    let active = true
    const range = reportRange(period)
    setLoading(true)
    setError('')
    onLoadReport(range.from, range.to)
      .then(result => { if (active) setReport(result) })
      .catch(loadError => { if (active) setError(loadError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [onLoadReport, period])
  const range = reportRange(period)
  const serviceSales = report?.serviceSales || []
  const gross = Number(report?.grossSales || 0)
  return <div className="page-content"><section className="page-heading"><div><span className="eyebrow green">SALES INSIGHTS</span><h1>Reports</h1><p>Daily, weekly, monthly and product-wise sales from saved orders.</p></div><button className="primary">Export report ↗</button></section>
    <section className="report-toolbar"><div><button className={period === 'today' ? 'active' : ''} onClick={() => setPeriod('today')}>Today</button><button className={period === 'week' ? 'active' : ''} onClick={() => setPeriod('week')}>This week</button><button className={period === 'month' ? 'active' : ''} onClick={() => setPeriod('month')}>This month</button></div><span>{range.from} — {range.to}</span></section>
    {error && <div className="form-error" role="alert">! {error}</div>}
    <section className="report-metrics"><article><span>Gross sales</span><strong>{loading ? '…' : money.format(gross)}</strong><em>Saved bills</em></article><article><span>Orders</span><strong>{loading ? '…' : report?.orderCount || 0}</strong><em>Selected period</em></article><article><span>Average bill</span><strong>{loading ? '…' : money.format(Number(report?.averageBill || 0))}</strong><em>Gross ÷ orders</em></article><article><span>Products/services</span><strong>{loading ? '…' : serviceSales.length}</strong><em>With recorded sales</em></article></section>
    <section className="panel service-report"><div className="panel-head"><div><h3>Product/service-wise sales</h3><p>Performance for the selected daily, weekly or monthly period</p></div><button className="ghost">Download CSV ↗</button></div><div className="table-wrap"><table><thead><tr><th>Service</th><th>Sales</th><th>Orders</th><th>Billable quantity</th><th>Physical pieces</th><th>Share</th></tr></thead><tbody>{serviceSales.map(item => { const share = gross > 0 ? Math.round(Number(item.sales) / gross * 100) : 0; return <tr key={item.serviceCode}><td><strong>{item.serviceName}</strong></td><td><strong>{money.format(Number(item.sales))}</strong></td><td>{item.orderCount}</td><td>{Number(item.billableQuantity).toLocaleString('en-IN')}</td><td>{item.physicalPieces}</td><td><div className="share"><i><em style={{ width: `${share}%` }}></em></i><b>{share}%</b></div></td></tr> })}</tbody></table>{!loading && serviceSales.length === 0 && <div className="list-empty">No sales found for this period.</div>}</div></section>
  </div>
}

function Placeholder({ page }) {
  const labels = { pickups: 'Pickup & delivery' }
  return <div className="page-content"><section className="page-heading"><div><span className="eyebrow green">COMING NEXT</span><h1>{labels[page]}</h1><p>This module is included in the agreed system architecture.</p></div></section><section className="panel empty-state"><span>◇</span><h3>{labels[page]} foundation is ready</h3><p>Detailed workflow implementation follows the billing and order-management approval.</p></section></div>
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(false)
  const [currentUser, setCurrentUser] = useState('')
  const [page, setPage] = useState('dashboard')
  const [data, setData] = useState({ services: [], customers: [], orders: [], metrics: [] })

  const loadWorkspace = useCallback(async () => {
    if (demoMode) {
      setData({ services: mockServices, customers: mockCustomers, orders: mockOrders, metrics: mockMetrics })
      return
    }
    const [summary, customerRows, serviceRows, orderRows] = await Promise.all([
      laundryApi.dashboard(), laundryApi.customers(''), laundryApi.services(), laundryApi.orders(openStatuses),
    ])
    setData({
      services: serviceRows.map(normalizeService),
      customers: customerRows.map(normalizeCustomer),
      orders: orderRows.map(normalizeOrder),
      metrics: dashboardMetrics(summary),
    })
  }, [])

  const handleLogin = async ({ username, password }) => {
    try {
      if (!demoMode) {
        laundryApi.setCredentials(username, password)
        await laundryApi.verifyLogin()
      }
      await loadWorkspace()
      setCurrentUser(username)
      setLoggedIn(true)
    } catch (error) {
      laundryApi.clearCredentials()
      throw error
    }
  }

  const handleLogout = () => {
    laundryApi.clearCredentials()
    setLoggedIn(false)
    setCurrentUser('')
    setPage('dashboard')
    setData({ services: [], customers: [], orders: [], metrics: [] })
  }

  const handleCreateOrder = useCallback(async (payload, requestId, finalise, autoWhatsapp) => {
    if (demoMode) {
      const orderNumber = `SO-DEMO-${String(Date.now()).slice(-6)}`
      const result = { ...payload, orderNumber, invoiceNumber: finalise ? `INV-DEMO-${String(Date.now()).slice(-6)}` : null }
      return { ...result, whatsappStatus: autoWhatsapp && finalise ? 'WAITING_FOR_PROVIDER' : null }
    }
    const created = await laundryApi.createOrder(payload, requestId)
    const result = finalise ? await laundryApi.finaliseOrder(created.orderNumber) : created
    const whatsapp = finalise && autoWhatsapp
      ? await laundryApi.queueWhatsappInvoice(result.orderNumber)
      : null
    await loadWorkspace()
    return { ...result, whatsappStatus: whatsapp?.status || null, whatsappError: whatsapp?.lastError || null }
  }, [loadWorkspace])

  const handleOpenOrders = useCallback(async customerId => {
    if (!demoMode) return laundryApi.openOrders(customerId)
    const customer = mockCustomers.find(item => Number(item.id) === Number(customerId))
    return Array.from({ length: customer?.openOrders || 0 }, (_, index) => ({ orderNumber: `DEMO-${index + 1}` }))
  }, [])

  const handleCreateCustomer = useCallback(async customer => {
    if (demoMode) {
      setData(current => ({ ...current, customers: [...current.customers, normalizeCustomer({ ...customer, id: Date.now() })] }))
      return
    }
    await laundryApi.createCustomer(customer)
    await loadWorkspace()
  }, [loadWorkspace])

  const handleLoadReport = useCallback(async (from, to) => {
    if (demoMode) return { from, to, grossSales: 0, orderCount: 0, averageBill: 0, serviceSales: [] }
    return laundryApi.salesReport(from, to)
  }, [])

  const handleRecordPayment = useCallback(async (payment, requestId) => {
    if (demoMode) return payment
    const result = await laundryApi.recordPayment(payment, requestId)
    await loadWorkspace()
    return result
  }, [loadWorkspace])

  const handleLoadDocument = useCallback(async orderNumber => {
    if (demoMode) throw new Error('Invoice documents require the functional backend mode.')
    return laundryApi.invoiceDocument(orderNumber)
  }, [])

  const handleQueueWhatsapp = useCallback(async orderNumber => {
    if (demoMode) return { status: 'WAITING_FOR_PROVIDER' }
    return laundryApi.queueWhatsappInvoice(orderNumber)
  }, [])

  const handleMarkTagsPrinted = useCallback(async orderNumber => {
    if (!demoMode) await laundryApi.markTagsPrinted(orderNumber)
  }, [])

  if (!loggedIn) return <Login onLogin={handleLogin} apiUrl={laundryApi.apiUrl} />
  const titles = Object.fromEntries(navItems.map(([id, label]) => [id, label]))
  return <div className="app-shell"><Sidebar page={page} setPage={setPage} readyCount={data.orders.filter(order => order.workStatus === 'READY').length} /><div className="main-shell"><Topbar title={titles[page]} onLogout={handleLogout} setPage={setPage} />{page === 'dashboard' && <Dashboard setPage={setPage} metrics={data.metrics} orders={data.orders} />}{page === 'new-order' && <NewOrder services={data.services} customers={data.customers} onCreateOrder={handleCreateOrder} checkOpenOrders={handleOpenOrders} createdBy={currentUser} />}{page === 'customers' && <Customers customers={data.customers} onCreateCustomer={handleCreateCustomer} />}{page === 'in-process' && <OrdersPage mode="in-process" orders={data.orders} />}{page === 'ready' && <OrdersPage mode="ready" orders={data.orders} />}{page === 'payments' && <Payments orders={data.orders} createdBy={currentUser} onRecordPayment={handleRecordPayment} onLoadDocument={handleLoadDocument} onQueueWhatsapp={handleQueueWhatsapp} onMarkTagsPrinted={handleMarkTagsPrinted} />}{page === 'reports' && <Reports onLoadReport={handleLoadReport} />}{page === 'pickups' && <Placeholder page={page} />}</div></div>
}
