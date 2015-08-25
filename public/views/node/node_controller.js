define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  var module = require('ui/modules').get('marvel', []);

  require('ui/routes')
    .when('/node/:node', {
      template: require('plugins/marvel/views/node/node_template.html'),
      resolve: {
        marvel: function (Private) {
          var routeInit = Private(require('plugins/marvel/lib/route_init'));
          return routeInit();
        }
      }
    });

  module.controller('nodeView', function ($scope, marvelMetrics, globalState, courier, timefilter, Private, $routeParams, $route) {
    var clusters = $route.current.locals.marvel.clusters;
    $scope.nodeName = $routeParams.node;
    var indexPattern = $scope.indexPattern = $route.current.locals.marvel.indexPattern;
    var ChartDataSource = Private(require('plugins/marvel/directives/chart/data_source'));
    var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));
    var docTitle = Private(require('ui/doc_title'));
    docTitle.change('Marvel - ' + $scope.nodeName, true);

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    $scope.charts = [
      'node_jvm_mem_percent',
      'node_space_free',
      'load_average_1m'
    ];

    $scope.dataSources = {};
    $scope.$on('$destroy', function () {
      _.each($scope.dataSources, function (dataSource) {
        dataSource.destroy();
      });
    });

    var ClusterStateDataSource = Private(require('plugins/marvel/lib/cluster_state_data_source'));
    $scope.dataSources.clusterState = new ClusterStateDataSource({
      indexPattern: indexPattern,
      cluster: globalState.cluster
    });
    $scope.dataSources.clusterState.register(courier);

    $scope.dataSources.clusterStatus = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    $scope.dataSources.clusterStatus.register(courier);
    $scope.cluster = _.find(clusters, { cluster_uuid: globalState.cluster });
    $scope.node = $scope.cluster.nodes[$scope.nodeName];
    $scope.$watch('dataSources.clusterStatus.clusters', function (clusters) {
      $scope.cluster = _.find(clusters, { cluster_uuid: globalState.cluster });
    });

    Promise
      .all($scope.charts.map(function (name, idx) {
        return marvelMetrics(globalState.cluster, name).then(function (metric) {
          var options = {
            indexPattern: indexPattern,
            cluster: globalState.cluster,
            metric: metric,
            filters: [{ term: { 'node_stats.node_id': $scope.nodeName } }]
          };
          var dataSource = new ChartDataSource(options);
          dataSource.register(courier);
          $scope.dataSources[name] = dataSource;
          return dataSource;
        });
      }))
      .then(function () {
        return courier.fetch();
      });


  });
});
