define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  var module = require('ui/modules').get('marvel', []);

  require('ui/routes')
    .when('/index/:index', {
      template: require('plugins/marvel/views/index/index_template.html'),
      resolve: {
        marvel: function (Private) {
          var routeInit = Private(require('plugins/marvel/lib/route_init'));
          return routeInit();
        }
      }
    });

  module.controller('indexView', function ($scope, marvelMetrics, globalState,
      kbnUrl, Notifier, courier, timefilter, Private, $routeParams, $route) {
    var clusters = $route.current.locals.marvel.clusters;
    $scope.indexName = $routeParams.index;
    var indexPattern = $scope.indexPattern = $route.current.locals.marvel.indexPattern;
    var IndexSummaryDataSource = Private(require('plugins/marvel/directives/index_summary/data_source'));
    var ChartDataSource = Private(require('plugins/marvel/directives/chart/data_source'));
    var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));
    var docTitle = Private(require('ui/doc_title'));
    docTitle.change('Marvel - ' + $scope.indexName, true);
    var notify = new Notifier({ location: 'Marvel' });

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    $scope.charts = [
      'index_search_request_rate',
      'index_request_rate',
      'index_size',
      'index_lucene_memory',
      'index_document_count',
      'index_fielddata'
    ];

    $scope.dataSources = {};
    $scope.$on('$destroy', function () {
      _.each($scope.dataSources, function (dataSource) {
        dataSource.destroy();
      });
    });

    $scope.dataSources.indexSummary = new IndexSummaryDataSource({
      indexPattern: indexPattern,
      cluster: globalState.cluster,
      indexName: $scope.indexName
    });
    $scope.dataSources.indexSummary.register(courier);

    var ClusterStateDataSource = Private(require('plugins/marvel/lib/cluster_state_data_source'));
    $scope.dataSources.clusterState = new ClusterStateDataSource({
      indexPattern: indexPattern,
      cluster: globalState.cluster
    });
    $scope.dataSources.clusterState.register(courier);

    $scope.dataSources.clusterStatus = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    $scope.dataSources.clusterStatus.register(courier);
    $scope.cluster = _.find(clusters, { cluster_uuid: globalState.cluster });

    // is the selected index valid?
    function checkIndexExists(indexName) {
      if (!_.has($scope.cluster.shardStats, indexName)) {
        notify.error('We can\'t seem to find this index in your Marvel data.');
        return kbnUrl.redirect('/indices');
      }
    }
    checkIndexExists($scope.indexName);

    $scope.$watch('dataSources.clusterStatus.clusters', function (clusters) {
      $scope.cluster = _.find(clusters, { cluster_uuid: globalState.cluster });
      checkIndexExists($scope.indexName);
    });

    Promise
      .all($scope.charts.map(function (name) {
        return marvelMetrics(globalState.cluster, name).then(function (metric) {
          var options = {
            indexPattern: indexPattern,
            cluster: globalState.cluster,
            metric: metric,
            filters: [{ term: { 'index_stats.index': $scope.indexName } }]
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

    $scope.$listen(globalState, 'save_with_changes', function (changes) {
      if (_.contains(changes, 'time')) {
        courier.fetch();
      }
    });

  });
});
