define(function (require) {
  var template = require('plugins/marvel/directives/cluster_status/index.html');
  var module = require('ui/modules').get('marvel/directives', []);
  var moment = require('moment');
  var _ = require('lodash');

  module.directive('marvelClusterStatus', function ($location, globalState, kbnUrl, marvelClusters) {
    return {
      restrict: 'E',
      template: template,
      scope: {
        source: '=',
        clusterState: '='
      },
      link: function ($scope) {
        $scope.total_shards = 0;
        $scope.primary_shards = 0;
        $scope.replica_shards = 0;
        $scope.unassigned_shards = 0;

        var unsubscribe = $scope.$on('courier:searchRefresh', function () {
          marvelClusters.fetch(true).then(function (clusters) {
            $scope.source.clusters = clusters;
          });
        });
        $scope.$on('$destroy', unsubscribe);

        $scope.createClass = function (cluster) {
          var classes = [cluster.status];
          if (cluster.license.type === 'lite') {
            classes.push('lite');
          }
          return classes.join(' ');
        };

        $scope.changeCluster = function (name) {
          if (globalState.cluster !== name) {
            globalState.cluster = name;
            globalState.save();
            kbnUrl.changePath('/overview');
          }
        };

        $scope.laggingCluster = false;


        function setShardStats(shardStats) {
          if (shardStats) {
            $scope.total_shards = shardStats.totals.primary + shardStats.totals.replica;
            $scope.primary_shards = shardStats.totals.primary;
            $scope.replica_shards = shardStats.totals.replica;
            $scope.unassigned_shards = shardStats.totals.unassigned.primary +
              shardStats.totals.unassigned.replica;
          }
        }

        $scope.$watch('source.clusters', function (clusters) {
          $scope.cluster = _.find(clusters, { cluster_uuid: $scope.source.cluster });
          setShardStats($scope.cluster.shardStats);
          if ($scope.cluster) {
            $scope.lastUpdate = moment.utc($scope.cluster.lastUpdate);
            var now = moment.utc();
            var diff = now.diff($scope.lastUpdate);
            $scope.lastSeen = moment.duration(diff, 'ms').humanize();
            if (diff > 120000) {
              $scope.laggingCluster = true;
            }
          }
        });
      }
    };
  });

});
