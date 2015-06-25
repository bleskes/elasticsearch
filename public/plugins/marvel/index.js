require.config({
  paths: {
    'lodash-deep': '/marvel/bower_components/lodash-deep/lodash-deep',
    'angular-resource': '/marvel/bower_components/angular-resource/angular-resource'
  }
});
define(function (require) {

  var injectCss = require('marvel/lib/inject_css');
  injectCss(require('text!marvel/css/main.css'));

  var apps = require('registry/apps');
  apps.register(function () {
    return {
      id: 'marvel',
      name: 'Marvel',
      order: 1000
    };
  });

  require('marvel/directives/index.js');
  require('marvel/views/indices/index.js');
  require('marvel/views/overview/index.js');
  require('marvel/views/settings/index.js');

});
