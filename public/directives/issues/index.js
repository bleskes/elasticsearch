define(function (require) {
  var template = require('text!marvel/directives/issues/index.html');
  var module = require('modules').get('marvel/directives', []);
  module.directive('marvelIssues', function (marvelMetrics) {
    return {
      restrict: 'E',
      scope: {
        title: '@',
        source: '=',
        link: '@'
      },
      template: template,
      link: function ($scope) {
        $scope.$watch('source.data', function () {
          $scope.total = $scope.source.data.length;
          $scope.displaying = ($scope.total <= 5) ? $scope.total : 5;
        });
      }
    };
  });
});


