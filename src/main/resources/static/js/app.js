(() => {
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
  });

  const folderInput = document.getElementById('notes-folder');
  const syncForm = document.getElementById('folder-sync-form');
  const chooseButton = document.getElementById('choose-folder');
  chooseButton?.addEventListener('click', () => folderInput?.click());
  folderInput?.addEventListener('change', () => {
    if (!folderInput.files.length || !syncForm || !chooseButton) return;
    for (const file of folderInput.files) {
      const addInput = (name, value) => {
        const input = document.createElement('input');
        input.type = 'hidden'; input.name = name; input.value = value;
        syncForm.append(input);
      };
      addInput('relativePaths', file.webkitRelativePath || file.name);
      addInput('modifiedAts', file.lastModified);
    }
    chooseButton.disabled = true;
    chooseButton.classList.add('is-loading');
    chooseButton.setAttribute('aria-busy', 'true');
    chooseButton.textContent = 'Syncing…';
    syncForm.submit();
  });

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
