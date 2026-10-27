/* QR scanner for /verify - uses the html5-qrcode library (loaded from a CDN on that page). */
(function () {
  'use strict';
  var startBtn = document.getElementById('scanToggle');
  var fileInput = document.getElementById('scanFile');
  var box = document.getElementById('scanBox');
  var msg = document.getElementById('scanMsg');
  if (!startBtn || typeof Html5Qrcode === 'undefined') {
    if (msg) { msg.textContent = 'The QR scanner could not be loaded. You can still type the certificate ID.'; }
    return;
  }
  var scanner = null;

  // A QR code holds .../verify?certificateId=CERT-2026-000001 (or just the ID itself)
  function extractId(text) {
    try { var id = new URL(text).searchParams.get('certificateId'); if (id) { return id; } } catch (e) { /* not a URL */ }
    var m = /CERT-\d{4}-\d+/i.exec(text);
    return m ? m[0] : text.trim();
  }
  function goVerify(text) {
    window.location.href = '/verify?certificateId=' + encodeURIComponent(extractId(text));
  }
  function stop() {
    if (!scanner) { return Promise.resolve(); }
    var s = scanner; scanner = null;
    return s.stop().catch(function () {}).then(function () { s.clear(); });
  }

  startBtn.addEventListener('click', function () {
    if (scanner) {
      stop().then(function () { box.classList.add('d-none'); startBtn.innerHTML = '<i class="bi bi-camera"></i> Scan with camera'; });
      return;
    }
    box.classList.remove('d-none');
    msg.textContent = 'Starting camera...';
    scanner = new Html5Qrcode('reader');
    scanner.start({ facingMode: 'environment' }, { fps: 10, qrbox: { width: 230, height: 230 } },
      function (text) { stop().then(function () { goVerify(text); }); }, function () {})
      .then(function () { msg.textContent = 'Point the camera at the certificate QR code.'; startBtn.innerHTML = '<i class="bi bi-stop-circle"></i> Stop camera'; })
      .catch(function (err) {
        scanner = null; box.classList.add('d-none');
        msg.textContent = 'Camera unavailable (' + err + '). Camera access needs HTTPS or localhost - or use "Scan from image".';
      });
  });

  if (fileInput) {
    fileInput.addEventListener('change', function () {
      if (!fileInput.files.length) { return; }
      var reader = new Html5Qrcode('reader-file');
      msg.textContent = 'Reading image...';
      reader.scanFile(fileInput.files[0], false)
        .then(goVerify)
        .catch(function () { msg.textContent = 'No QR code found in that image. Try a clearer picture.'; });
    });
  }
})();
