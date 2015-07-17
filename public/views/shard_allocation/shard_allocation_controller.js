define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  require('marvel/directives/shard_allocation/index');

  var module = require('modules').get('marvel', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics',
    'pasvaz.bindonce'
  ]);

  require('routes').when('/marvel/shard_allocation', {
    template: require('text!marvel/views/shard_allocation/shard_allocation_template.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('shard_allocation', function (courier, $http, $route, $scope, Promise, Private, timefilter, globalState) {
    var clusters = $route.current.locals.marvel.clusters;
    var indexPattern = $scope.indexPattern = $route.current.locals.marvel.indexPattern;
    var ClusterStatusDataSource = Private(require('marvel/directives/cluster_status/data_source'));
    var ShardRecoveryDataSource = Private(require('marvel/directives/shard_activity/data_source'));

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    $scope.dataSources = {};

    $scope.dataSources.clusterStatus = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    $scope.dataSources.clusterStatus.register(courier);

    $scope.dataSources.shardActivity = new ShardRecoveryDataSource(indexPattern, globalState.cluster);
    $scope.dataSources.shardActivity.register(courier);

    courier.fetch();

  });
});

