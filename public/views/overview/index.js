define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  require('plugins/marvel/services/settings');
  require('plugins/marvel/services/metrics');
  require('plugins/marvel/services/clusters');
  require('ui/notify/notify');
  require('angular-bindonce');

  var module = require('ui/modules').get('marvel', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics',
    'pasvaz.bindonce',
    'kibana/notify'
  ]);

  require('ui/routes')
  .when('/overview', {
    template: require('plugins/marvel/views/overview/index.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('plugins/marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('overview', function (kbnUrl, globalState, $scope, timefilter,
        $route, courier, marvelMetrics, Private, Promise, $timeout) {

    var ChartDataSource = Private(require('plugins/marvel/directives/chart/data_source'));
    var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));
    var ShardRecoveryDataSource = Private(require('plugins/marvel/directives/shard_activity/data_source'));
    var IssueDataSource = Private(require('plugins/marvel/directives/issues/data_source'));
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
            indexPattern: indexPattern,
            cluster: globalState.cluster,
            metric: metric
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
        var dataSource = new ShardRecoveryDataSource(indexPattern, globalState.cluster);
        dataSource.register(courier);
        $scope.dataSources.shardActivity = dataSource;
        return $scope.dataSources.shardActivity;
      })
      .then(fetch);

    $scope.$on('$destroy', function () {
      _.each($scope.dataSources, function (dataSource) {
        dataSource.destroy();
      });
    });

    function fetch(withoutCourier) {
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

