/* ============================================
   DNtech — Testimonial Carousel
   Auto-rotating with pause-on-hover
   ============================================ */

(function () {
  const track = document.getElementById('testimonialTrack');
  const dotsContainer = document.getElementById('testimonialDots');
  const prevBtn = document.getElementById('testimonialPrev');
  const nextBtn = document.getElementById('testimonialNext');
  if (!track || !dotsContainer) return;

  const slides = track.querySelectorAll('.testimonials__slide');
  let current = 0;
  let interval;

  // Create dots
  slides.forEach(function (_, i) {
    const dot = document.createElement('button');
    dot.className = 'testimonials__dot' + (i === 0 ? ' active' : '');
    dot.setAttribute('aria-label', 'Go to slide ' + (i + 1));
    dot.addEventListener('click', function () { goTo(i); });
    dotsContainer.appendChild(dot);
  });

  function goTo(index) {
    current = index;
    track.style.transform = 'translateX(-' + (current * 100) + '%)';
    dotsContainer.querySelectorAll('.testimonials__dot').forEach(function (d, i) {
      d.classList.toggle('active', i === current);
    });
  }

  function next() { goTo((current + 1) % slides.length); }
  function prev() { goTo((current - 1 + slides.length) % slides.length); }

  if (prevBtn) prevBtn.addEventListener('click', prev);
  if (nextBtn) nextBtn.addEventListener('click', next);

  // Auto-rotate
  function startAuto() { interval = setInterval(next, 5000); }
  function stopAuto() { clearInterval(interval); }
  startAuto();

  // Pause on hover
  const wrapper = track.closest('.testimonials');
  if (wrapper) {
    wrapper.addEventListener('mouseenter', stopAuto);
    wrapper.addEventListener('mouseleave', startAuto);
  }
})();
