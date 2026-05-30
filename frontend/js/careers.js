/* DNtech — Careers Filter & Apply Form Stepper */
(function () {
  // Job filter
  const searchInput = document.getElementById('jobSearch');
  const typeFilter = document.getElementById('jobTypeFilter');
  const jobCards = document.querySelectorAll('.job-card');
  const countEl = document.getElementById('jobCount');

  function filterJobs() {
    if (!searchInput || !jobCards.length) return;
    const keyword = searchInput.value.toLowerCase();
    const type = typeFilter ? typeFilter.value : '';
    let visible = 0;
    jobCards.forEach(function (card) {
      const title = (card.getAttribute('data-title') || '').toLowerCase();
      const cardType = card.getAttribute('data-type') || '';
      const matchKeyword = !keyword || title.includes(keyword);
      const matchType = !type || cardType === type;
      card.style.display = (matchKeyword && matchType) ? '' : 'none';
      if (matchKeyword && matchType) visible++;
    });
    if (countEl) countEl.textContent = visible;
  }

  if (searchInput) searchInput.addEventListener('input', debounce(filterJobs, 200));
  if (typeFilter) typeFilter.addEventListener('change', filterJobs);

  // Multi-step apply form
  const steps = document.querySelectorAll('.apply-form-step');
  const stepIndicators = document.querySelectorAll('.apply-step');
  const stepLines = document.querySelectorAll('.apply-step__line');
  let currentStep = 0;

  window.nextStep = function () {
    if (currentStep < steps.length - 1) {
      steps[currentStep].classList.remove('active');
      stepIndicators[currentStep].classList.remove('active');
      stepIndicators[currentStep].classList.add('completed');
      if (stepLines[currentStep]) stepLines[currentStep].classList.add('completed');
      currentStep++;
      steps[currentStep].classList.add('active');
      stepIndicators[currentStep].classList.add('active');
    }
  };
  window.prevStep = function () {
    if (currentStep > 0) {
      steps[currentStep].classList.remove('active');
      stepIndicators[currentStep].classList.remove('active');
      currentStep--;
      stepIndicators[currentStep].classList.remove('completed');
      if (stepLines[currentStep]) stepLines[currentStep].classList.remove('completed');
      steps[currentStep].classList.add('active');
      stepIndicators[currentStep].classList.add('active');
    }
  };
})();
