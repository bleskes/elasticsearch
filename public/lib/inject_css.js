define(function (require) {
  return function injectCss(raw) {
    var styleTag = document.createElement('style');
    styleTag.type = 'text/css';
    var head = document.head || document.getElementsByTagName('head')[0];
    var cssText = document.createTextNode(raw);
    styleTag.appendChild(cssText);
    head.appendChild(styleTag);
  };
});
