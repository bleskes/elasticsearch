require.config({
  paths: {
    'lodash-deep': '/marvel/bower_components/lodash-deep/lodash-deep',
    'angular-resource': '/marvel/bower_components/angular-resource/angular-resource',
    'nvd3': 'marvel/bower_components/nvd3/build/nv.d3',
    'nvd3_directives': 'marvel/bower_components/nvd3_directives/dist/angular-nvd3'
  },
  shim: {
    'nvd3': ['css!marvel/bower_components/nvd3/build/nv.d3.css', 'd3'],
    'nvd3_directives': ['angular', 'd3', 'nvd3']
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
  require('marvel/views/issues/issues_controller.js');
  require('marvel/views/setup/setup_controller.js');

});
