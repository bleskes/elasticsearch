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
        onlyActive: '='
      },
      template: template,
      link: function ($scope) {
        $scope.formatNumber = formatNumber;

        $scope.lookup = {
          GATEWAY: 'Primary',
          REPLICA: 'Replica',
          SNAPSHOT: 'Snapshot',
          RELOCATION: 'Relocation'
        };

        $scope.$watch('source.data', function (data) {
          $scope.data = $scope.onlyActive ? _.filter(data, function (item) {
            return item.stage !== 'DONE';
          }) : data;
        });

        $scope.getIpAndPort = function (transport) {
          var matches = transport.match(/([\d\.:]+)\]$/);
          if (matches) return matches[1];
          return transport;
        };
      }
    };
  });
});
