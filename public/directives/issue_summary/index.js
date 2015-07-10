define(function (require) {
  var module = require('modules').get('marvel/directives', []);
  var template = require('text!marvel/directives/issue_summary/index.html');

  module.directive('marvelIssueSummary', function () {
    return {
      restrict: 'E',
      scope: {
        title: '@',
        source: '='
      },
      template: template
    };
  });
});
