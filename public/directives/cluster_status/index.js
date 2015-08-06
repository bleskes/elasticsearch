define(function (require) {
  var template = require('plugins/marvel/directives/cluster_status/index.html');
  var module = require('ui/modules').get('marvel/directives', []);
  var moment = require('moment');
  var _ = require('lodash');

  module.directive('marvelClusterStatus', function ($location, globalState, kbnUrl, marvelClusters, marvelClusterState) {
    return {
      restrict: 'E',
      template: template,
      scope: {
        source: '='
      },
      link: function ($scope) {
        $scope.clusterState = marvelClusterState;
        $scope.primary_shards = 0;
        $scope.replica_shards = 0;
        $scope.unassigned_shards = 0;

        var unsubscribe = $scope.$on('courier:searchRefresh', function () {
          marvelClusters.fetch(true).then(function (clusters) {
            $scope.source.clusters = clusters;
          });
        });
        $scope.$on('$destroy', unsubscribe);

        $scope.changeCluster = function (name) {
          if(globalState.cluster !== name) {
            globalState.cluster = name;
            globalState.save();
            kbnUrl.changePath($location.path());
          }
        };

        $scope.laggingCluster = false;

        $scope.$watch('clusterState.shardStats', function (shardStats) {
          if (shardStats) {
            $scope.primary_shards = shardStats.totals.primary;
            $scope.replica_shards = shardStats.totals.replica;
            $scope.unassigned_shards = shardStats.totals.unassigned.primary +
              shardStats.totals.unassigned.replica;
          }
        });

        $scope.$watch('source.clusters', function (clusters) {
          $scope.cluster = _.find(clusters, { _id: $scope.source.cluster });
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
