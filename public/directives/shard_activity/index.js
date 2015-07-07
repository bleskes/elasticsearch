define(function (require) {
  var template = require('text!marvel/directives/shard_activity/index.html');
  var module = require('modules').get('marvel/directives', []);
  var formatNumber = require('marvel/lib/format_number');
  module.directive('marvelShardActivity', function () {
    return {
      restrict: 'E',
      scope: {
        source: '='
      },
      template: template,
      link: function ($scope) {
        $scope.formatNumber = formatNumber;

        $scope.lookup = {
          GATEWAY: 'Primary',
          REPLICA: 'Replica',
          SNAPSHOT: 'Snapshot'
        };

        $scope.getIpAndPort = function (transport) {
          var matches = transport.match(/([\d\.:]+)\]$/);
          if (matches) return matches[1];
          return transport;
        };
      }
    };
  });
});



