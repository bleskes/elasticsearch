define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  require('plugin/marvel/directives/shard_allocation/index');

  var module = require('modules').get('marvel', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics',
    'pasvaz.bindonce'
  ]);

  require('routes').when('/marvel/shard_allocation', {
    template: require('marvel/views/shard_allocation/shard_allocation_template.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('plugin/marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('shard_allocation', function (courier, $http, $route, $scope, Promise, Private, timefilter, globalState) {
    var clusters = $route.current.locals.marvel.clusters;
    var indexPattern = $scope.indexPattern = $route.current.locals.marvel.indexPattern;
    var ClusterStatusDataSource = Private(require('plugin/marvel/directives/cluster_status/data_source'));
    var ShardRecoveryDataSource = Private(require('plugin/marvel/directives/shard_activity/data_source'));

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    $scope.dataSources = {};

    $scope.dataSources.clusterStatus = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    $scope.dataSources.clusterStatus.register(courier);
    $scope.cluster = _.find($scope.dataSources.clusterStatus.clusters, { _id: globalState.cluster });
    $scope.$watch('dataSources.clusterStatus.clusters', function (clusters) {
      $scope.cluster = _.find(clusters, { _id: globalState.cluster });
    });

    $scope.$on('$destroy', function () {
      _.each($scope.dataSources, function (dataSource) {
        dataSource.destroy();
      });
    });

    courier.fetch();

  });
});

