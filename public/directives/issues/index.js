define(function (require) {
  var template = require('text!marvel/directives/issues/index.html');
  var module = require('modules').get('marvel/directives', []);
  module.directive('marvelIssues', function () {
    return {
      restrict: 'E',
      scope: {
        title: '@',
        source: '=',
        link: '@'
      },
      template: template
    };
  });
});


