(function($) {
  "use strict";
$(window).on("scroll", function(e){
  if ($(window).scrollTop() >= 60) {
      $('.ren-navbar').addClass('fixed-header');
      $('.ren-navbar').addClass('visible-title');
  }
  else {
      $('.ren-navbar').removeClass('fixed-header');
      $('.ren-navbar').removeClass('visible-title');
  }
  });
})(jQuery);