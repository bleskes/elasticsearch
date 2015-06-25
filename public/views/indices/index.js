define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var injectCss = require('marvel/lib/inject_css');
  injectCss(require('text!marvel/css/main.css'));

  var module = require('modules').get('marvel', [
    'marvel/directives'
  ]);

  require('routes')
  .when('/marvel/indices', {
    template: require('text!marvel/views/indices/index.html')
  });

  module.controller('indices', function ($scope) {

  });


});
