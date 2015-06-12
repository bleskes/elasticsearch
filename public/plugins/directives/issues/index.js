define(function (require) {
  var template = require('text!marvel/plugins/directives/issues/index.html');
  var module = require('modules').get('marvel/directives', []);
  module.directive('marvelIssues', function () {
    return {
      restrict: 'E',
      scope: {
        title: '@',
        issues: '=',
        link: '@'
      },
      template: template
    };
  });
});


