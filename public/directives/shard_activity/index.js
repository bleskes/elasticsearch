define(function (require) {
  var template = require('plugins/marvel/directives/shard_activity/index.html');
  var module = require('ui/modules').get('marvel/directives', []);
  var formatNumber = require('plugins/marvel/lib/format_number');
  var _ = require('lodash');
  module.directive('marvelShardActivity', function () {
    return {
      restrict: 'E',
      scope: {
        data: '=',
        onlyActive: '=?'
      },
      template: template,
      link: function ($scope) {
        $scope.formatNumber = formatNumber;
        $scope.visibleData = [];

        $scope.toggleActive = function () {
          $scope.onlyActive = !$scope.onlyActive;
          filterData($scope.data);
        };

        $scope.lookup = {
          STORE: 'Primary',
          GATEWAY: 'Primary',
          REPLICA: 'Replica',
          SNAPSHOT: 'Snapshot',
          RELOCATION: 'Relocation'
        };

        function filterData() {
          if ($scope.data) {
            $scope.visibleData = _.filter($scope.data, function (item) {
              if ($scope.onlyActive) {
                return item.stage !== 'DONE';
              }
              return true;
            });
          }
        }
        filterData();

        $scope.$watch('data', filterData);

        $scope.getIpAndPort = function (transport) {
          var matches = transport.match(/([\d\.:]+)\]$/);
          if (matches) return matches[1];
          return transport;
        };
      }
    };
  });
});
