/* DNtech — Gallery Lightbox */
(function () {
  const lightbox = document.getElementById('lightbox');
  if (!lightbox) return;
  const lightboxImg = lightbox.querySelector('.lightbox__content img');
  const closeBtn = lightbox.querySelector('.lightbox__close');
  const prevBtn = lightbox.querySelector('.lightbox__nav--prev');
  const nextBtn = lightbox.querySelector('.lightbox__nav--next');
  const items = document.querySelectorAll('.portfolio-item');
  let currentIndex = 0;

  items.forEach(function (item, i) {
    item.addEventListener('click', function () {
      currentIndex = i;
      openLightbox();
    });
  });

  function openLightbox() {
    const img = items[currentIndex].querySelector('img');
    if (img) lightboxImg.src = img.src;
    lightbox.classList.add('active');
    document.body.style.overflow = 'hidden';
  }
  function closeLightbox() {
    lightbox.classList.remove('active');
    document.body.style.overflow = '';
  }
  function nextSlide() { currentIndex = (currentIndex + 1) % items.length; openLightbox(); }
  function prevSlide() { currentIndex = (currentIndex - 1 + items.length) % items.length; openLightbox(); }

  if (closeBtn) closeBtn.addEventListener('click', closeLightbox);
  if (prevBtn) prevBtn.addEventListener('click', prevSlide);
  if (nextBtn) nextBtn.addEventListener('click', nextSlide);
  lightbox.addEventListener('click', function (e) { if (e.target === lightbox) closeLightbox(); });
  document.addEventListener('keydown', function (e) {
    if (!lightbox.classList.contains('active')) return;
    if (e.key === 'Escape') closeLightbox();
    if (e.key === 'ArrowRight') nextSlide();
    if (e.key === 'ArrowLeft') prevSlide();
  });
})();
