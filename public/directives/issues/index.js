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
          $scope.status = '';
          if (_.some($scope.source.data, { status: 'yellow'})) {
            $scope.status = 'yellow';
          }
          if (_.some($scope.source.data, { status: 'red' })) {
            $scope.status = 'red';
          }
          $scope.total = $scope.source.data.length;
          $scope.displaying = ($scope.total <= 6) ? $scope.total : 6;
        });
      }
    };
  });
});


