(() => {
  const loadingOverlay = document.getElementById('loading-overlay');
  const loadingOverlayTitle = document.getElementById('loading-overlay-title');
  const loadingOverlayDetail = document.getElementById('loading-overlay-detail');
  const showLoadingOverlay = (title, detail = 'Please keep this page open.') => {
    if (!loadingOverlay) return;
    loadingOverlayTitle.textContent = title;
    loadingOverlayDetail.textContent = detail;
    loadingOverlay.hidden = false;
  };
  const hideLoadingOverlay = () => { if (loadingOverlay) loadingOverlay.hidden = true; };

  const currentPath = window.location.pathname;
  const navLinks = document.querySelectorAll('.site-nav a[data-route]');
  const activeLink = [...navLinks].find(link => {
    const route = link.dataset.route;
    return route === '/' ? currentPath === '/' : currentPath.startsWith(route);
  });
  if (activeLink) {
    activeLink.classList.add('is-active');
    const pageName = document.querySelector('[data-page-name]');
    if (pageName) pageName.textContent = activeLink.textContent.trim();
  }

  const sidebar = document.getElementById('app-sidebar');
  const menuButton = document.querySelector('.menu-button');
  menuButton?.addEventListener('click', () => {
    const isOpen = sidebar.classList.toggle('is-open');
    menuButton.setAttribute('aria-expanded', String(isOpen));
  });

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && sidebar?.classList.contains('is-open')) {
      sidebar.classList.remove('is-open');
      menuButton?.setAttribute('aria-expanded', 'false');
      menuButton?.focus();
    }
  });

  document.addEventListener('submit', event => {
    const form = event.target.closest('.api-form');
    if (!form || form.dataset.submitting) return;
    form.dataset.submitting = 'true';
    const button = form.querySelector('button[type="submit"]');
    if (!button) return;
    button.disabled = true;
    button.classList.add('is-loading');
    button.setAttribute('aria-busy', 'true');
    button.textContent = button.dataset.loadingLabel || 'Working…';
    if (form.dataset.loadingOverlay !== undefined) showLoadingOverlay(form.dataset.loadingOverlay, form.dataset.loadingDetail);
  });

  const sourceInput = document.getElementById('notes-files');
  const syncForm = document.getElementById('folder-sync-form');
  const chooseButton = document.getElementById('choose-files');
  const dropzone = document.getElementById('source-dropzone');
  const uploadStatus = document.getElementById('upload-status');
  const uploadLabel = document.getElementById('upload-status-label');
  const uploadPercent = document.getElementById('upload-percent');
  const uploadProgress = document.getElementById('upload-progress');
  const selectedFiles = document.getElementById('selected-files');

  const describeFiles = files => files.length === 1 ? files[0].name : `${files.length} files ready to upload`;
  const showProgress = (percent, label) => {
    if (!uploadStatus) return;
    uploadStatus.hidden = false;
    uploadLabel.textContent = label;
    uploadPercent.textContent = `${percent}%`;
    uploadProgress.style.width = `${percent}%`;
    uploadProgress.parentElement.setAttribute('aria-valuenow', String(percent));
  };
  const setFiles = files => {
    if (!sourceInput || !files.length) return;
    const transfer = new DataTransfer();
    [...files].forEach(file => transfer.items.add(file));
    sourceInput.files = transfer.files;
    selectedFiles.textContent = describeFiles(sourceInput.files);
  };
  const upload = () => {
    if (!sourceInput?.files.length || !syncForm || !chooseButton) return;
    const data = new FormData(syncForm);
    [...sourceInput.files].forEach(file => {
      data.append('relativePaths', file.webkitRelativePath || file.name);
      data.append('modifiedAts', file.lastModified);
    });
    chooseButton.disabled = true;
    chooseButton.setAttribute('aria-busy', 'true');
    showProgress(0, `Uploading ${describeFiles(sourceInput.files)}…`);
    const request = new XMLHttpRequest();
    request.open('POST', syncForm.action);
    request.upload.addEventListener('progress', event => {
      if (event.lengthComputable) showProgress(Math.round(event.loaded / event.total * 100), `Uploading ${describeFiles(sourceInput.files)}…`);
    });
    request.upload.addEventListener('load', () => {
      showProgress(100, 'Upload complete. Indexing sources…');
      showLoadingOverlay('Indexing your sources', 'Your upload is complete. We are preparing it for quizzes.');
    });
    request.addEventListener('load', () => {
      if (request.status >= 200 && request.status < 400) {
        showProgress(100, 'Indexing sources…');
        window.location.assign('/sources');
      } else { hideLoadingOverlay(); window.location.assign('/sources'); }
    });
    request.addEventListener('error', () => { hideLoadingOverlay(); window.location.assign('/sources'); });
    request.send(data);
  };
  chooseButton?.addEventListener('click', event => { event.stopPropagation(); sourceInput?.click(); });
  dropzone?.addEventListener('click', () => sourceInput?.click());
  dropzone?.addEventListener('keydown', event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); sourceInput?.click(); } });
  sourceInput?.addEventListener('change', () => { setFiles(sourceInput.files); upload(); });
  ['dragenter', 'dragover'].forEach(type => dropzone?.addEventListener(type, event => { event.preventDefault(); dropzone.classList.add('is-dragging'); }));
  ['dragleave', 'drop'].forEach(type => dropzone?.addEventListener(type, event => { event.preventDefault(); dropzone.classList.remove('is-dragging'); }));
  dropzone?.addEventListener('drop', event => { setFiles(event.dataTransfer.files); upload(); });

  document.addEventListener('submit', async event => {
    const form = event.target.closest('.dismiss-review');
    if (!form) return;
    event.preventDefault();
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true; button.classList.add('is-loading');
    button.setAttribute('aria-busy', 'true'); button.textContent = 'Dismissing…';
    try {
      const response = await fetch(`${form.action}?partial`, { method: 'POST', body: new FormData(form) });
      if (response.ok) form.closest('.review')?.remove();
      else form.submit();
    } catch { form.submit(); }
  });
})();
