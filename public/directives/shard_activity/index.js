define(function (require) {
  var template = require('plugins/marvel/directives/shard_activity/index.html');
  var module = require('ui/modules').get('marvel/directives', []);
  var formatNumber = require('plugins/marvel/lib/format_number');
  var _ = require('lodash');
  module.directive('marvelShardActivity', function () {
    return {
      restrict: 'E',
      scope: {
        source: '=',
        onlyActive: '=?'
      },
      template: template,
      link: function ($scope) {
        $scope.formatNumber = formatNumber;
        $scope.visibleData = [];
        $scope.data = [];

        $scope.toggleActive = function () {
          $scope.onlyActive = !$scope.onlyActive;
          filterData($scope.data);
        };

        $scope.lookup = {
          GATEWAY: 'Primary',
          REPLICA: 'Replica',
          SNAPSHOT: 'Snapshot',
          RELOCATION: 'Relocation'
        };

        if ($scope.onlyActive != null) $scope.onlyActive = true;

        function filterData() {
          if ($scope.source && $scope.source.data) {
            $scope.visibleData = _.filter($scope.source.data, function (item) {
              if ($scope.onlyActive) {
                return item.stage !== 'DONE';
              }
              return true;
            });
          }
        }
        filterData();

        $scope.$watch('source.data', filterData);

        $scope.getIpAndPort = function (transport) {
          var matches = transport.match(/([\d\.:]+)\]$/);
          if (matches) return matches[1];
          return transport;
        };
      }
    };
  });
});
