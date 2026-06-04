/* ============================================
   DNtech — Form Validation & AJAX Submit
   ============================================ */

(function () {
  const forms = document.querySelectorAll('[data-validate]');
  forms.forEach(function (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      if (validateForm(form)) {
        submitForm(form);
      }
    });
    // Real-time validation
    form.querySelectorAll('.form-input, .form-textarea, .form-select').forEach(function (input) {
      input.addEventListener('blur', function () { validateField(input); });
      input.addEventListener('input', function () {
        if (input.classList.contains('error')) validateField(input);
      });
    });
  });

  function validateForm(form) {
    let valid = true;
    form.querySelectorAll('[required]').forEach(function (field) {
      if (!validateField(field)) valid = false;
    });
    return valid;
  }

  function validateField(field) {
    const value = field.value.trim();
    let errorMsg = '';
    // Required check
    if (field.hasAttribute('required') && !value) {
      errorMsg = 'This field is required';
    }
    // Email check
    else if (field.type === 'email' && value) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(value)) errorMsg = 'Please enter a valid email address';
    }
    // Phone check
    else if (field.type === 'tel' && value) {
      const phoneRegex = /^[\+]?[\d\s\-\(\)]{7,20}$/;
      if (!phoneRegex.test(value)) errorMsg = 'Please enter a valid phone number';
    }
    // Max length check
    else if (field.maxLength > 0 && value.length > field.maxLength) {
      errorMsg = 'Maximum ' + field.maxLength + ' characters allowed';
    }

    const errorEl = field.parentElement.querySelector('.form-error');
    if (errorMsg) {
      field.classList.add('error');
      if (errorEl) { errorEl.textContent = errorMsg; errorEl.style.display = 'block'; }
      return false;
    } else {
      field.classList.remove('error');
      if (errorEl) { errorEl.style.display = 'none'; }
      return true;
    }
  }

  async function submitForm(form) {
    const btn = form.querySelector('[type="submit"]');
    const originalText = btn ? btn.innerHTML : '';
    if (btn) { btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...'; btn.disabled = true; }

    // Convert form data to JSON
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());

    // Determine the API endpoint based on the form id or action
    let endpoint = form.getAttribute('action');
    if (form.id === 'trainingEnquiryForm') {
      endpoint = API_BASE + '/training-enquiries';
    } else if (form.id === 'loginForm') {
      endpoint = API_BASE + '/auth/login';
    } else if (!endpoint || endpoint === '#' || endpoint === '') {
      endpoint = API_BASE + '/enquiries'; // Default endpoint for general contact forms
    }

    try {
      const response = await fetch(endpoint, {
        method: form.getAttribute('method') || 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });

      const result = await response.json();

      if (response.ok && result.success) {
        if (result.data && result.data.redirect) {
          window.location.href = result.data.redirect;
          return;
        }
        showToast(result.message || 'Success!', 'success');
        form.reset();
        form.querySelectorAll('.error').forEach(function (el) { el.classList.remove('error'); });
      } else {
        showToast(result.message || 'Something went wrong', 'error');
      }
    } catch (error) {
      console.error('Error submitting form:', error);
      showToast('Connection error. Please try again later.', 'error');
    } finally {
      if (btn) { btn.innerHTML = originalText; btn.disabled = false; }
    }
  }
})();
