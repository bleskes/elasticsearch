define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var injectCss = require('marvel/lib/inject_css');
  injectCss(require('text!marvel/css/main.css'));

  var module = require('modules').get('marvel', [
    'marvel/directives'
  ]);

  require('routes')
  .when('/marvel/indices', {
    template: require('text!marvel/views/indices/index.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('indices', function ($scope, $route, timefilter, Private, Promise, marvelMetrics, globalState, courier) {
    var ChartDataSource = Private(require('marvel/directives/chart/data_source'));
    var ClusterStatusDataSource = Private(require('marvel/directives/cluster_status/data_source'));
    var indexPattern = $route.current.locals.marvel.indexPattern;
    var clusters = $route.current.locals.marvel.clusters;

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    // Define the metrics for the three charts at the top of the
    // page. Use the metric keys from the metrics hash.
    $scope.charts = [
      'index_document_count',
      'index_throttle_time',
      'index_shard_query_rate'
    ];

    // Setup the data sources for the charts
    $scope.dataSources = {};


    // Map the metric keys to ChartDataSources and register them with
    // the courier. Once this is finished call courier fetch.
    Promise
      .all($scope.charts.map(function (name) {
        return marvelMetrics(globalState.cluster, name).then(function (metric) {
          var dataSource = new ChartDataSource(metric, indexPattern, globalState.cluster);
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
      .then(fetch);

    function fetch() {
      var tasks = [];
      _.each($scope.dataSources.issues, function(dataSource) {
        tasks.push(dataSource.fetch());
      });
      tasks.push(courier.fetch());
      return Promise.all(tasks);
    }
  });


});
