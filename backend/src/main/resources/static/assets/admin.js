'use strict';
document.querySelectorAll('.sidebar nav a').forEach(a => {
  if (a.getAttribute('href') === location.pathname) a.classList.add('current');
});
document.querySelectorAll('[data-print]').forEach(button => button.addEventListener('click', () => window.print()));
document.querySelectorAll('[data-lock-submit]').forEach(form => {
  form.addEventListener('submit', event => {
    if (form.dataset.submitting === 'true') { event.preventDefault(); return; }
    form.dataset.submitting = 'true';
    form.querySelectorAll('button[type="submit"]').forEach(button => { button.disabled = true; });
  });
});
// Restore submission controls after browser Back/bfcache without changing request IDs.
window.addEventListener('pageshow', () => {
  document.querySelectorAll('[data-lock-submit]').forEach(form => {
    if (form.dataset.submitting === 'true') {
      delete form.dataset.submitting;
      form.querySelectorAll('button[type="submit"]').forEach(button => { button.disabled = false; });
    }
  });
});
const orderForm = document.getElementById('order-form');
if (orderForm) {
  const body = document.getElementById('line-items');
  const format = value => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value);
  const reindex = () => {
    body.querySelectorAll('.order-line').forEach((row, index) => {
      row.querySelectorAll('[name]').forEach(field => {
        field.name = field.name.replace(/items\[\d+\]/, `items[${index}]`);
        field.removeAttribute('id');
      });
    });
  };
  const recalculate = () => {
    let cents = 0;
    body.querySelectorAll('.order-line').forEach(row => {
      const option = row.querySelector('[data-service]').selectedOptions[0];
      const quantity = Number(row.querySelector('[data-quantity]').value) || 0;
      const lineCents = Math.round((Number(option?.dataset.rate) || 0) * quantity * 100 + 1e-8);
      cents += lineCents;
      row.querySelector('.line-total').textContent = format(lineCents / 100);
    });
    const discount = Number(orderForm.elements.discount.value) || 0;
    const tax = Number(orderForm.elements.tax.value) || 0;
    document.getElementById('estimated-total').textContent = format(Math.round(cents / 100 - discount + tax));
  };
  document.getElementById('add-line').addEventListener('click', () => {
    if (body.children.length >= 50) return;
    const row = body.firstElementChild.cloneNode(true);
    row.querySelector('[data-service]').value = '';
    row.querySelector('[data-quantity]').value = '1';
    row.querySelector('[data-pieces]').value = '1';
    body.append(row); reindex(); recalculate();
  });
  body.addEventListener('click', event => {
    const button = event.target.closest('.remove-line');
    if (button && body.children.length > 1) { button.closest('tr').remove(); reindex(); recalculate(); }
  });
  orderForm.addEventListener('input', recalculate);
  orderForm.addEventListener('change', recalculate);
  recalculate();
}
