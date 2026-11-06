// Small enhancements on top of htmx: toasts, the add/edit drawer, live timers,
// the money input mask, menus and keyboard shortcuts.
(() => {
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
  const lang = document.documentElement.lang || 'en';
  const msg = key => ($('#i18n')?.dataset ?? {})[key] ?? '';

  /* ---------- Toasts ---------- */
  function dismiss(toast) {
    if (!toast || toast.classList.contains('leaving')) return;
    toast.classList.add('leaving');
    setTimeout(() => toast.remove(), 200);
  }

  function arm(toast) {
    if (toast.dataset.armed) return;
    toast.dataset.armed = '1';
    const sticky = toast.classList.contains('toast-error') || toast.dataset.undo === 'true';
    const delay = sticky ? 8000 : 4500;
    let timer = setTimeout(() => dismiss(toast), delay);
    toast.addEventListener('mouseenter', () => clearTimeout(timer));
    toast.addEventListener('mouseleave', () => { timer = setTimeout(() => dismiss(toast), 2500); });
  }

  function showToast(type, text) {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.setAttribute('role', type === 'error' ? 'alert' : 'status');
    const span = document.createElement('span');
    span.className = 'toast-msg';
    span.textContent = text;
    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'toast-close';
    close.dataset.dismiss = '';
    close.setAttribute('aria-label', msg('close'));
    close.textContent = '×';
    toast.append(span, close);
    $('#toasts').append(toast);
    arm(toast);
  }

  document.addEventListener('click', e => {
    const close = e.target.closest('[data-dismiss]');
    if (close) dismiss(close.closest('.toast'));
    const undo = e.target.closest('.toast-action');
    if (undo) setTimeout(() => dismiss(undo.closest('.toast')), 150);
  });

  /* ---------- Drawer ---------- */
  const drawer = () => $('#drawer');

  function openDrawer() {
    const d = drawer();
    if (!d.open) d.showModal();
    const invalid = $('[aria-invalid="true"]', d);
    (invalid || $('[autofocus]', d))?.focus();
  }

  document.addEventListener('click', e => {
    if (e.target.closest('[data-close-drawer]')) drawer().close();
    // A click whose target is the dialog itself is either on the backdrop or on the panel's own
    // background; only the first should close it (and lose what was typed).
    const d = drawer();
    if (e.target === d) {
      const r = d.getBoundingClientRect();
      const outside = e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom;
      if (outside) d.close();
    }
  });

  /* ---------- htmx hooks ---------- */
  document.addEventListener('htmx:afterSwap', e => {
    if (e.detail.target.id === 'drawer-body' && e.detail.target.children.length) openDrawer();
  });
  document.addEventListener('htmx:afterSettle', tick);
  document.addEventListener('htmx:oobAfterSwap', () => $$('#toasts .toast').forEach(arm));
  document.addEventListener('htmx:responseError', () => showToast('error', msg('error')));
  document.addEventListener('htmx:sendError', () => showToast('error', msg('offline')));

  document.addEventListener('closeDrawer', () => drawer()?.close());
  document.addEventListener('storeAdded', () => $$('form[data-reset-on="storeAdded"]').forEach(f => {
    f.reset();
    $('input', f)?.focus();
  }));

  // Deleting from the detail page: go back to the list, keeping the "Undo" toast.
  document.addEventListener('watchDeleted', () => {
    if (document.body.dataset.page !== 'watch') return;
    setTimeout(() => {
      const toast = $('#toasts .toast:last-child');
      try { sessionStorage.setItem('carryToast', toast ? toast.outerHTML : ''); } catch { /* ignore */ }
      location.href = '/';
    }, 50);
  });

  document.addEventListener('DOMContentLoaded', () => {
    drawer()?.addEventListener('close', () => { $('#drawer-body').innerHTML = ''; });
    let carried = null;
    try { carried = sessionStorage.getItem('carryToast'); sessionStorage.removeItem('carryToast'); } catch { /* ignore */ }
    if (carried) {
      $('#toasts').insertAdjacentHTML('beforeend', carried);
      const toast = $('#toasts .toast:last-child');
      delete toast.dataset.armed;
      if (window.htmx) htmx.process(toast);
      arm(toast);
    }
    tick();
  });

  /* ---------- Live updates (Server-Sent Events) ----------
     Browsers allow only 6 connections per site over plain HTTP, and each live page keeps one open.
     So only the visible page stays connected; a hidden or left page lets go of its connection
     and catches up ("live-resync") when it is shown again. Server events become "live-<name>"
     events on <body>, which elements pick up with hx-trigger="live-watch-5 from:body". */
  let source = null;
  let missedUpdates = false;

  function connect() {
    if (source || document.visibilityState !== 'visible') return;
    source = new EventSource('/events');
    source.onmessage = e => window.htmx?.trigger(document.body, `live-${e.data}`);
  }

  function disconnect() {
    if (!source) return;
    source.close();
    source = null;
    missedUpdates = true;
  }

  function resume() {
    connect();
    if (missedUpdates) {
      missedUpdates = false;
      window.htmx?.trigger(document.body, 'live-resync');
    }
  }

  document.addEventListener('visibilitychange', () => (document.visibilityState === 'visible' ? resume() : disconnect()));
  window.addEventListener('pagehide', disconnect);
  window.addEventListener('pageshow', resume);

  /* ---------- Menus: close when clicking elsewhere or picking an item ---------- */
  document.addEventListener('click', e => {
    $$('details.menu[open]').forEach(menu => {
      if (!menu.contains(e.target) || e.target.closest('.menu-list a, .menu-list button')) menu.open = false;
    });
  });

  /* ---------- Keyboard shortcuts ---------- */
  document.addEventListener('keydown', e => {
    if (e.ctrlKey || e.metaKey || e.altKey || e.target.closest('input, textarea, select, [contenteditable]')) return;
    if (drawer()?.open) return;
    if (e.key === 'n' || e.key === 'N') {
      const button = $('#new-watch');
      if (button) { e.preventDefault(); button.click(); }
    }
    if (e.key === 'Escape') $$('details.menu[open]').forEach(m => { m.open = false; });
  });

  /* ---------- Money input: type digits, get "4.500,00" ---------- */
  document.addEventListener('input', e => {
    const input = e.target.closest('[data-money]');
    if (!input) return;
    const digits = input.value.replace(/\D/g, '').replace(/^0+/, '').slice(0, 11);
    if (!digits) { input.value = ''; return; }
    const cents = digits.padStart(3, '0');
    const reais = cents.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    input.value = `${reais},${cents.slice(-2)}`;
  });

  /* ---------- Live relative times and timers ---------- */
  const rtf = new Intl.RelativeTimeFormat(lang, { numeric: 'auto' });
  const UNITS = [['day', 86400], ['hour', 3600], ['minute', 60], ['second', 1]];

  function relative(date) {
    const seconds = (date - Date.now()) / 1000;
    const abs = Math.abs(seconds);
    const [unit, size] = UNITS.find(([, s]) => abs >= s || s === 1);
    return rtf.format(Math.round(seconds / size), unit);
  }

  function tick() {
    $$('time[data-rel]').forEach(t => {
      const date = new Date(t.getAttribute('datetime'));
      if (Number.isNaN(date.getTime())) return;
      const overdue = t.dataset.rel === 'future' && date <= Date.now();
      t.textContent = overdue ? msg('soon') : relative(date);
      t.title = date.toLocaleString(lang);
    });
    $$('[data-elapsed]').forEach(el => {
      const since = new Date(el.dataset.since);
      if (Number.isNaN(since.getTime())) return;
      const s = Math.max(0, Math.floor((Date.now() - since) / 1000));
      el.textContent = `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
    });
  }
  setInterval(tick, 1000);
})();
