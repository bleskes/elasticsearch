define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  require('plugins/marvel/directives/shard_allocation/index');

  var module = require('ui/modules').get('marvel', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics'
  ]);

  require('ui/routes').when('/shard_allocation', {
    template: require('plugins/marvel/views/shard_allocation/shard_allocation_template.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('plugins/marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('shard_allocation', function (courier, $http, $route, $scope, Promise, Private, timefilter, globalState) {
    var clusters = $route.current.locals.marvel.clusters;
    var indexPattern = $scope.indexPattern = $route.current.locals.marvel.indexPattern;
    var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));
    var ShardRecoveryDataSource = Private(require('plugins/marvel/directives/shard_activity/data_source'));
    var docTitle = Private(require('ui/doc_title'));
    docTitle.change('Marvel', true);

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    $scope.dataSources = {};

    var ClusterStateDataSource = Private(require('plugins/marvel/lib/cluster_state_data_source'));
    $scope.dataSources.clusterState = new ClusterStateDataSource({
      indexPattern: indexPattern,
      cluster: globalState.cluster
    });
    $scope.dataSources.clusterState.register(courier);

    $scope.dataSources.clusterStatus = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    $scope.dataSources.clusterStatus.register(courier);

    $scope.cluster = _.find($scope.dataSources.clusterStatus.clusters, { cluster_uuid: globalState.cluster });
    $scope.$watch('dataSources.clusterStatus.clusters', function (clusters) {
      $scope.cluster = _.find(clusters, { cluster_uuid: globalState.cluster });
    });

    $scope.$on('$destroy', function () {
      _.each($scope.dataSources, function (dataSource) {
        dataSource.destroy();
      });
    });

    courier.fetch();

  });
});

