define(function (require) {
  var module = require('ui/modules').get('marvel/directives', []);
  var template = require('plugins/marvel/directives/issue_summary/index.html');

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
