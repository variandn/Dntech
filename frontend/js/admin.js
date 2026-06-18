/* ============================================
   DNtech — Admin Dashboard JS
   All data is loaded dynamically from the API.
   Requires: api-config.js (API_BASE), utils.js
   ============================================ */
(function () {

  // Fetch helper to automatically pass credentials (session cookies) for standalone/cross-origin requests
  function fetchWithCreds(url, options = {}) {
    options.credentials = 'include';
    return fetch(url, options);
  }

  // ── Table Sorting ────────────────────────────────────────────
  const tables = document.querySelectorAll('.data-table[data-sortable]');
  tables.forEach(function (table) {
    table.querySelectorAll('th[data-sort]').forEach(function (th) {
      th.style.cursor = 'pointer';
      th.addEventListener('click', function () {
        sortTable(table, Array.from(th.parentElement.children).indexOf(th));
      });
    });
  });

  function sortTable(table, colIndex) {
    const tbody = table.querySelector('tbody');
    if (!tbody) return;
    const rows = Array.from(tbody.querySelectorAll('tr'));
    const ascending = !table.getAttribute('data-sort-asc') || table.getAttribute('data-sort-asc') === 'false';
    rows.sort(function (a, b) {
      const aText = a.children[colIndex] ? a.children[colIndex].textContent.trim() : '';
      const bText = b.children[colIndex] ? b.children[colIndex].textContent.trim() : '';
      return ascending ? aText.localeCompare(bText) : bText.localeCompare(aText);
    });
    rows.forEach(function (row) { tbody.appendChild(row); });
    table.setAttribute('data-sort-asc', ascending);
  }

  // ── Generic Status Update Handler ────────────────────────────
  document.addEventListener('change', function (e) {
    if (e.target && e.target.classList.contains('status-select')) {
      const sel = e.target;
      const row = sel.closest('tr');
      if (!row || !row.dataset.id) return;

      const endpoint = sel.dataset.endpoint || detectEndpoint();
      if (!endpoint) return;

      row.style.opacity = '0.6';

      fetchWithCreds(API_BASE + endpoint, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: row.dataset.id, status: sel.value })
      })
        .then(r => r.json())
        .then(data => {
          row.style.opacity = '1';
          if (data.success) {
            showToast('Status updated successfully', 'success');
          } else {
            showToast(data.message || 'Update failed', 'error');
          }
        })
        .catch(() => {
          row.style.opacity = '1';
          showToast('Connection error', 'error');
        });
    }
  });

  /** Detect the API endpoint from the current page */
  function detectEndpoint() {
    const path = window.location.pathname;
    if (path.includes('enquiries.html') && !path.includes('training')) return '/enquiries';
    if (path.includes('training-enquiries')) return '/training-enquiries';
    if (path.includes('applications')) return '/applications';
    if (path.includes('jobs')) return '/jobs';
    return null;
  }

  // ── Dashboard Stats ──────────────────────────────────────────
  async function loadDashboardStats() {
    const metricValues = document.querySelectorAll('.metric-card__value');
    if (metricValues.length < 4) return; // Not on dashboard page

    try {
      const resp = await fetchWithCreds(API_BASE + '/dashboard/stats');
      const result = await resp.json();
      if (result.success && result.data) {
        const d = result.data;
        metricValues[0].textContent = d.newEnquiries || 0;
        metricValues[1].textContent = d.jobApplications || 0;
        metricValues[2].textContent = d.trainingSignups || 0;
        metricValues[3].textContent = d.activeJobs || 0;
      }
    } catch (err) {
      console.error('Failed to load dashboard stats:', err);
    }
  }

  // ── Enquiries Table (enquiries.html + dashboard) ─────────────
  async function loadEnquiries() {
    const tableBody = document.querySelector('#enquiriesTableBody');
    if (!tableBody) return;

    try {
      tableBody.innerHTML = '<tr><td colspan="6" class="text-center"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';

      const response = await fetchWithCreds(API_BASE + '/enquiries');
      const result = await response.json();
      if (!response.ok || !result.success) throw new Error(result.message || 'Failed to fetch');

      const data = result.data || [];

      if (data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center">No enquiries found.</td></tr>';
        return;
      }

      tableBody.innerHTML = '';
      data.forEach(item => {
        const date = item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '—';
        const tr = document.createElement('tr');
        tr.dataset.id = item._id;
        tr.innerHTML = `
          <td>${date}</td>
          <td><strong>${sanitiseHTML(item.name)}</strong><br><span class="text-muted" style="font-size: 12px;">${sanitiseHTML(item.email)}</span></td>
          <td>${sanitiseHTML(item.subject || 'General')}</td>
          <td>
            <select class="form-select status-select" data-endpoint="/enquiries" style="padding: 4px 30px 4px 12px; font-size: 12px; height: auto;">
              <option value="new" ${item.status === 'new' ? 'selected' : ''}>New</option>
              <option value="read" ${item.status === 'read' ? 'selected' : ''}>Read</option>
              <option value="replied" ${item.status === 'replied' ? 'selected' : ''}>Replied</option>
            </select>
          </td>
          <td><button class="btn btn--secondary btn--sm" style="padding: 4px 12px;" onclick="viewEnquiry('${item._id}')"><i class="fas fa-eye"></i> View</button></td>
        `;
        tableBody.appendChild(tr);
      });
    } catch (err) {
      console.error(err);
      tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Failed to load data. Ensure backend is running.</td></tr>';
    }
  }

  // ── Jobs Table (jobs.html) ───────────────────────────────────
  async function loadJobs() {
    const tableBody = document.querySelector('#jobsTableBody');
    if (!tableBody) return;

    try {
      tableBody.innerHTML = '<tr><td colspan="6" class="text-center"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';

      const response = await fetchWithCreds(API_BASE + '/jobs');
      const result = await response.json();
      if (!response.ok || !result.success) throw new Error(result.message || 'Failed to fetch');

      const data = result.data || [];

      if (data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center">No job listings found.</td></tr>';
        return;
      }

      tableBody.innerHTML = '';
      data.forEach(item => {
        const statusClass = item.status === 'active' ? 'badge--success' : (item.status === 'draft' ? 'badge--warning' : 'badge--secondary');
        const statusLabel = item.status ? item.status.charAt(0).toUpperCase() + item.status.slice(1) : 'Active';
        const tr = document.createElement('tr');
        tr.dataset.id = item._id;
        tr.innerHTML = `
          <td><strong>${sanitiseHTML(item.title)}</strong></td>
          <td>${sanitiseHTML(item.department)}</td>
          <td>${sanitiseHTML(item.type || 'Full-Time')}</td>
          <td>${sanitiseHTML(item.closingDate || '—')}</td>
          <td><span class="badge ${statusClass}">${statusLabel}</span></td>
          <td>
            <button class="btn btn--secondary btn--sm" style="padding: 4px 8px;" onclick="editJob('${item._id}')"><i class="fas fa-edit"></i></button>
            <button class="btn btn--sm" style="padding: 4px 8px; color: var(--color-danger); border-color: var(--color-danger); background: transparent;" onclick="deleteJob('${item._id}')"><i class="fas fa-trash"></i></button>
          </td>
        `;
        tableBody.appendChild(tr);
      });
    } catch (err) {
      console.error(err);
      tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Failed to load jobs.</td></tr>';
    }
  }

  // ── Applications Table (applications.html) ───────────────────
  async function loadApplications() {
    const tableBody = document.querySelector('#applicationsTableBody');
    if (!tableBody) return;

    try {
      tableBody.innerHTML = '<tr><td colspan="5" class="text-center"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';

      const response = await fetchWithCreds(API_BASE + '/applications');
      const result = await response.json();
      if (!response.ok || !result.success) throw new Error(result.message || 'Failed to fetch');

      const data = result.data || [];

      if (data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="5" class="text-center">No applications found.</td></tr>';
        return;
      }

      tableBody.innerHTML = '';
      data.forEach(item => {
        const date = item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '—';
        const tr = document.createElement('tr');
        tr.dataset.id = item._id;
        tr.innerHTML = `
          <td>${date}</td>
          <td><strong>${sanitiseHTML(item.name)}</strong><br><span class="text-muted" style="font-size: 12px;">${sanitiseHTML(item.email)}</span></td>
          <td>${sanitiseHTML(item.position)}</td>
          <td>${item.cvUrl ? '<a href="' + item.cvUrl + '" class="text-primary" target="_blank"><i class="fas fa-download"></i> Download</a>' : '—'}</td>
          <td>
            <select class="form-select status-select" data-endpoint="/applications" style="padding: 4px 30px 4px 12px; font-size: 12px; height: auto;">
              <option value="new" ${item.status === 'new' ? 'selected' : ''}>Under Review</option>
              <option value="interview" ${item.status === 'interview' ? 'selected' : ''}>Interview Scheduled</option>
              <option value="rejected" ${item.status === 'rejected' ? 'selected' : ''}>Rejected</option>
              <option value="hired" ${item.status === 'hired' ? 'selected' : ''}>Hired</option>
            </select>
          </td>
        `;
        tableBody.appendChild(tr);
      });
    } catch (err) {
      console.error(err);
      tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Failed to load applications.</td></tr>';
    }
  }

  // ── Training Enquiries Table (training-enquiries.html) ───────
  async function loadTrainingEnquiries() {
    const tableBody = document.querySelector('#trainingTableBody');
    if (!tableBody) return;

    try {
      tableBody.innerHTML = '<tr><td colspan="5" class="text-center"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';

      const response = await fetchWithCreds(API_BASE + '/training-enquiries');
      const result = await response.json();
      if (!response.ok || !result.success) throw new Error(result.message || 'Failed to fetch');

      const data = result.data || [];

      if (data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="5" class="text-center">No training enquiries found.</td></tr>';
        return;
      }

      tableBody.innerHTML = '';
      data.forEach(item => {
        const date = item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '—';
        const tr = document.createElement('tr');
        tr.dataset.id = item._id;
        tr.innerHTML = `
          <td>${date}</td>
          <td><strong>${sanitiseHTML(item.name)}</strong><br><span class="text-muted" style="font-size: 12px;">${sanitiseHTML(item.email)}</span></td>
          <td>${sanitiseHTML(item.course)}</td>
          <td>${sanitiseHTML(item.phone || '—')}</td>
          <td>
            <select class="form-select status-select" data-endpoint="/training-enquiries" style="padding: 4px 30px 4px 12px; font-size: 12px; height: auto;">
              <option value="new" ${item.status === 'new' ? 'selected' : ''}>Pending Contact</option>
              <option value="contacted" ${item.status === 'contacted' ? 'selected' : ''}>Contacted</option>
              <option value="enrolled" ${item.status === 'enrolled' ? 'selected' : ''}>Enrolled</option>
            </select>
          </td>
        `;
        tableBody.appendChild(tr);
      });
    } catch (err) {
      console.error(err);
      tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Failed to load training enquiries.</td></tr>';
    }
  }

  // ── Global Action Functions ──────────────────────────────────
  window.viewEnquiry = function (id) {
    showToast('Opening enquiry details…', 'info');
    // Future: open a modal with full enquiry details
  };

  window.editJob = function (id) {
    showToast('Edit job form coming soon', 'info');
    // Future: open a modal pre-filled with job data
  };

  window.deleteJob = function (id) {
    if (!confirm('Are you sure you want to delete this job listing?')) return;
    fetchWithCreds(API_BASE + '/jobs?id=' + id, { method: 'DELETE' })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          showToast('Job deleted', 'success');
          loadJobs();
        } else {
          showToast(data.message || 'Delete failed', 'error');
        }
      })
      .catch(() => showToast('Connection error', 'error'));
  };

  // ── Chart.js Initialization ──────────────────────────────────
  const ctx = document.getElementById('statsChart');
  if (ctx) {
    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
        datasets: [
          {
            label: 'Website Visits',
            data: [120, 190, 150, 220, 180, 250, 210],
            backgroundColor: 'rgba(58, 110, 165, 0.7)',
            borderColor: 'rgba(58, 110, 165, 1)',
            borderWidth: 1,
            borderRadius: 4
          },
          {
            label: 'Applications',
            data: [12, 19, 15, 25, 22, 10, 8],
            backgroundColor: 'rgba(90, 159, 224, 0.7)',
            borderColor: 'rgba(90, 159, 224, 1)',
            borderWidth: 1,
            borderRadius: 4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(0,0,0,0.05)' }
          },
          x: {
            grid: { display: false }
          }
        },
        plugins: {
          legend: {
            position: 'top',
          }
        }
      }
    });
  }

  // ── Session Authentication & Profile Management ──────────────
  async function checkSession() {
    const isLoginPage = window.location.pathname.includes('login.html');
    
    try {
      const resp = await fetchWithCreds(API_BASE + '/auth/session');
      const result = await resp.json();
      
      if (resp.ok && result.success && result.data) {
        if (isLoginPage) {
          window.location.href = 'dashboard.html';
          return;
        }
        updateUserProfileInfo(result.data);
      } else {
        if (!isLoginPage) {
          window.location.href = 'login.html';
        }
      }
    } catch (err) {
      console.error('Session check failed:', err);
      if (!isLoginPage) {
        window.location.href = 'login.html';
      }
    }
  }

  function updateUserProfileInfo(user) {
    const navbarUser = document.querySelector('.navbar .fw-medium');
    if (navbarUser) {
      navbarUser.innerHTML = `<i class="fas fa-user-circle"></i> ${sanitiseHTML(user.username || 'Admin User')}`;
    }
    
    const settingsUsername = document.getElementById('settingsUsername');
    const settingsEmail = document.getElementById('settingsEmail');
    if (settingsUsername) {
      settingsUsername.value = user.username || 'admin';
    }
    if (settingsEmail) {
      settingsEmail.value = user.email || '';
    }
  }

  // Intercept logout button clicks for proper API logout
  document.querySelectorAll('a[href="login.html"]').forEach(btn => {
    if (btn.textContent.trim().toLowerCase() === 'logout') {
      btn.addEventListener('click', async function (e) {
        e.preventDefault();
        try {
          await fetchWithCreds(API_BASE + '/auth/logout', { method: 'POST' });
        } catch (err) {
          console.error('Logout request failed:', err);
        }
        window.location.href = 'login.html';
      });
    }
  });

  // ── Init: Session validation & page data loading ─────────────
  checkSession().then(() => {
    // Only load page data if we are authenticated (unless on login page)
    if (!window.location.pathname.includes('login.html')) {
      loadDashboardStats();
      loadEnquiries();
      loadJobs();
      loadApplications();
      loadTrainingEnquiries();
    }
  });

})();
