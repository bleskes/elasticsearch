define(function (require) {
  var template = require('text!marvel/plugins/directives/chart/index.html');
  var module = require('modules').get('marvel/directives', []);
  module.directive('marvelChart', function () {
    return {
      restrict: 'E',
      scope: {
        title: '@',
        total: '@',
        unit: '@'
      },
      template: template
    };
  });
});

