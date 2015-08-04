define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  var module = require('modules').get('marvel', []);

  require('routes')
    .when('/marvel/index/:index', {
      template: require('marvel/views/index/index_template.html'),
      resolve: {
        marvel: function (Private) {
          var routeInit = Private(require('plugins/marvel/lib/route_init'));
          return routeInit();
        }
      }
    });

  module.controller('indexView', function ($scope, marvelMetrics, globalState, courier, timefilter, Private, $routeParams, $route) {
    var clusters = $route.current.locals.marvel.clusters;
    $scope.indexName = $routeParams.index;
    var indexPattern = $scope.indexPattern = $route.current.locals.marvel.indexPattern;
    var ChartDataSource = Private(require('plugins/marvel/directives/chart/data_source'));
    var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    $scope.charts = [
      'index_document_count',
      'index_search_request_rate',
      'index_request_rate',
      'index_size',
      'index_lucene_memory',
      'index_refresh_time'
    ];

    $scope.dataSources = {};
    $scope.$on('$destroy', function () {
      _.each($scope.dataSources, function (dataSource) {
        dataSource.destroy();
      });
    });

    $scope.dataSources.clusterStatus = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    $scope.dataSources.clusterStatus.register(courier);
    $scope.cluster = _.find(clusters, { _id: globalState.cluster });
    $scope.$watch('dataSources.clusterStatus.clusters', function (clusters) {
      $scope.cluster = _.find(clusters, { _id: globalState.cluster });
    });

    Promise
      .all($scope.charts.map(function (name) {
        return marvelMetrics(globalState.cluster, name).then(function (metric) {
          var options = {
            indexPattern: indexPattern,
            cluster: globalState.cluster,
            metric: metric,
            filters: [{ term: { 'index.raw': $scope.indexName } }]
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
