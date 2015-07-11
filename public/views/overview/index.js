define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  // require('plugins/visualize/saved_visualizations/saved_visualizations');
  // require('components/timepicker/timepicker');
  require('marvel/services/settings');
  require('marvel/services/metrics');
  require('marvel/services/clusters');
  require('angular-bindonce');

  var module = require('modules').get('marvel', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics',
    'nvd3',
    'pasvaz.bindonce'
  ]);

  require('routes')
  .when('/marvel', {
    template: require('text!marvel/views/overview/index.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('overview', function (kbnUrl, globalState, $scope, timefilter, $route, courier, marvelMetrics, Private, Promise, $timeout) {
    var ChartDataSource = Private(require('marvel/directives/chart/data_source'));
    var ClusterStatusDataSource = Private(require('marvel/directives/cluster_status/data_source'));
    var ShardRecoveryDataSource = Private(require('marvel/directives/shard_activity/data_source'));
    var IssueDataSource = Private(require('marvel/directives/issues/data_source'));
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
      'search_request_rate',
      'index_request_rate',
      'index_latency'
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
      .then(function() {
        var dataSource = new ShardRecoveryDataSource(indexPattern, globalState.cluster);
        dataSource.register(courier);
        $scope.dataSources.shardActivity = dataSource;
        return $scope.dataSources.shardActivity;
      })
      .then(function () {
        $scope.dataSources.issues = {};
        // _.each(['cluster', 'node', 'index'], function (type) {
        // Index doesn't work, returns a 404
        _.each(['cluster', 'node', 'index'], function (type) {
          var dataSource = new IssueDataSource(globalState.cluster, type);
          var unsubscribe = dataSource.register($scope);
          $scope.$on('$destoy', unsubscribe);
          $scope.dataSources.issues[type] = dataSource;
        });
        return $scope.dataSources.issues;
      })
      .then(fetch);

    function fetch (withoutCourier) {
      var tasks = [];
      _.each($scope.dataSources.issues, function (dataSource) {
        tasks.push(dataSource.fetch());
      });
      tasks.push(courier.fetch());
      return Promise.all(tasks);
    }

    $scope.$listen(globalState, 'save_with_changes', function (changes) {
      if (_.contains(changes, 'time')) {
        fetch();
      }
    });

  });

});

