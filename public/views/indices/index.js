define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var moment = require('moment');

  var module = require('ui/modules').get('marvel', [
    'plugins/marvel/directives'
  ]);

  require('ui/routes')
  .when('/indices', {
    template: require('plugins/marvel/views/indices/index.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('plugins/marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('indices', function ($scope, $route, timefilter, Private, Promise, marvelMetrics, globalState, courier) {
    var ChartDataSource = Private(require('plugins/marvel/directives/chart/data_source'));
    var TableDataSource = Private(require('plugins/marvel/lib/table_data_source'));
    var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));
    var indexPattern = $route.current.locals.marvel.indexPattern;
    var clusters = $route.current.locals.marvel.clusters;
    var docTitle = Private(require('ui/doc_title'));
    docTitle.change('Marvel', true);

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    // Define the metrics for the three charts at the top of the
    // page. Use the metric keys from the metrics hash.
    $scope.charts = [
      'cluster_search_request_rate',
      'cluster_query_latency',
      'cluster_index_request_rate',
      'cluster_index_latency'
    ];

    // Setup the data sources for the charts
    $scope.dataSources = {};

    var ClusterStateDataSource = Private(require('plugins/marvel/lib/cluster_state_data_source'));
    $scope.dataSources.clusterState = new ClusterStateDataSource({
      indexPattern: indexPattern,
      cluster: globalState.cluster
    });
    $scope.dataSources.clusterState.register(courier);

    // Map the metric keys to ChartDataSources and register them with
    // the courier. Once this is finished call courier fetch.
    Promise
      .all($scope.charts.map(function (name) {
        return marvelMetrics(globalState.cluster, name).then(function (metric) {
          var options = {
            metric: metric,
            indexPattern: indexPattern,
            cluster: globalState.cluster
          };
          var dataSource = new ChartDataSource(options);
          dataSource.register(courier);
          $scope.dataSources[name] = dataSource;
          return dataSource;
        });
      }))
      .then(function () {
        var dataSource = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
        dataSource.register(courier);
        $scope.dataSources.cluster_status = dataSource;
        return dataSource;
      })
      .then(function () {
        var dataSource = new TableDataSource({
          index: indexPattern,
          cluster: _.find(clusters, { cluster_uuid: globalState.cluster }),
          clusters: clusters,
          metrics: [
            'index_document_count',
            'index_search_request_rate',
            'index_request_rate',
            'index_merge_rate'
          ],
          type: 'index',
          duration: moment.duration(5, 'minutes')
        });
        dataSource.register(courier);
        $scope.dataSources.indices_table = dataSource;
        return dataSource;
      })
      .then(fetch);

    function fetch() {
      return Promise.all([courier.fetch()]);
    }
    $scope.$listen(globalState, 'save_with_changes', function (changes) {
      if (_.contains(changes, 'time')) {
        fetch();
      }
    });

    $scope.$on('$destroy', function () {
      _.each($scope.dataSources, function (dataSource) {
        dataSource.destroy();
      });
    });
  });



});
