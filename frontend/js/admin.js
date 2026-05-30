/* DNtech — Admin Dashboard JS */
(function () {
  // Table sort
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

  // Inline status update logic for dynamic elements
  document.addEventListener('change', function (e) {
    if (e.target && e.target.classList.contains('status-select')) {
      const sel = e.target;
      const row = sel.closest('tr');
      if (row) row.style.opacity = '0.6';
      
      // Send API update (Mocking the exact endpoint for now)
      // fetch('/api/enquiries/' + row.dataset.id, { method: 'PUT', ... })
      setTimeout(function () {
        if (row) row.style.opacity = '1';
        showToast('Status updated successfully', 'success');
      }, 500);
    }
  });

  // Fetch data for Enquiries Table if it exists
  async function loadEnquiries() {
    const tableBody = document.querySelector('#enquiriesTableBody');
    if (!tableBody) return;

    try {
      tableBody.innerHTML = '<tr><td colspan="6" class="text-center"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';
      
      const response = await fetch('http://localhost:8080/dntech/api/enquiries');
      if (!response.ok) throw new Error('Failed to fetch');
      
      const data = await response.json();
      
      if (data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center">No enquiries found.</td></tr>';
        return;
      }

      tableBody.innerHTML = '';
      data.forEach(item => {
        const date = new Date(item.createdAt).toLocaleDateString();
        const tr = document.createElement('tr');
        tr.dataset.id = item._id;
        tr.innerHTML = `
          <td>${date}</td>
          <td><strong>${item.name}</strong><br><span class="text-muted" style="font-size: 12px;">${item.email}</span></td>
          <td>${item.subject || 'General'}</td>
          <td>
            <select class="form-select status-select" style="padding: 4px 30px 4px 12px; font-size: 12px; height: auto;">
              <option value="new" ${item.status === 'new' ? 'selected' : ''}>New</option>
              <option value="read" ${item.status === 'read' ? 'selected' : ''}>Read</option>
              <option value="replied" ${item.status === 'replied' ? 'selected' : ''}>Replied</option>
            </select>
          </td>
          <td><button class="btn btn--secondary btn--sm" style="padding: 4px 12px;"><i class="fas fa-eye"></i> View</button></td>
        `;
        tableBody.appendChild(tr);
      });
    } catch (err) {
      console.error(err);
      tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Failed to load data. Ensure backend is running.</td></tr>';
    }
  }

  loadEnquiries();

  // Chart.js Initialization
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
})();
