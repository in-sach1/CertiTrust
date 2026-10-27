/* CertiTrust front-end behaviour: reveal-on-scroll, animated counters, mini QR art, revoke modal, loading buttons. */
(function () {
  'use strict';

  // Reveal elements when they scroll into view
  var revealItems = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); } });
    }, { threshold: 0.12 });
    revealItems.forEach(function (el) { io.observe(el); });
  } else {
    revealItems.forEach(function (el) { el.classList.add('in'); });
  }

  // Animated counters: <span data-count="1234" data-decimals="1">
  function runCounter(el) {
    var target = parseFloat(el.getAttribute('data-count')) || 0;
    var decimals = parseInt(el.getAttribute('data-decimals') || '0', 10);
    var start = null, duration = 1400;
    function step(ts) {
      if (start === null) { start = ts; }
      var p = Math.min((ts - start) / duration, 1);
      var eased = 1 - Math.pow(1 - p, 3);
      el.textContent = (target * eased).toLocaleString(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
      if (p < 1) { requestAnimationFrame(step); }
    }
    requestAnimationFrame(step);
  }
  var counters = document.querySelectorAll('[data-count]');
  if ('IntersectionObserver' in window) {
    var co = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) { if (e.isIntersecting) { runCounter(e.target); co.unobserve(e.target); } });
    }, { threshold: 0.3 });
    counters.forEach(function (el) { co.observe(el); });
  } else {
    counters.forEach(runCounter);
  }

  // Decorative QR-style pattern on the landing page (not a real code)
  document.querySelectorAll('.qr-mini').forEach(function (box) {
    var seed = 7, n = 15, html = '';
    function rnd() { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; }
    for (var r = 0; r < n; r++) {
      for (var c = 0; c < n; c++) {
        var finder = (r < 5 && c < 5) || (r < 5 && c > 9) || (r > 9 && c < 5);
        var ring = finder && (r === 0 || r === 4 || c === 0 || c === 4 || (c > 9 && (c === 10 || c === 14)) || (r > 9 && (r === 10 || r === 14)));
        var core = finder && !ring && ((r === 2 && c === 2) || (r === 2 && c === 12) || (r === 12 && c === 2));
        var on = finder ? (ring || core) : rnd() > 0.5;
        html += '<i class="' + (on ? '' : 'off') + '"></i>';
      }
    }
    box.innerHTML = html;
  });

  // Revoke modal: point the form at the chosen certificate
  var revokeModal = document.getElementById('revokeModal');
  if (revokeModal) {
    revokeModal.addEventListener('show.bs.modal', function (ev) {
      var btn = ev.relatedTarget;
      if (!btn) { return; }
      var id = btn.getAttribute('data-cert');
      document.getElementById('revokeCertLabel').textContent = id;
      var form = document.getElementById('revokeForm');
      form.action = '/manage/certificates/' + encodeURIComponent(id) + '/revoke';
      form.querySelector('textarea').value = '';
    });
  }

  // Edit modals: copy data-* attributes of the clicked button into the form fields
  document.querySelectorAll('[data-edit-target]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var modal = document.getElementById(btn.getAttribute('data-edit-target'));
      var form = modal.querySelector('form');
      form.action = btn.getAttribute('data-action-prefix') + btn.getAttribute('data-id') + '/edit';
      Object.keys(btn.dataset).forEach(function (key) {
        var field = form.elements[key];
        if (field && ['id', 'actionPrefix', 'editTarget', 'bsToggle', 'bsTarget'].indexOf(key) < 0) { field.value = btn.dataset[key]; }
      });
      if (form.elements.role) { toggleInstitutionRow(form); }
    });
  });

  // User form: only issuers need an institution
  function toggleInstitutionRow(form) {
    var row = form.querySelector('.institution-row');
    if (row && form.elements.role) { row.style.display = form.elements.role.value === 'ISSUER' ? '' : 'none'; }
  }
  document.querySelectorAll('select[name=role]').forEach(function (sel) {
    if (sel.form && sel.form.querySelector('.institution-row')) {
      sel.addEventListener('change', function () { toggleInstitutionRow(sel.form); });
      toggleInstitutionRow(sel.form);
    }
  });

  // Demo login helper: <button data-fill-email="a@b" data-fill-pass="secret">
  document.querySelectorAll('[data-fill-email]').forEach(function (b) {
    b.addEventListener('click', function () {
      document.getElementById('email').value = b.getAttribute('data-fill-email');
      document.getElementById('password').value = b.getAttribute('data-fill-pass');
    });
  });

  // Confirm dangerous actions: <form data-confirm="Are you sure?">
  document.querySelectorAll('form[data-confirm]').forEach(function (f) {
    f.addEventListener('submit', function (e) { if (!window.confirm(f.getAttribute('data-confirm'))) { e.preventDefault(); } });
  });

  // Loading indicator on submit buttons: <button data-loading>
  document.querySelectorAll('form').forEach(function (f) {
    f.addEventListener('submit', function () {
      var b = f.querySelector('button[data-loading]');
      if (b && f.checkValidity()) {
        setTimeout(function () { b.disabled = true; b.innerHTML = '<span class="loader"></span> Please wait...'; }, 0);
      }
    });
  });

  // Copy-to-clipboard buttons: <button data-copy="text">
  document.querySelectorAll('[data-copy]').forEach(function (b) {
    b.addEventListener('click', function () {
      if (navigator.clipboard) { navigator.clipboard.writeText(b.getAttribute('data-copy')); }
      var old = b.innerHTML; b.innerHTML = '<i class="bi bi-check2"></i> Copied';
      setTimeout(function () { b.innerHTML = old; }, 1400);
    });
  });
})();
