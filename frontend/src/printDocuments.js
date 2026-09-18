const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll("'", '&#039;')
}

function dateTime(value) {
  if (!value) return 'Not set'
  return new Date(value).toLocaleString('en-IN', {
    dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata',
  })
}

function openPrintWindow(title, body, pageCss = '') {
  const popup = window.open('', '_blank', 'width=980,height=900')
  if (!popup) throw new Error('Allow pop-ups in the browser to print this document.')
  popup.document.open()
  popup.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(title)}</title><style>
    *{box-sizing:border-box}body{margin:0;padding:28px;color:#17231d;font-family:Arial,sans-serif}h1,h2,h3,p{margin:0}
    .head{display:flex;justify-content:space-between;gap:24px;padding-bottom:18px;border-bottom:2px solid #0f8a55}.brand h1{color:#075938;font-size:25px}.brand p,.muted{margin-top:5px;color:#66756d;font-size:12px;line-height:1.5}.doc{text-align:right}.doc h2{font-size:20px}.doc b{display:block;margin-top:6px;color:#075938}
    .customer{display:grid;grid-template-columns:1fr 1fr;gap:18px;margin:20px 0;padding:14px;background:#f3f8f5;border-radius:9px}.customer b{display:block;margin-bottom:5px;font-size:12px}.customer span{font-size:12px;line-height:1.5}
    table{width:100%;border-collapse:collapse;margin-top:14px;font-size:12px}th{padding:10px 8px;text-align:left;background:#075938;color:#fff}td{padding:10px 8px;border-bottom:1px solid #dfe7e2}th:last-child,td:last-child{text-align:right}.totals{width:330px;margin:18px 0 0 auto}.totals div{display:flex;justify-content:space-between;padding:6px 0;font-size:12px}.totals .grand{margin-top:5px;padding-top:10px;border-top:2px solid #17231d;font-size:17px}.paid{color:#075938}.balance{color:#b64141}.foot{margin-top:28px;padding-top:15px;border-top:1px dashed #aebbb4;text-align:center;color:#68766f;font-size:11px}
    .tag-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}.tag{min-height:118px;padding:10px;border:1.5px dashed #1c2b23;border-radius:6px;page-break-inside:avoid}.tag .shop{font-size:10px;font-weight:bold;color:#075938}.tag h3{margin:7px 0 4px;font-size:14px}.tag .code{font-family:monospace;font-size:12px;font-weight:bold}.tag small{display:block;margin-top:5px;color:#66756d}.tag .piece{float:right;padding:3px 6px;background:#e2f3ea;border-radius:10px;font-size:10px;font-weight:bold}
    ${pageCss}@media print{body{padding:10mm}.no-print{display:none!important}}
  </style></head><body>${body}</body></html>`)
  popup.document.close()
  popup.focus()
  setTimeout(() => popup.print(), 250)
}

export function printInvoice(documentData) {
  const rows = documentData.items.map(item => `<tr><td>${escapeHtml(item.serviceName)}</td><td>${escapeHtml(item.unit)}</td><td>${Number(item.quantity).toLocaleString('en-IN')}</td><td>${item.pieces}</td><td>${money.format(Number(item.rate))}</td><td>${money.format(Number(item.lineTotal))}</td></tr>`).join('')
  const business = documentData.business
  const body = `<section class="head"><div class="brand"><h1>${escapeHtml(business.name)}</h1><p>${escapeHtml(business.address)}<br>${escapeHtml(business.phone)}${business.gstNumber ? `<br>GST: ${escapeHtml(business.gstNumber)}` : ''}</p></div><div class="doc"><h2>INVOICE</h2><b>${escapeHtml(documentData.invoiceNumber)}</b><p class="muted">Order: ${escapeHtml(documentData.orderNumber)}<br>${dateTime(documentData.placedAt)}</p></div></section>
    <section class="customer"><div><b>BILL TO</b><span>${escapeHtml(documentData.customer.name)}<br>${escapeHtml(documentData.customer.phone)}<br>${escapeHtml(documentData.customer.address)}</span></div><div><b>DELIVERY</b><span>${dateTime(documentData.deliveryAt)}<br>Payment: ${escapeHtml(documentData.paymentStatus)}${documentData.notes ? `<br>Note: ${escapeHtml(documentData.notes)}` : ''}</span></div></section>
    <table><thead><tr><th>Service</th><th>Unit</th><th>Qty/Weight</th><th>Pieces</th><th>Rate</th><th>Amount</th></tr></thead><tbody>${rows}</tbody></table>
    <section class="totals"><div><span>Subtotal</span><b>${money.format(Number(documentData.subtotal))}</b></div><div><span>Discount</span><b>${money.format(Number(documentData.discount))}</b></div><div><span>Tax</span><b>${money.format(Number(documentData.tax))}</b></div><div><span>Round off</span><b>${money.format(Number(documentData.roundOff))}</b></div><div class="grand"><span>Total</span><b>${money.format(Number(documentData.total))}</b></div><div class="paid"><span>Paid</span><b>${money.format(Number(documentData.amountPaid))}</b></div><div class="balance"><span>Balance</span><b>${money.format(Number(documentData.balance))}</b></div></section>
    <footer class="foot">Thank you for choosing ${escapeHtml(business.name)}.</footer>`
  openPrintWindow(`Invoice ${documentData.invoiceNumber}`, body)
}

export function printGarmentTags(documentData) {
  if (!documentData.tags.length) throw new Error('This order has no printable garment tags.')
  const tags = documentData.tags.map(tag => `<article class="tag"><span class="piece">Piece ${tag.pieceSequence}</span><div class="shop">${escapeHtml(documentData.business.name)}</div><h3>${escapeHtml(tag.serviceName)}</h3><div class="code">${escapeHtml(tag.tagNumber)}</div><small>${escapeHtml(documentData.customer.name)} · ${escapeHtml(documentData.orderNumber)}</small><small>Delivery: ${dateTime(documentData.deliveryAt)}</small></article>`).join('')
  openPrintWindow(`Tags ${documentData.orderNumber}`, `<main class="tag-grid">${tags}</main>`, '@page{size:A4;margin:8mm}')
}

function canvasText(context, text, x, y, maxWidth) {
  let value = String(text ?? '')
  while (context.measureText(value).width > maxWidth && value.length > 3) value = `${value.slice(0, -2)}…`
  context.fillText(value, x, y)
}

export function downloadInvoiceImage(documentData) {
  const width = 900
  const height = 570 + documentData.items.length * 54
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, width, height)
  context.fillStyle = '#075938'
  context.fillRect(0, 0, width, 120)
  context.fillStyle = '#ffffff'
  context.font = 'bold 34px Arial'
  context.fillText(documentData.business.name, 42, 55)
  context.font = '18px Arial'
  context.fillText(`INVOICE ${documentData.invoiceNumber}`, 42, 90)
  context.textAlign = 'right'
  context.fillText(`Order ${documentData.orderNumber}`, width - 42, 58)
  context.fillText(dateTime(documentData.placedAt), width - 42, 90)
  context.textAlign = 'left'
  context.fillStyle = '#17231d'
  context.font = 'bold 20px Arial'
  context.fillText(documentData.customer.name, 42, 164)
  context.font = '16px Arial'
  context.fillStyle = '#5f6f66'
  canvasText(context, `${documentData.customer.phone} · ${documentData.customer.address || 'Address not set'}`, 42, 190, 810)
  let y = 235
  context.fillStyle = '#eff7f3'
  context.fillRect(32, y - 27, width - 64, 42)
  context.fillStyle = '#075938'
  context.font = 'bold 15px Arial'
  context.fillText('SERVICE', 48, y)
  context.fillText('QTY', 470, y)
  context.fillText('PCS', 575, y)
  context.textAlign = 'right'
  context.fillText('AMOUNT', width - 48, y)
  context.textAlign = 'left'
  documentData.items.forEach(item => {
    y += 54
    context.fillStyle = '#17231d'
    context.font = '16px Arial'
    canvasText(context, item.serviceName, 48, y, 380)
    context.fillText(Number(item.quantity).toLocaleString('en-IN'), 470, y)
    context.fillText(String(item.pieces), 575, y)
    context.textAlign = 'right'
    context.fillText(money.format(Number(item.lineTotal)), width - 48, y)
    context.textAlign = 'left'
    context.strokeStyle = '#e1e8e4'
    context.beginPath(); context.moveTo(42, y + 18); context.lineTo(width - 42, y + 18); context.stroke()
  })
  y += 65
  context.fillStyle = '#17231d'
  context.font = 'bold 20px Arial'
  context.fillText('Total', 520, y)
  context.textAlign = 'right'
  context.fillText(money.format(Number(documentData.total)), width - 48, y)
  y += 36
  context.font = '17px Arial'
  context.fillStyle = '#0f8a55'
  context.fillText(`Paid ${money.format(Number(documentData.amountPaid))}`, width - 48, y)
  y += 32
  context.fillStyle = Number(documentData.balance) > 0 ? '#b64141' : '#0f8a55'
  context.fillText(`Balance ${money.format(Number(documentData.balance))}`, width - 48, y)
  context.textAlign = 'left'
  context.fillStyle = '#66756d'
  context.font = '15px Arial'
  context.fillText('Thank you. Please retain this invoice image for your records.', 42, height - 42)
  canvas.toBlob(blob => {
    if (!blob) return
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `${documentData.invoiceNumber}.png`
    link.click()
    setTimeout(() => URL.revokeObjectURL(link.href), 1000)
  }, 'image/png')
}
