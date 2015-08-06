define(function (require) {
  var template = require('plugins/marvel/directives/nav/index.html');
  var _ = require('lodash');
  var module = require('ui/modules').get('marvel/directives', []);
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
          { id: 'shard_allocation', display: 'Shard Allocation', url: '#/marvel/shard_allocation' },
        ];
        $scope.sections = _.each($scope.sections, function (section) {
          section.class = (section.url === '#' + path) ? 'active' : '';
        });
      }
    };
  });
});

