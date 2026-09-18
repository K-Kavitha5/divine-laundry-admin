const services = [
  { id: 1, category: 'Laundry by kg', name: 'Wash & Iron', rate: 120, unit: 'KG', icon: '♨', tint: 'mint' },
  { id: 2, category: 'Laundry by kg', name: 'Wash & Fold', rate: 90, unit: 'KG', icon: '◫', tint: 'sky' },
  { id: 3, category: 'Laundry by kg', name: 'Wash & Fold - Express', rate: 100, unit: 'KG', icon: '⚡', tint: 'sand' },
  { id: 4, category: 'Ironing', name: 'Shirt', rate: 14, unit: 'PIECE', icon: '♧', tint: 'sky' },
  { id: 5, category: 'Ironing', name: 'T-shirt', rate: 14, unit: 'PIECE', icon: '♜', tint: 'mint' },
  { id: 6, category: 'Ironing', name: 'Pant / Trouser', rate: 14, unit: 'PIECE', icon: '♢', tint: 'violet' },
  { id: 7, category: 'Dry clean', group: 'Men', name: 'Blazer / Coat', rate: 300, unit: 'PIECE', icon: '♙', tint: 'sand' },
  { id: 8, category: 'Dry clean', group: 'Women', name: 'Long Dress', rate: 120, unit: 'PIECE', icon: '♛', tint: 'mint' },
  { id: 9, category: 'Shoe cleaning', name: 'Sports shoes', rate: 249, unit: 'PIECE', icon: '◒', tint: 'rose' },
  { id: 10, category: 'Shoe cleaning', name: 'Leather shoes', rate: 249, unit: 'PIECE', icon: '◓', tint: 'sand' },
  { id: 11, category: 'Sofa cleaning', name: 'Sofa — 1 seater', rate: 190, unit: 'PIECE', icon: '▰', tint: 'mint' },
  { id: 70, category: 'Laundry by kg', name: 'Wash & Iron - Express', rate: 150, unit: 'KG', icon: '⚡', tint: 'mint' },
  { id: 71, category: 'Ironing', name: 'Long Dress', rate: 20, unit: 'PIECE', icon: '♛', tint: 'rose' },
  { id: 72, category: 'Ironing', name: 'Pillow Cover', rate: 10, unit: 'PIECE', icon: '▰', tint: 'rose' },
  { id: 73, category: 'Ironing', name: 'Coat / Blazer', rate: 100, unit: 'PIECE', icon: '♜', tint: 'rose' },
  { id: 74, category: 'Ironing', name: 'Over Coat', rate: 50, unit: 'PIECE', icon: '♜', tint: 'rose' },
  { id: 75, category: 'Shoe cleaning', name: 'Canvas Shoes', rate: 249, unit: 'PIECE', icon: '◓', tint: 'sky' },
  { id: 76, category: 'Shoe cleaning', name: 'Leather Shoes', rate: 249, unit: 'PIECE', icon: '◓', tint: 'sky' },
  { id: 77, category: 'Shoe cleaning', name: 'Suede Shoes', rate: 249, unit: 'PIECE', icon: '◒', tint: 'sky' },
  { id: 78, category: 'Shoe cleaning', name: 'Crocs / Sandals', rate: 120, unit: 'PIECE', icon: '◒', tint: 'sky' },
  { id: 79, category: 'Shoe cleaning', name: 'Slippers', rate: 199, unit: 'PIECE', icon: '◓', tint: 'sky' },
]

const dryCleanCatalog = {
  Men: [['White Shirt', 99, '♙'], ['T-Shirt', 80, '♜'], ['Vest', 149, '♢'], ['Under Wear', 59, '⌄'], ['Long Pullover', 99, '♧'], ['Sweat Pants', 119, 'Ⅱ'], ['Capri', 129, 'Ⅱ'], ['Pyjama', 300, '♙'], ['Track Pant', 119, 'Ⅱ'], ['Shorts', 109, '▽'], ['Long Coat', 399, '♜'], ['Jeans', 139, 'Ⅱ'], ['Achkan', 199, '♜'], ['Tie', 99, '♦'], ['Sherwani Set', 350, '♙']],
  Women: [['Petticoat', 299, '♛'], ['Brassieres', 99, '◡'], ['Track Pant', 199, 'Ⅱ'], ['Stockings', 199, '♢'], ['Long Pullover', 99, '♧'], ['Long Coat', 399, '♜'], ['Blazer / Coat', 359, '♜'], ['Scarf', 139, '⌁'], ['Slacks', 99, 'Ⅱ'], ['Capri', 199, 'Ⅱ'], ['Leggings', 199, 'Ⅱ'], ['Jumper', 199, '♧'], ['Dangree', 299, '♛'], ['Jeans', 139, 'Ⅱ'], ['Pants', 129, 'Ⅱ'], ['T-Shirt', 80, '♜'], ['Shirt', 80, '♙']],
  Kids: [['Baby Blanket', 199, '▤'], ['Track Pant', 199, 'Ⅱ'], ['Leggings', 199, 'Ⅱ'], ['Swimming Costume', 199, '♧'], ['Long Pullover', 299, '♧'], ['Sherwani', 199, '♙'], ['Dangree', 199, '♛'], ['Jumper', 199, '♧'], ['Shorts', 49, '▽'], ['Capri', 99, 'Ⅱ'], ['Jeans', 70, 'Ⅱ'], ['Pants', 70, 'Ⅱ'], ['T-Shirt', 70, '♜']],
  Household: [['Table Runner', 179, '▰'], ['Foot Mat', 59, '▤'], ['Table Mat', 89, '▰'], ['Bath Robe', 89, '♙'], ['Socks', 49, '♢'], ['Handbag', 399, '▰']],
  Accessories: [['Handkerchief', 49, '◇'], ['Tie', 59, '♦'], ['Rain Coat', 299, '♧'], ['Hat', 199, '⌂'], ['Cap', 100, '◒'], ['Muffler', 199, '⌁']],
}

const groupTints = { Men: 'sand', Women: 'mint', Kids: 'rose', Household: 'sky', Accessories: 'violet' }
let nextServiceId = 12
Object.entries(dryCleanCatalog).forEach(([group, items]) => items.forEach(([name, rate, icon]) => services.push({ id: nextServiceId++, category: 'Dry clean', group, name, rate, unit: 'PIECE', icon, tint: groupTints[group] })))

const seedCustomers = [
  { id: 1, name: 'Arun Kumar', phone: '98765 43210', area: 'Thillai Nagar', openOrders: 2, due: 218 },
  { id: 2, name: 'Meena S', phone: '98421 55770', area: 'Woraiyur', openOrders: 1, due: 283 },
  { id: 3, name: 'Ravi Textiles', phone: '99444 22881', area: 'Cantonment', openOrders: 1, due: 160 },
  { id: 4, name: 'Karthik R', phone: '90927 20832', area: 'Srirangam', openOrders: 0, due: 0 },
  { id: 5, name: 'Divya N', phone: '90000 00005', area: 'KK Nagar', openOrders: 0, due: 0 },
]

const seedOrders = [
  { id: 'SO-2026-002009', customer: 'Arun Kumar', service: 'Wash & Iron · 1.39 kg · 7 pcs', delivery: 'Today · 7:04 PM', workStatus: 'CLEANED', payment: 'UNPAID', total: 160, staff: 'Noel' },
  { id: 'SO-2026-002008', customer: 'Meena S', service: 'Wash & Fold · 4.79 kg · 18 pcs', delivery: 'Today · 5:38 PM', workStatus: 'WASHING', payment: 'UNPAID', total: 478, staff: 'Noel' },
  { id: 'SO-2026-002007', customer: 'Ravi Textiles', service: 'Wash & Iron · 2.46 kg · 8 pcs', delivery: 'Overdue · 1:55 PM', workStatus: 'IRONING', payment: 'PARTIAL', total: 283, staff: 'Noel' },
  { id: 'SO-2026-002006', customer: 'Karthik R', service: 'Wash & Fold · 3.27 kg · 1 pc', delivery: 'Tomorrow · 11:54 AM', workStatus: 'READY', payment: 'PAID', total: 588, staff: 'Dhanush' },
  { id: 'SO-2026-002005', customer: 'Divya N', service: 'Pant / Trouser · 5 pcs', delivery: 'Tomorrow · 1:29 PM', workStatus: 'READY', payment: 'UNPAID', total: 401, staff: 'Noel' },
]

const STORAGE_KEY = 'divine-laundry-offline-test-v1'
const clone = value => JSON.parse(JSON.stringify(value))

function readPrototypeData() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
    if (saved && Array.isArray(saved.customers) && Array.isArray(saved.orders)) return saved
  } catch (error) {
    console.warn('Offline test data could not be read.', error)
  }
  return null
}

const savedPrototypeData = readPrototypeData()
let customers = savedPrototypeData?.customers?.length ? savedPrototypeData.customers : clone(seedCustomers)
let orders = savedPrototypeData?.orders?.length ? savedPrototypeData.orders : clone(seedOrders)

function savePrototypeData() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ customers, orders }))
  } catch (error) {
    console.warn('Offline test data could not be saved.', error)
  }
}

function nextOrderNumber() {
  const largest = orders.reduce((max, order) => {
    const suffix = Number(String(order.id).match(/(\d+)$/)?.[1] || 0)
    return Math.max(max, suffix)
  }, 2009)
  return `SO-2026-${String(largest + 1).padStart(6, '0')}`
}

const phoneDigits = value => String(value || '').replace(/\D/g, '')
const whatsAppPhone = value => {
  const digits = phoneDigits(value)
  return digits.length === 10 ? `91${digits}` : digits
}
const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character])
const orderPaid = order => Number(order.paid ?? (order.payment === 'PAID' ? order.total : order.payment === 'PARTIAL' ? Math.round(order.total / 2) : 0))
const orderBalance = order => Math.max(0, Number(order.total) - orderPaid(order))
const orderCustomer = order => customers.find(customer => customer.id === order.customerId) || customers.find(customer => customer.name === order.customer)

const metrics = [
  { label: 'Today sales', value: '₹12,480', change: '+8.4%', icon: '₹', tone: 'green' },
  { label: 'New orders', value: '28', change: '+6 today', icon: '＋', tone: 'blue' },
  { label: 'In process', value: '36', change: '15 overdue', icon: '◌', tone: 'violet' },
  { label: 'Ready', value: '55', change: '₹27,943', icon: '✓', tone: 'green' },
  { label: 'Unpaid', value: '₹11,247', change: '15 orders', icon: '!', tone: 'red' },
  { label: 'This month', value: '₹3.42L', change: '+12.6%', icon: '↗', tone: 'dark' },
]

const navItems = [
  ['dashboard', 'Dashboard', '⌂'], ['new-order', 'New order', '＋'], ['in-process', 'In process', '◌'],
  ['ready', 'Ready', '✓'], ['customers', 'Customers', '♙'], ['pickups', 'Pickup & delivery', '▱'],
  ['payments', 'Payments', '₹'], ['reports', 'Reports', '↗'],
]

const state = {
  loggedIn: false,
  page: 'dashboard',
  category: 'Laundry by kg',
  serviceGroup: 'All',
  customerId: customers[0]?.id || null,
  cart: [],
  notice: '',
  paymentNotice: '',
  customerQuery: '',
  quickOpen: false,
  customOpen: false,
  customerModalOpen: false,
  customerError: '',
  paymentOrderId: null,
  deliveryDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10),
  deliveryTime: '19:00',
  orderNotes: '',
  autoWhatsApp: true,
  finalising: false,
}

const root = document.querySelector('#root')
const money = value => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(value)
const status = value => `<span class="status ${value.toLowerCase().replaceAll(' ', '-')}">${value}</span>`

function customerModalTemplate() {
  if (!state.customerModalOpen) return ''
  return `<div class="modal-backdrop" id="customer-modal-backdrop"><section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="customer-modal-title">
    <div class="modal-head"><div><span class="eyebrow green">NEW CUSTOMER</span><h2 id="customer-modal-title">Create customer</h2><p>The phone number will be used for the test WhatsApp bill.</p></div><button type="button" id="close-customer-modal" aria-label="Close">×</button></div>
    <form id="customer-form"><label>Customer name<input name="name" required maxlength="80" placeholder="Example: Raj Kumar" autofocus></label>
      <label>WhatsApp phone number<input name="phone" required inputmode="numeric" maxlength="18" placeholder="10-digit mobile number"></label>
      <label>Area / address<input name="area" required maxlength="140" placeholder="Example: Thillai Nagar, Trichy"></label>
      ${state.customerError ? `<div class="form-error">${escapeHtml(state.customerError)}</div>` : ''}
      <div class="modal-actions"><button class="secondary" id="cancel-customer" type="button">Cancel</button><button class="primary" type="submit">Create & select customer →</button></div>
    </form>
  </section></div>`
}

function createInvoiceCanvas(order) {
  const customer = orderCustomer(order) || { name: order.customer, phone: '', area: '' }
  const items = order.items?.length ? order.items : [{ name: order.service, quantity: 1, rate: order.total, lineTotal: order.total, unit: 'ORDER', pieces: 0 }]
  const canvas = document.createElement('canvas')
  canvas.width = 1080
  canvas.height = 1350
  const context = canvas.getContext('2d')
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.fillStyle = '#075938'
  context.fillRect(0, 0, canvas.width, 190)
  context.fillStyle = '#ffffff'
  context.font = '700 52px Arial, sans-serif'
  context.fillText('DIVINE LAUNDRY', 70, 85)
  context.font = '24px Arial, sans-serif'
  context.fillText('Admin-generated laundry invoice', 70, 130)
  context.textAlign = 'right'
  context.font = '700 30px Arial, sans-serif'
  context.fillText(order.invoiceId || order.id.replace('SO-', 'INV-'), 1010, 82)
  context.font = '22px Arial, sans-serif'
  context.fillText(order.id, 1010, 124)
  context.textAlign = 'left'

  context.fillStyle = '#12251c'
  context.font = '700 27px Arial, sans-serif'
  context.fillText('BILL TO', 70, 250)
  context.font = '700 34px Arial, sans-serif'
  context.fillText(customer.name || 'Customer', 70, 300)
  context.font = '24px Arial, sans-serif'
  context.fillStyle = '#5f7168'
  context.fillText(customer.phone || '', 70, 340)
  context.fillText(customer.area || '', 70, 380)
  context.fillStyle = '#12251c'
  context.font = '700 25px Arial, sans-serif'
  context.fillText('Created', 650, 250)
  context.font = '24px Arial, sans-serif'
  context.fillStyle = '#5f7168'
  context.fillText(new Date(order.createdAt || Date.now()).toLocaleString('en-IN'), 650, 292)
  context.fillStyle = '#12251c'
  context.font = '700 25px Arial, sans-serif'
  context.fillText('Delivery', 650, 340)
  context.font = '24px Arial, sans-serif'
  context.fillStyle = '#5f7168'
  context.fillText(order.delivery || '-', 650, 380)

  const tableTop = 445
  context.fillStyle = '#e5f5ed'
  context.fillRect(60, tableTop, 960, 62)
  context.fillStyle = '#075938'
  context.font = '700 23px Arial, sans-serif'
  context.fillText('ITEM', 80, tableTop + 40)
  context.fillText('QTY', 600, tableTop + 40)
  context.fillText('RATE', 745, tableTop + 40)
  context.textAlign = 'right'
  context.fillText('AMOUNT', 995, tableTop + 40)
  context.textAlign = 'left'

  let y = tableTop + 105
  items.slice(0, 9).forEach(item => {
    const lineTotal = Number(item.lineTotal ?? item.rate * item.quantity)
    context.fillStyle = '#12251c'
    context.font = '700 24px Arial, sans-serif'
    context.fillText(String(item.name).slice(0, 35), 80, y)
    context.font = '22px Arial, sans-serif'
    context.fillStyle = '#5f7168'
    const quantity = Number(item.quantity || 0)
    context.fillText(`${quantity} ${String(item.unit || '').toLowerCase()}${item.pieces ? ` · ${item.pieces} pcs` : ''}`, 600, y)
    context.fillText(money(Number(item.rate || 0)), 745, y)
    context.textAlign = 'right'
    context.fillText(money(lineTotal), 995, y)
    context.textAlign = 'left'
    context.strokeStyle = '#e4ebe7'
    context.beginPath()
    context.moveTo(70, y + 28)
    context.lineTo(1010, y + 28)
    context.stroke()
    y += 72
  })
  if (items.length > 9) {
    context.fillStyle = '#5f7168'
    context.font = '20px Arial, sans-serif'
    context.fillText(`+ ${items.length - 9} more items`, 80, y)
    y += 50
  }

  const totalsTop = Math.max(y + 45, 955)
  context.fillStyle = '#f7faf8'
  context.fillRect(560, totalsTop, 460, 245)
  const totalRows = [
    ['Invoice total', Number(order.total)],
    ['Paid', orderPaid(order)],
    ['Balance', orderBalance(order)],
  ]
  totalRows.forEach(([label, amount], index) => {
    const rowY = totalsTop + 60 + index * 65
    context.fillStyle = index === 2 ? '#075938' : '#5f7168'
    context.font = `${index === 2 ? '700 30px' : '24px'} Arial, sans-serif`
    context.fillText(label, 590, rowY)
    context.textAlign = 'right'
    context.fillText(money(amount), 990, rowY)
    context.textAlign = 'left'
  })
  context.fillStyle = '#075938'
  context.font = '700 27px Arial, sans-serif'
  context.fillText(`Payment: ${order.payment || 'UNPAID'}`, 70, totalsTop + 65)
  context.fillStyle = '#5f7168'
  context.font = '22px Arial, sans-serif'
  context.fillText(`Mode: ${order.paymentMode || '-'}`, 70, totalsTop + 110)
  context.fillText('Thank you for choosing Divine Laundry.', 70, 1280)
  return canvas
}

function invoiceFilename(order) {
  return `${order.invoiceId || order.id.replace('SO-', 'INV-')}.png`
}

function downloadInvoiceImage(order) {
  const link = document.createElement('a')
  link.download = invoiceFilename(order)
  link.href = createInvoiceCanvas(order).toDataURL('image/png')
  link.click()
}

function whatsAppMessage(order) {
  const customer = orderCustomer(order) || { name: order.customer }
  return `Hello ${customer.name},\n\nDivine Laundry bill ${order.invoiceId || order.id}\nOrder: ${order.id}\nTotal: ${money(order.total)}\nPaid: ${money(orderPaid(order))}\nBalance: ${money(orderBalance(order))}\nPayment: ${order.payment}\n\nYour invoice image is attached/shared separately. Thank you.`
}

async function shareInvoiceToWhatsApp(order) {
  const customer = orderCustomer(order)
  if (!customer || phoneDigits(customer.phone).length < 10) {
    state.paymentNotice = 'Add a valid WhatsApp phone number to this customer first.'
    render()
    return
  }
  const message = whatsAppMessage(order)
  const canvas = createInvoiceCanvas(order)
  try {
    if (navigator.share && window.File) {
      const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'))
      const file = new File([blob], invoiceFilename(order), { type: 'image/png' })
      if (!navigator.canShare || navigator.canShare({ files: [file] })) {
        await navigator.share({ title: `Divine Laundry ${order.id}`, text: message, files: [file] })
        state.paymentNotice = 'Invoice image shared from this device.'
        render()
        return
      }
    }
  } catch (error) {
    if (error?.name === 'AbortError') return
  }
  downloadInvoiceImage(order)
  const url = `https://wa.me/${whatsAppPhone(customer.phone)}?text=${encodeURIComponent(message)}`
  window.open(url, '_blank', 'noopener,noreferrer')
  state.paymentNotice = 'Invoice image downloaded. WhatsApp opened with the bill text; attach the downloaded PNG before sending.'
  render()
}

function loginTemplate() {
  return `<main class="login-page">
    <section class="login-story">
      <div class="brand brand-light"><span class="brand-mark">D</span><span>Divine Laundry</span></div>
      <div class="story-copy">
        <span class="eyebrow">ADMIN OPERATIONS</span>
        <h1>Every garment.<br>Tracked with care.</h1>
        <p>A faster, safer workspace for orders, billing, garment tags, pickups and customer communication.</p>
        <div class="story-stats"><div><strong>1</strong><span>order</span></div><i></i><div><strong>1</strong><span>invoice</span></div><i></i><div><strong>0</strong><span>duplicates</span></div></div>
      </div>
      <small>Private system · Authorised staff only</small>
    </section>
    <section class="login-panel">
      <form class="login-card" id="login-form">
        <div class="mobile-brand brand"><span class="brand-mark">D</span><span>Divine Laundry</span></div>
        <span class="eyebrow green">WELCOME BACK</span><h2>Sign in to your workspace</h2><p>Use your owner or staff account.</p>
        <label>Email or phone number<input value="admin" autocomplete="username"></label>
        <label>Password<input type="password" value="ChangeMe123!" autocomplete="current-password"></label>
        <div class="login-options"><label><input type="checkbox"> Remember me</label><button type="button" class="text-button">Forgot password?</button></div>
        <button class="primary wide" type="submit">Sign in securely <span>→</span></button>
        <div class="demo-note"><span>i</span> Prototype access is pre-filled for review.</div>
      </form>
    </section>
  </main>`
}

function sidebarTemplate() {
  const navigation = navItems.map(([id, label, icon]) => `<button data-page="${id}" class="${state.page === id ? 'active' : ''}"><span class="nav-icon">${icon}</span><span>${label}</span>${id === 'ready' ? '<em>55</em>' : ''}</button>`).join('')
  return `<aside class="sidebar">
    <div class="brand"><span class="brand-mark">D</span><span><b>Divine</b><small>Laundry Admin</small></span></div>
    <nav><span class="nav-label">WORKSPACE</span>${navigation}<span class="nav-label space">MANAGE</span>
      <button><span class="nav-icon">□</span><span>Services & prices</span></button><button><span class="nav-icon">⚙</span><span>Settings</span></button>
    </nav>
    <div class="sidebar-help"><span>?</span><div><b>Need help?</b><small>Setup & support</small></div><button>→</button></div>
  </aside>`
}

function topbarTemplate() {
  const label = navItems.find(([id]) => id === state.page)?.[1] || 'Dashboard'
  return `<header class="topbar"><div><span class="crumb">Divine Laundry /</span><strong>${label}</strong></div><div class="top-actions">
    <label class="global-search"><span>⌕</span><input placeholder="Order ID, phone or customer"></label><div class="quick-wrap"><button class="quick-trigger" id="quick-actions">＋ Quick actions <span>⌄</span></button>${state.quickOpen ? `<div class="quick-menu"><button data-page="pickups">Schedule pickup<span>→</span></button><button id="quick-create-customer">Create customer<span>→</span></button><button data-close-quick>Create staff<span>→</span></button><button data-close-quick>Create package<span>→</span></button><button data-close-quick>Create service<span>→</span></button><button data-close-quick>Create batch invoice<span>→</span></button><button data-close-quick>Give refund<span>→</span></button><button data-page="pickups">Recurring pickup<span>→</span></button></div>` : ''}</div><button class="round">◔</button><button class="round has-dot">♢</button>
    <button class="profile" id="logout"><span>DK</span><div><b>Divine Laundry</b><small>Owner</small></div><i>⌄</i></button>
  </div></header>`
}

function orderTable(rows = orders) {
  return `<div class="table-wrap"><table><thead><tr><th>Order</th><th>Customer</th><th>Service</th><th>Delivery</th><th>Status</th><th>Payment</th><th>Total</th><th></th></tr></thead><tbody>${rows.map(order => `<tr>
    <td><button class="order-link">${escapeHtml(order.id)}</button><small>${escapeHtml(order.staff)}</small></td><td><strong>${escapeHtml(order.customer)}</strong></td><td>${escapeHtml(order.service)}</td>
    <td class="${order.delivery.startsWith('Overdue') ? 'danger-text' : ''}">${escapeHtml(order.delivery)}</td><td>${status(order.workStatus)}</td><td>${status(order.payment)}</td><td><strong>${money(order.total)}</strong></td><td><button class="more">•••</button></td>
  </tr>`).join('')}</tbody></table></div>`
}

function dashboardTemplate() {
  const cards = metrics.map(metric => `<article class="metric ${metric.tone}"><div class="metric-top"><span>${metric.icon}</span><small>•••</small></div><p>${metric.label}</p><div><strong>${metric.value}</strong><em>${metric.change}</em></div></article>`).join('')
  const pipeline = [['Received', 18, 24], ['Washing', 12, 52], ['Ironing', 9, 68], ['Cleaned', 15, 82], ['Ready', 55, 100]].map(([label, count, width]) => `<div><span>${label}<b>${count}</b></span><i><em style="width:${width}%"></em></i></div>`).join('')
  return `<div class="page-content">
    <section class="page-heading"><div><span class="eyebrow green">TUESDAY, 1 SEPTEMBER</span><h1>Good evening, Barani</h1><p>Here is what needs your attention today.</p></div><button class="primary" data-page="new-order">＋ Create new order</button></section>
    <section class="metrics-grid">${cards}</section>
    <section class="dashboard-grid"><article class="panel pipeline-panel"><div class="panel-head"><div><h3>Order pipeline</h3><p>Current operational workload</p></div><button class="ghost">This week⌄</button></div><div class="pipeline">${pipeline}</div><div class="pipeline-note"><span>✓</span><div><b>68% on-time completion</b><small>5 orders are at risk of delay</small></div><button data-page="in-process">View orders →</button></div></article>
      <article class="panel delivery-panel"><div class="panel-head"><div><h3>Upcoming deliveries</h3><p>Next four hours</p></div><button class="icon-button">↗</button></div>${orders.slice(0, 4).map((order, index) => `<div class="delivery-item"><span class="time-dot t${index}"></span><div><b>${order.customer}</b><small>${order.service}</small></div><strong>${order.delivery.split('·')[1] || '11:30 AM'}</strong></div>`).join('')}</article></section>
    <section class="panel orders-panel"><div class="panel-head"><div><h3>Recent orders</h3><p>Live status across the outlet</p></div><button class="ghost" data-page="in-process">View all orders →</button></div>${orderTable()}</section>
  </div>`
}

function newOrderTemplate() {
  const categories = [...new Set(services.map(item => item.category))]
  const customer = customers.find(item => item.id === state.customerId)
  const subtotal = state.cart.reduce((sum, item) => sum + item.rate * item.quantity, 0)
  const rounded = Math.round(subtotal)
  const pieces = state.cart.reduce((sum, item) => sum + item.pieces, 0)
  const categoryButtons = categories.map(item => `<button data-category="${item}" class="${state.category === item ? 'active' : ''}">${item}</button>`).join('')
  const groups = [...new Set(services.filter(item => item.category === state.category).map(item => item.group).filter(Boolean))]
  const visibleServices = services.filter(item => item.category === state.category && (state.serviceGroup === 'All' || item.group === state.serviceGroup))
  const groupButtons = groups.length ? `<div class="group-tabs">${['All', ...groups].map(group => `<button data-service-group="${group}" class="${state.serviceGroup === group ? 'active' : ''}">${group}</button>`).join('')}</div>` : ''
  const serviceCards = visibleServices.map(service => `<button class="service-card" data-service="${service.id}"><span class="service-icon ${service.tint}">${service.icon}</span><div><b>${service.name}</b><small>${money(service.rate)} / ${service.unit === 'KG' ? 'kg' : 'piece'}</small></div><em>＋</em></button>`).join('')
  const options = customers.map(item => `<option value="${item.id}" ${item.id === state.customerId ? 'selected' : ''}>${escapeHtml(item.name)} · ${escapeHtml(item.phone)}</option>`).join('')
  const cartItems = state.cart.map(item => `<article class="cart-item"><div class="cart-item-top"><div><b>${item.name}</b><small>${money(item.rate)} / ${item.unit.toLowerCase()}</small></div><button data-remove="${item.id}">×</button></div><div class="item-inputs">
    <label>${item.unit === 'KG' ? 'Weight (kg)' : 'Quantity'}<input data-item="${item.id}" data-field="quantity" type="number" step="${item.unit === 'KG' ? '.01' : '1'}" value="${item.quantity}"></label>
    <label>Physical pieces<input data-item="${item.id}" data-field="pieces" type="number" value="${item.pieces}"></label><strong>${money(item.rate * item.quantity)}</strong></div></article>`).join('')
  return `<div class="pos-page"><section class="catalog-pane">
    <div class="pos-heading"><div><span class="eyebrow green">NEW ORDER</span><h1>Create laundry order</h1><p>Select services and capture physical garment counts.</p></div><div class="order-mode"><button class="selected">Walk-in</button><button>Pickup</button><button>Delivery</button></div></div>
    <div class="catalog-tools"><label><span>⌕</span><input placeholder="Search service or scan barcode"></label><button>▦ Scan</button></div><div class="category-tabs">${categoryButtons}</div>${groupButtons}<div class="catalog-result"><span>${state.serviceGroup === 'All' ? state.category : `${state.category} · ${state.serviceGroup}`}</span><b>${visibleServices.length} services</b></div><div class="service-grid">${serviceCards}</div><button class="custom-service" id="toggle-custom">＋ Add a custom item</button>
    ${state.customOpen ? `<form class="custom-item-form"><div class="custom-form-head"><div><b>Custom item</b><small>Add an uncommon garment without changing the main catalog.</small></div><button type="button" id="close-custom">×</button></div><div class="custom-fields"><label>Item name<input placeholder="Item name"></label><label>Pricing unit<select><option>Per piece</option><option>Per kg</option></select></label><label>Quantity<input type="number" value="1"></label><label>Physical pieces<input type="number" value="1"></label><label>Rate<input type="number" placeholder="₹ 0.00"></label><label>HSN/SAC<input placeholder="Optional"></label></div><div class="custom-options"><label><input type="checkbox"> Include tax</label><label><input type="checkbox"> No tag print</label><label><input type="checkbox"> No piece count</label><button class="primary" type="button">Add to bill</button></div></form>` : ''}
  </section><aside class="cart-pane"><div class="cart-head"><div><span class="eyebrow green">CURRENT BILL</span><h2>Order summary</h2></div><button class="ghost" id="clear-cart">Clear</button></div>
    <div class="customer-field-head"><b>Customer</b><button type="button" id="order-add-customer">＋ New customer</button></div><label class="customer-select"><select id="customer-select">${options}</select></label>
    ${customer?.openOrders ? `<div class="open-order-warning"><span>!</span><div><b>${customer.openOrders} open order${customer.openOrders > 1 ? 's' : ''} found</b><small>A new click creates a separate bill with its own number.</small></div><button>Review</button></div>` : ''}
    <div class="cart-items">${cartItems || '<div class="empty-state"><span>＋</span><h3>Bill is empty</h3><p>Select a service to begin.</p></div>'}</div>
    <div class="schedule-row"><label>Delivery date<input id="delivery-date" type="date" value="${state.deliveryDate}"></label><label>Time<input id="delivery-time" type="time" value="${state.deliveryTime}"></label></div><label class="notes-field">Order notes<input id="order-notes" value="${escapeHtml(state.orderNotes)}" placeholder="Stains, fabric care, customer request..."></label>
    <label class="auto-whatsapp"><input id="auto-whatsapp" type="checkbox" ${state.autoWhatsApp ? 'checked' : ''}><span><b>Prepare bill image for WhatsApp after payment</b><small>The offline test downloads/shares a PNG invoice—never a bill link.</small></span></label>
    <div class="bill-totals"><div><span>Subtotal</span><b>${money(subtotal)}</b></div><div><span>Physical pieces</span><b>${pieces}</b></div><div><span>Round off</span><b>${money(rounded - subtotal)}</b></div><div class="grand-total"><span>Total</span><strong>${money(rounded)}</strong></div></div>
    ${state.notice ? `<div class="success-message">✓ ${escapeHtml(state.notice)}</div>` : ''}<div class="cart-actions"><button class="secondary">Save draft</button><button class="primary" id="finalise" ${state.finalising ? 'disabled' : ''}>${state.finalising ? 'Creating…' : 'Create bill & payment →'}</button></div>
  </aside></div>`
}

function customersTemplate() {
  const filtered = customers.filter(customer => `${customer.name} ${customer.phone} ${customer.area}`.toLowerCase().includes(state.customerQuery.toLowerCase()))
  return `<div class="page-content"><section class="page-heading"><div><span class="eyebrow green">CUSTOMERS</span><h1>Customer records</h1><p>One profile, every order and payment. Offline test records stay in this browser.</p></div><button class="primary" id="open-customer-modal">＋ Add customer</button></section><section class="panel customer-panel"><div class="list-tools"><label class="global-search"><span>⌕</span><input id="customer-query" value="${escapeHtml(state.customerQuery)}" placeholder="Search name, phone or area"></label><button class="ghost">All customers⌄</button></div><div class="customer-list">${filtered.map(customer => `<article><span class="avatar">${escapeHtml(customer.name.split(' ').map(word => word[0]).slice(0, 2).join(''))}</span><div><b>${escapeHtml(customer.name)}</b><small>${escapeHtml(customer.phone)} · ${escapeHtml(customer.area)}</small></div><div><span>Open orders</span><b>${customer.openOrders}</b></div><div><span>Balance due</span><b class="${customer.due ? 'danger-text' : ''}">${money(customer.due)}</b></div><button class="ghost" data-start-order="${customer.id}">New order →</button></article>`).join('')}</div></section></div>`
}

function ordersTemplate(ready) {
  const rows = orders.filter(order => ready ? order.workStatus === 'READY' : order.workStatus !== 'READY')
  const summary = ready ? [['Today', 4, '₹1,595'], ['Future', 3, '₹1,514'], ['Overdue', 48, '₹24,834'], ['All ready', 55, '₹27,943']] : [['Near delivery', 15, '₹11,653'], ['Future', 6, '₹3,158'], ['Overdue', 15, '₹11,247'], ['All in process', 36, '₹26,058']]
  return `<div class="page-content"><section class="page-heading"><div><span class="eyebrow green">OPERATIONS</span><h1>${ready ? 'Ready for delivery' : 'In-process orders'}</h1><p>${ready ? 'Collect payment, print receipt and hand over safely.' : 'Track washing, ironing and cleaning progress.'}</p></div><button class="primary">Batch update⌄</button></section><section class="status-summary">${summary.map(([label, value, amount], index) => `<article class="${index === 2 ? 'alert' : ''}"><span>${index === 2 ? '!' : ready ? '✓' : '◌'}</span><div><small>${label}</small><strong>${value}</strong><em>${amount}</em></div></article>`).join('')}</section><section class="panel orders-panel"><div class="list-tools"><label class="global-search"><span>⌕</span><input placeholder="Filter loaded orders"></label><div><button class="ghost">Payment status⌄</button><button class="ghost">Staff⌄</button><button class="ghost">Export ↗</button></div></div>${orderTable(rows)}</section></div>`
}

function reportsTemplate() {
  const serviceSales = [
    ['Wash & Iron / kg', '₹1,38,420', '168', '1,204.6 kg', 40], ['Wash & Fold / kg', '₹82,940', '104', '921.5 kg', 24],
    ['Ironing', '₹56,280', '91', '3,642 pcs', 16], ['Dry cleaning', '₹44,760', '36', '298 pcs', 13], ['Shoe & sofa cleaning', '₹19,600', '13', '61 units', 7],
  ]
  const weekly = [['W1', 58, '₹58K'], ['W2', 78, '₹78K'], ['W3', 69, '₹69K'], ['W4', 91, '₹91K'], ['W5', 46, '₹46K']]
  return `<div class="page-content"><section class="page-heading"><div><span class="eyebrow green">SALES INSIGHTS</span><h1>Reports</h1><p>Sales, services, collections and outstanding amounts.</p></div><button class="primary">Export report ↗</button></section>
    <section class="report-toolbar"><div><button>Today</button><button>This week</button><button class="active">This month</button><button>Custom</button></div><span>01 Sep 2026 — 30 Sep 2026</span></section>
    <section class="report-metrics"><article><span>Gross sales</span><strong>₹3,42,000</strong><em>+12.6% vs last month</em></article><article><span>Orders</span><strong>412</strong><em>+38 orders</em></article><article><span>Average bill</span><strong>₹830</strong><em>+₹42</em></article><article><span>Amount collected</span><strong>₹3,06,420</strong><em>89.6% collected</em></article></section>
    <section class="reports-grid"><article class="panel sales-chart"><div class="panel-head"><div><h3>Weekly sales</h3><p>Revenue by week in September</p></div><button class="ghost">Revenue⌄</button></div><div class="bar-chart">${weekly.map(([week, height, value]) => `<div><span>${value}</span><i><em style="height:${height}%"></em></i><b>${week}</b></div>`).join('')}</div></article>
      <article class="panel payment-mix"><div class="panel-head"><div><h3>Payment mix</h3><p>Collected amount by mode</p></div></div><div class="donut"><span><b>₹3.06L</b><small>Collected</small></span></div><ul><li><i class="cash"></i>Cash <b>46%</b></li><li><i class="upi"></i>UPI <b>42%</b></li><li><i class="card"></i>Card <b>12%</b></li></ul></article></section>
    <section class="panel service-report"><div class="panel-head"><div><h3>Service-wise sales</h3><p>Product/service performance for the selected period</p></div><button class="ghost">Download CSV ↗</button></div><div class="table-wrap"><table><thead><tr><th>Service</th><th>Sales</th><th>Orders</th><th>Billable quantity</th><th>Share</th></tr></thead><tbody>${serviceSales.map(([name, sales, count, quantity, share]) => `<tr><td><strong>${name}</strong></td><td><strong>${sales}</strong></td><td>${count}</td><td>${quantity}</td><td><div class="share"><i><em style="width:${share}%"></em></i><b>${share}%</b></div></td></tr>`).join('')}</tbody></table></div></section>
  </div>`
}

function paymentsTemplate() {
  const selected = orders.find(order => order.id === state.paymentOrderId) || orders.find(order => orderBalance(order) > 0) || orders[0]
  const selectedCustomer = selected ? orderCustomer(selected) : null
  const orderRows = orders.slice(0, 12).map(order => {
    const customer = orderCustomer(order)
    return `<button class="payment-order ${selected?.id === order.id ? 'active' : ''}" data-select-payment="${order.id}"><span><b>${escapeHtml(order.id)}</b><small>${escapeHtml(customer?.name || order.customer)} · ${escapeHtml(customer?.phone || '')}</small></span><span><b>${money(order.total)}</b><small>${status(order.payment)}</small></span></button>`
  }).join('')
  return `<div class="page-content"><section class="page-heading"><div><span class="eyebrow green">PAYMENTS & SHARING</span><h1>Collect payment and send bill</h1><p>Test a full or partial payment, then download/share the invoice as an image.</p></div><button class="primary" data-page="new-order">＋ New order</button></section>
    ${state.paymentNotice ? `<div class="payment-notice">${escapeHtml(state.paymentNotice)}</div>` : ''}
    <section class="payments-layout"><article class="panel payment-orders"><div class="panel-head"><div><h3>Recent bills</h3><p>Select the bill you want to test</p></div></div><div class="payment-order-list">${orderRows}</div></article>
      <article class="panel payment-detail">${selected ? `<div class="payment-detail-head"><div><span class="eyebrow green">${escapeHtml(selected.invoiceId || selected.id.replace('SO-', 'INV-'))}</span><h2>${escapeHtml(selected.id)}</h2><p>${escapeHtml(selectedCustomer?.name || selected.customer)} · ${escapeHtml(selectedCustomer?.phone || '')}</p></div>${status(selected.payment)}</div>
        <div class="payment-summary"><div><span>Total</span><strong>${money(selected.total)}</strong></div><div><span>Paid</span><strong>${money(orderPaid(selected))}</strong></div><div><span>Balance</span><strong class="${orderBalance(selected) ? 'danger-text' : ''}">${money(orderBalance(selected))}</strong></div></div>
        <form id="payment-form"><input type="hidden" name="orderId" value="${escapeHtml(selected.id)}"><div class="payment-fields"><label>Payment mode<select name="mode"><option>Cash</option><option>UPI</option><option>Card</option><option>Bank transfer</option></select></label><label>Amount<input name="amount" type="number" min="0.01" step="0.01" max="${orderBalance(selected)}" value="${orderBalance(selected)}" ${orderBalance(selected) === 0 ? 'disabled' : ''}></label><label>Transaction / note<input name="reference" maxlength="60" placeholder="Optional"></label></div><button class="primary wide" type="submit" ${orderBalance(selected) === 0 ? 'disabled' : ''}>Record payment</button></form>
        <div class="invoice-actions"><button class="secondary" data-download-invoice="${escapeHtml(selected.id)}">↓ Download invoice PNG</button><button class="whatsapp-button" data-whatsapp-invoice="${escapeHtml(selected.id)}">WhatsApp invoice image →</button></div>
        <div class="offline-share-note"><b>How this offline test works</b><p>On a compatible phone, the system share sheet can include the PNG. On desktop, the PNG downloads and WhatsApp opens with the bill text; attach that PNG before sending. Fully automatic media delivery needs the official WhatsApp Business connection.</p></div>` : '<div class="empty-state"><span>₹</span><h3>No bills yet</h3><p>Create a bill first.</p></div>'}</article>
    </section></div>`
}

function placeholderTemplate() {
  const label = { pickups: 'Pickup & delivery' }[state.page]
  return `<div class="page-content"><section class="page-heading"><div><span class="eyebrow green">COMING NEXT</span><h1>${label}</h1><p>This module is included in the agreed system architecture.</p></div></section><section class="panel empty-state"><span>◇</span><h3>${label} foundation is ready</h3><p>Detailed workflow implementation follows the billing and order-management approval.</p></section></div>`
}

function render() {
  if (!state.loggedIn) {
    root.innerHTML = loginTemplate()
    return
  }
  let page = dashboardTemplate()
  if (state.page === 'new-order') page = newOrderTemplate()
  if (state.page === 'customers') page = customersTemplate()
  if (state.page === 'in-process') page = ordersTemplate(false)
  if (state.page === 'ready') page = ordersTemplate(true)
  if (state.page === 'payments') page = paymentsTemplate()
  if (state.page === 'reports') page = reportsTemplate()
  if (state.page === 'pickups') page = placeholderTemplate()
  root.innerHTML = `<div class="app-shell">${sidebarTemplate()}<div class="main-shell">${topbarTemplate()}${page}</div></div>${customerModalTemplate()}`
}

function createOrderFromCart() {
  if (state.finalising) return
  const customer = customers.find(item => item.id === state.customerId)
  const billableItems = state.cart.filter(item => Number(item.quantity) > 0)
  if (!customer) {
    state.notice = 'Create or select a customer before making the bill.'
    render()
    return
  }
  if (!billableItems.length) {
    state.notice = 'Select at least one item before making the bill.'
    render()
    return
  }
  state.finalising = true
  const id = nextOrderNumber()
  const total = Math.round(billableItems.reduce((sum, item) => sum + Number(item.rate) * Number(item.quantity), 0))
  const pieces = billableItems.reduce((sum, item) => sum + Number(item.pieces || 0), 0)
  const itemSnapshots = billableItems.map(item => ({
    id: item.id,
    name: item.name,
    unit: item.unit,
    rate: Number(item.rate),
    quantity: Number(item.quantity),
    pieces: Number(item.pieces || 0),
    lineTotal: Number(item.rate) * Number(item.quantity),
  }))
  const order = {
    id,
    invoiceId: id.replace('SO-', 'INV-'),
    customerId: customer.id,
    customer: customer.name,
    service: `${itemSnapshots.map(item => item.name).slice(0, 2).join(' + ')}${itemSnapshots.length > 2 ? ` +${itemSnapshots.length - 2} more` : ''} · ${pieces} pcs`,
    items: itemSnapshots,
    delivery: `${state.deliveryDate} · ${state.deliveryTime}`,
    deliveryDate: state.deliveryDate,
    deliveryTime: state.deliveryTime,
    notes: state.orderNotes,
    autoWhatsApp: state.autoWhatsApp,
    workStatus: 'RECEIVED',
    payment: 'UNPAID',
    paid: 0,
    total,
    staff: 'Owner',
    createdAt: new Date().toISOString(),
    payments: [],
  }
  orders.unshift(order)
  customer.openOrders = Number(customer.openOrders || 0) + 1
  customer.due = Number(customer.due || 0) + total
  savePrototypeData()
  state.paymentOrderId = id
  state.paymentNotice = `${id} created once for ${customer.name}. Record the payment, then test the WhatsApp invoice image.`
  state.cart = []
  state.orderNotes = ''
  state.finalising = false
  state.page = 'payments'
  render()
}

root.addEventListener('submit', event => {
  if (event.target.id === 'login-form') { event.preventDefault(); state.loggedIn = true; render(); return }
  if (event.target.id === 'customer-form') {
    event.preventDefault()
    const form = new FormData(event.target)
    const name = String(form.get('name') || '').trim()
    const phone = String(form.get('phone') || '').trim()
    const area = String(form.get('area') || '').trim()
    const digits = phoneDigits(phone)
    if (name.length < 2) state.customerError = 'Enter the customer name.'
    else if (digits.length < 10 || digits.length > 15) state.customerError = 'Enter a valid 10-digit WhatsApp mobile number.'
    else if (customers.some(customer => phoneDigits(customer.phone) === digits)) state.customerError = 'This phone number already belongs to a customer.'
    else {
      const id = customers.reduce((max, customer) => Math.max(max, Number(customer.id) || 0), 0) + 1
      customers.unshift({ id, name, phone, area, openOrders: 0, due: 0 })
      savePrototypeData()
      state.customerId = id
      state.cart = []
      state.customerModalOpen = false
      state.customerError = ''
      state.quickOpen = false
      state.page = 'new-order'
      state.notice = `${name} was created and is selected for this bill.`
    }
    render()
    return
  }
  if (event.target.id === 'payment-form') {
    event.preventDefault()
    const form = new FormData(event.target)
    const order = orders.find(item => item.id === form.get('orderId'))
    const amount = Number(form.get('amount'))
    if (!order || !Number.isFinite(amount) || amount <= 0) {
      state.paymentNotice = 'Enter a valid payment amount.'
      render()
      return
    }
    const acceptedAmount = Math.min(amount, orderBalance(order))
    if (acceptedAmount <= 0) {
      state.paymentNotice = 'This bill is already fully paid.'
      render()
      return
    }
    order.paid = Math.round((orderPaid(order) + acceptedAmount) * 100) / 100
    order.payment = orderBalance(order) === 0 ? 'PAID' : 'PARTIAL'
    order.paymentMode = String(form.get('mode') || 'Cash')
    order.paymentReference = String(form.get('reference') || '').trim()
    order.payments = [...(order.payments || []), { amount: acceptedAmount, mode: order.paymentMode, reference: order.paymentReference, createdAt: new Date().toISOString() }]
    const customer = orderCustomer(order)
    if (customer) customer.due = Math.max(0, Math.round((Number(customer.due || 0) - acceptedAmount) * 100) / 100)
    savePrototypeData()
    state.paymentNotice = `${money(acceptedAmount)} recorded for ${order.id}. The invoice image now includes paid and balance details.`
    render()
  }
})

root.addEventListener('click', event => {
  const pageButton = event.target.closest('[data-page]')
  if (pageButton) { state.page = pageButton.dataset.page; state.notice = ''; state.quickOpen = false; render(); return }
  if (event.target.closest('#logout')) { state.loggedIn = false; state.page = 'dashboard'; render(); return }
  if (event.target.closest('#quick-actions')) { state.quickOpen = !state.quickOpen; render(); return }
  if (event.target.closest('#open-customer-modal') || event.target.closest('#order-add-customer') || event.target.closest('#quick-create-customer')) { state.customerModalOpen = true; state.customerError = ''; state.quickOpen = false; render(); return }
  if (event.target.closest('#close-customer-modal') || event.target.closest('#cancel-customer') || event.target.id === 'customer-modal-backdrop') { state.customerModalOpen = false; state.customerError = ''; render(); return }
  if (event.target.closest('[data-close-quick]')) { state.quickOpen = false; render(); return }
  const startOrderButton = event.target.closest('[data-start-order]')
  if (startOrderButton) { state.customerId = Number(startOrderButton.dataset.startOrder); state.page = 'new-order'; state.cart = []; state.notice = 'Customer selected. Choose items for the new bill.'; render(); return }
  const categoryButton = event.target.closest('[data-category]')
  if (categoryButton) { state.category = categoryButton.dataset.category; state.serviceGroup = 'All'; render(); return }
  const groupButton = event.target.closest('[data-service-group]')
  if (groupButton) { state.serviceGroup = groupButton.dataset.serviceGroup; render(); return }
  const serviceButton = event.target.closest('[data-service]')
  if (serviceButton) {
    const service = services.find(item => item.id === Number(serviceButton.dataset.service))
    const existing = state.cart.find(item => item.id === service.id)
    if (existing) { existing.quantity += 1; existing.pieces += 1 } else { state.cart.push({ ...service, quantity: 1, pieces: 1 }) }
    render(); return
  }
  const removeButton = event.target.closest('[data-remove]')
  if (removeButton) { state.cart = state.cart.filter(item => item.id !== Number(removeButton.dataset.remove)); render(); return }
  if (event.target.closest('#toggle-custom')) { state.customOpen = !state.customOpen; render(); return }
  if (event.target.closest('#close-custom')) { state.customOpen = false; render(); return }
  if (event.target.closest('#clear-cart')) { state.cart = []; state.notice = ''; render(); return }
  if (event.target.closest('#finalise')) { createOrderFromCart(); return }
  const selectPaymentButton = event.target.closest('[data-select-payment]')
  if (selectPaymentButton) { state.paymentOrderId = selectPaymentButton.dataset.selectPayment; state.paymentNotice = ''; render(); return }
  const downloadButton = event.target.closest('[data-download-invoice]')
  if (downloadButton) {
    const order = orders.find(item => item.id === downloadButton.dataset.downloadInvoice)
    if (order) { downloadInvoiceImage(order); state.paymentNotice = `${invoiceFilename(order)} downloaded.`; render() }
    return
  }
  const whatsAppButton = event.target.closest('[data-whatsapp-invoice]')
  if (whatsAppButton) {
    const order = orders.find(item => item.id === whatsAppButton.dataset.whatsappInvoice)
    if (order) shareInvoiceToWhatsApp(order)
  }
})

root.addEventListener('change', event => {
  if (event.target.id === 'customer-select') { state.customerId = Number(event.target.value); render(); return }
  if (event.target.id === 'delivery-date') { state.deliveryDate = event.target.value; return }
  if (event.target.id === 'delivery-time') { state.deliveryTime = event.target.value; return }
  if (event.target.id === 'auto-whatsapp') { state.autoWhatsApp = event.target.checked; return }
  if (event.target.matches('[data-item]')) {
    const item = state.cart.find(entry => entry.id === Number(event.target.dataset.item))
    item[event.target.dataset.field] = Math.max(0, Number(event.target.value)); render()
  }
})

root.addEventListener('input', event => {
  if (event.target.id === 'order-notes') { state.orderNotes = event.target.value; return }
  if (event.target.id === 'customer-query') { state.customerQuery = event.target.value; customersTemplate(); const cursor = event.target.selectionStart; render(); const next = document.querySelector('#customer-query'); next.focus(); next.setSelectionRange(cursor, cursor) }
})

render()
