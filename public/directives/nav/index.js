define(function (require) {
  var template = require('text!marvel/directives/nav/index.html');
  var module = require('modules').get('marvel/directives', []);
  module.directive('marvelNav', function ($location) {
    return {
      restrict: 'E',
      template: template,
      scope: { },
      link: function ($scope, element, attrs) {
        var path = $location.path();
        $scope.sections = [
          { id: 'overview', display: 'Overview', url: '#/marvel' },
          { id: 'indices', display: 'Indices', url: '#/marvel/indices' },
          { id: 'nodes', display: 'Nodes', url: '#/marvel/nodes' },
        ];
        $scope.sections = _.each($scope.sections, function (section) {
          section.class = (section.url === '#' + path)? 'active' : '';
        });
      }
    };
  });
});

