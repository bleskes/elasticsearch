define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  // require('plugins/visualize/saved_visualizations/saved_visualizations');
  // require('ui/timepicker/timepicker');
  require('plugins/marvel/services/settings');
  require('plugins/marvel/services/metrics');
  require('plugins/marvel/services/clusters');
  require('angular-bindonce');

  var module = require('ui/modules').get('marvel', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics',
    'nvd3',
    'pasvaz.bindonce'
  ]);

  require('ui/routes')
  .when('/nodes', {
    template: require('plugins/marvel/views/nodes/nodes_template.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('plugins/marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('nodes', function (kbnUrl, globalState, $scope, timefilter, $route, courier, marvelMetrics, Private, Promise, es) {
    var ChartDataSource = Private(require('plugins/marvel/directives/chart/data_source'));
    var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));
    var NodesDataSource = Private(require('plugins/marvel/directives/node_listing/nodes_data_source'));
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
      'search_request_rate',
      'index_request_rate',
      'index_latency'
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
            metric: metric,
            cluster: globalState.cluster
          }
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
      .then(function() {
        var dataSource = new NodesDataSource(indexPattern, globalState.cluster);
        dataSource.register(courier);
        $scope.dataSources.nodes = dataSource;
        return $scope.dataSources.nodes;
      })
      .then(fetch);

    function fetch () {
      return Promise.all([courier.fetch()]);
    }

    $scope.$listen(globalState, 'save_with_changes', function (changes) {
      if (_.contains(changes, 'time')) {
        fetch();
      }
    });


    });

});

