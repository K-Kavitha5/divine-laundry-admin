'use strict';
document.querySelectorAll('.sidebar nav a').forEach(a => {
  if (a.getAttribute('href') === location.pathname) a.classList.add('current');
});
document.querySelectorAll('[data-print]').forEach(button => button.addEventListener('click', () => window.print()));
document.querySelectorAll('[data-lock-submit]').forEach(form => {
  form.addEventListener('submit', event => {
    const message = form.dataset.confirm;
    if (message && !window.confirm(message)) event.preventDefault();
  });
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
  const imageByCode = {
    SHIRT_DRY: 'shirt.svg', TSHIRT_DRY_M: 'tshirt.svg', TSHIRT_DRY_W: 'tshirt.svg', TSHIRT_DRY_K: 'tshirt.svg',
    VEST_DRY_M: 'vest.svg', UNDERWEAR_DRY_M: 'underwear.svg', PULLOVER_DRY_M: 'pullover.svg', PULLOVER_DRY_W: 'pullover.svg', PULLOVER_DRY_K: 'pullover.svg',
    SWEATPANT_DRY_M: 'sweat-pants.svg', CAPRI_DRY_M: 'capri.svg', CAPRI_DRY_W: 'capri.svg', CAPRI_DRY_K: 'capri.svg',
    PYJAMA_DRY_M: 'pyjama.svg', TRACKPANT_DRY_M: 'track-pant.svg', TRACKPANT_DRY_W: 'track-pant.svg', TRACKPANT_DRY_K: 'track-pant.svg',
    SHORTS_DRY_M: 'shorts.svg', SHORTS_DRY_K: 'shorts.svg', LONGCOAT_DRY_M: 'long-coat.svg', LONGCOAT_DRY_W: 'long-coat.svg',
    BLAZER_DRY_M: 'blazer.svg', BLAZER_DRY_W: 'blazer.svg', JEANS_DRY_M: 'jeans.svg', JEANS_DRY_W: 'jeans.svg', JEANS_DRY_K: 'jeans.svg',
    TIE_DRY_M: 'tie.svg', TIE_DRY_A: 'tie.svg', SHERWANI_DRY_M: 'sherwani.svg', SHERWANI_DRY_K: 'sherwani.svg',
    ACHKAN_DRY_M: 'blazer.svg', DANGREE_DRY_W: 'dress.svg', DANGREE_DRY_K: 'dress.svg', JUMPER_DRY_W: 'sweater.svg', JUMPER_DRY_K: 'sweater.svg',
    HANDBAG_DRY_H: 'bag.svg', BABYBLANKET_DRY_K: 'blanket.svg',
    SHIRT_IRON: 'shirt.svg', TSHIRT_IRON: 'tshirt.svg', PANT_IRON: 'trousers.svg', LONG_DRESS_IRON: 'dress.svg',
    PILLOW_COVER_IRON: 'bedsheet.svg', COAT_BLAZER_IRON: 'blazer.svg', OVERCOAT_IRON: 'long-coat.svg',
    TABLERUNNER_DRY_H: 'bedsheet.svg', FOOTMAT_DRY_H: 'bedsheet.svg', TABLEMAT_DRY_H: 'bedsheet.svg', BATHROBE_DRY_H: 'blanket.svg',
    SOCKS_DRY_H: 'trousers.svg', HANDKERCHIEF_DRY_A: 'default-laundry.svg', RAINCOAT_DRY_A: 'long-coat.svg',
    WASH_IRON_KG: 'washing.svg', WASH_FOLD_KG: 'washing.svg', WASH_FOLD_EXPRESS_KG: 'washing.svg', WASH_IRON_EXPRESS_KG: 'washing.svg',
    SPORT_SHOE: 'shoe.svg', CANVAS_SHOE: 'shoe.svg', LEATHER_SHOE: 'shoe.svg', SUEDE_SHOE: 'shoe.svg', CROCS_SANDALS: 'shoe.svg', SLIPPERS: 'shoe.svg',
    SOFA_SEAT: 'sofa.svg'
  };
  const imageFor = (name = '', category = '', code = '') => {
    const value = `${name} ${category} ${code}`.toLowerCase();
    if (imageByCode[code]) return `/assets/service-images/${imageByCode[code]}`;
    if (value.includes('sofa') || value.includes('upholstery')) return '/assets/service-images/sofa.svg';
    if (value.includes('shoe')) return '/assets/service-images/shoe.svg';
    if (value.includes('wash') || value.includes('kg')) return '/assets/service-images/washing.svg';
    if (value.includes('iron')) return '/assets/service-images/iron.svg';
    if (value.includes('blanket')) return '/assets/service-images/blanket.svg';
    if (value.includes('curtain')) return '/assets/service-images/curtain.svg';
    if (value.includes('bag') || value.includes('handbag')) return '/assets/service-images/bag.svg';
    if (value.includes('jean')) return '/assets/service-images/jeans.svg';
    if (value.includes('blazer') || value.includes('coat') || value.includes('achkan') || value.includes('sherwani')) return '/assets/service-images/blazer.svg';
    if (value.includes('bed') || value.includes('bedsheet') || value.includes('duvet') || value.includes('quilt')) return '/assets/service-images/bedsheet.svg';
    if (value.includes('saree') || value.includes('silk') || value.includes('dress')) return '/assets/service-images/saree.svg';
    if (value.includes('t-shirt') || value.includes('tshirt')) return '/assets/service-images/tshirt.svg';
    if (value.includes('pant') || value.includes('trouser')) return '/assets/service-images/trousers.svg';
    if (value.includes('shirt') || value.includes('vest') || value.includes('pullover') || value.includes('jumper')) return '/assets/service-images/shirt.svg';
    return '/assets/service-images/default-laundry.svg';
  };
  const catalog = document.getElementById('service-catalog');
  const dryCatalog = document.getElementById('dry-service-catalog');
  const directCatalog = document.getElementById('direct-service-catalog');
  const search = document.getElementById('service-search');
  const catalogCount = document.getElementById('catalog-count');
  const catalogEmpty = document.getElementById('catalog-empty');
  let selectedCategory = 'Dry Clean';
  let selectedGroup = 'All';
  const categoryCopy = {
    'Dry Clean': ['DRY CLEAN', 'Every garment, handled with care.', 'Choose a group or search to find a service quickly.', '/assets/service-images/shirt.svg'],
    'Laundry by KG': ['LAUNDRY BY KG', 'Fresh & clean always.', 'Wash, fold, and express care priced by the kilogram.', '/assets/service-images/washing.svg'],
    Ironing: ['IRONING', 'Pressed to perfection.', 'Crisp finishing for everyday wear and special pieces.', '/assets/service-images/iron.svg'],
    'Shoe Cleaning': ['SHOE CLEANING', 'Step out fresh.', 'Specialist care for every pair.', '/assets/service-images/shoe.svg'],
    'Sofa Cleaning': ['SOFA CLEANING', 'A cleaner place to relax.', 'Refresh upholstery and make every seat feel new.', '/assets/service-images/sofa.svg']
  };
  const renderCatalog = () => {
    const query = search.value.trim().toLowerCase();
    let visible = 0;
    const isDryClean = selectedCategory === 'Dry Clean';
    dryCatalog.hidden = !isDryClean;
    directCatalog.hidden = isDryClean;
    dryCatalog.querySelectorAll('.service-group').forEach(group => {
      let groupVisible = 0;
      group.querySelectorAll('.service-card').forEach(card => {
        const matchesGroup = selectedGroup === 'All' || card.dataset.serviceGroup === selectedGroup;
        const text = `${card.dataset.serviceName} ${card.dataset.serviceGroup || ''} ${card.dataset.serviceCategory}`.toLowerCase();
        const matchesSearch = !query || text.includes(query);
        const show = matchesGroup && matchesSearch;
        card.hidden = !show;
        if (show) { groupVisible += 1; visible += 1; }
      });
      group.hidden = groupVisible === 0;
    });
    directCatalog.querySelectorAll('.service-card').forEach(card => {
      const text = `${card.dataset.serviceName} ${card.dataset.serviceCategory}`.toLowerCase();
      const show = card.dataset.serviceCategory === selectedCategory && (!query || text.includes(query));
      card.hidden = !show;
      if (show) visible += 1;
    });
    catalogCount.textContent = `${visible} service${visible === 1 ? '' : 's'}`;
    catalogEmpty.hidden = visible !== 0;
    document.getElementById('dry-filter-bar').hidden = selectedCategory !== 'Dry Clean';
    const copy = categoryCopy[selectedCategory];
    document.getElementById('catalog-banner-label').textContent = copy[0];
    document.getElementById('catalog-banner-title').textContent = copy[1];
    document.getElementById('catalog-banner-copy').textContent = copy[2];
    document.getElementById('catalog-banner-image').src = copy[3];
  };
  const syncLine = row => {
    const select = row.querySelector('[data-service]');
    const option = select?.selectedOptions[0];
    const name = option?.dataset.name || '';
    row.querySelector('[data-line-name]').textContent = name || 'Select a service';
    row.querySelector('[data-line-meta]').textContent = name ? `${option.dataset.category} · ₹${option.dataset.rate} / ${option.dataset.unit}` : 'Choose from the service menu';
    const fallback = '/assets/service-images/default-laundry.svg';
    const lineImage = row.querySelector('[data-line-image]');
    lineImage.onerror = () => { lineImage.onerror = null; lineImage.src = fallback; };
    lineImage.src = imageFor(name, option?.dataset.category, option?.dataset.code);
    row.classList.toggle('has-service', Boolean(name));
  };
  const syncCardQuantities = () => {
    const quantities = new Map();
    body.querySelectorAll('.order-line').forEach(row => {
      const serviceId = row.querySelector('[data-service]').value;
      if (serviceId) quantities.set(serviceId, Number(row.querySelector('[data-quantity]').value) || 0);
    });
    catalog.querySelectorAll('.service-card').forEach(card => {
      card.querySelector('[data-card-quantity]').textContent = quantities.get(card.dataset.serviceId) || 0;
    });
  };
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
      syncLine(row);
    });
    const discount = Number(orderForm.elements.discount.value) || 0;
    const tax = Number(orderForm.elements.tax.value) || 0;
    document.getElementById('estimated-total').textContent = format(Math.round(cents / 100 - discount + tax));
    document.getElementById('empty-ticket').hidden = body.children.length > 0 && [...body.children].some(row => row.querySelector('[data-service]').value);
    syncCardQuantities();
  };
  const selectService = card => {
    const existing = [...body.querySelectorAll('.order-line')].find(item => item.querySelector('[data-service]').value === card.dataset.serviceId);
    if (existing) {
      const quantity = existing.querySelector('[data-quantity]');
      const pieces = existing.querySelector('[data-pieces]');
      quantity.value = (Number(quantity.value) || 0) + 1;
      pieces.value = Math.min(500, (Number(pieces.value) || 0) + 1);
      recalculate();
      existing.classList.add('line-pulse');
      window.setTimeout(() => existing.classList.remove('line-pulse'), 350);
      return;
    }
    let row = [...body.querySelectorAll('.order-line')].find(item => !item.querySelector('[data-service]').value);
    if (!row) row = addLine();
    if (!row) return;
    const select = row.querySelector('[data-service]');
    select.value = card.dataset.serviceId;
    row.querySelector('[data-quantity]').value = '1';
    row.querySelector('[data-pieces]').value = '1';
    select.dispatchEvent(new Event('change', { bubbles: true }));
    row.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  };
  const addLine = () => {
    if (body.children.length >= 50) return null;
    const row = body.firstElementChild.cloneNode(true);
    row.querySelector('[data-service]').value = '';
    row.querySelector('[data-quantity]').value = '1';
    row.querySelector('[data-pieces]').value = '1';
    body.append(row); reindex(); syncLine(row); return row;
  };
  document.querySelectorAll('.service-card').forEach(card => card.addEventListener('click', () => selectService(card)));
  document.querySelectorAll('.primary-service').forEach(button => button.addEventListener('click', () => {
    selectedCategory = button.dataset.topCategory;
    selectedGroup = 'All';
    document.querySelectorAll('.primary-service').forEach(item => item.classList.toggle('active', item === button));
    document.querySelectorAll('.dry-filter').forEach(item => item.classList.toggle('active', item.dataset.dryGroup === 'All'));
    search.value = '';
    renderCatalog();
  }));
  document.querySelectorAll('.dry-filter').forEach(button => button.addEventListener('click', () => {
    selectedGroup = button.dataset.dryGroup;
    document.querySelectorAll('.dry-filter').forEach(item => item.classList.toggle('active', item === button));
    renderCatalog();
  }));
  search.addEventListener('input', renderCatalog);
  document.getElementById('add-line').addEventListener('click', () => {
    addLine(); recalculate();
  });
  body.addEventListener('click', event => {
    const button = event.target.closest('.remove-line');
    if (button && body.children.length > 1) { button.closest('.order-line').remove(); reindex(); recalculate(); }
  });
  orderForm.addEventListener('input', recalculate);
  orderForm.addEventListener('change', recalculate);
  document.querySelectorAll('[data-service-image]').forEach(image => {
    const card = image.closest('.service-card');
    image.onerror = () => { image.onerror = null; image.src = '/assets/service-images/default-laundry.svg'; };
    image.src = imageFor(card.dataset.serviceName, `${card.dataset.serviceCategory} ${card.dataset.serviceGroup || ''}`, card.dataset.serviceCode);
  });
  renderCatalog();
  recalculate();
}
