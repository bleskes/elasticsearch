define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var moment = require('moment');

  var module = require('ui/modules').get('marvel', [
    'plugins/marvel/directives'
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
    var TableDataSource = Private(require('plugins/marvel/lib/table_data_source'));
    var indexPattern = $route.current.locals.marvel.indexPattern;
    var clusters = $route.current.locals.marvel.clusters;
    var docTitle = Private(require('ui/doc_title'));
    docTitle.change('Marvel', true);


    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    var cluster = _.find(clusters, { cluster_uuid: globalState.cluster });
    $scope.nodes = cluster.nodes;

    // Setup the data sources for the charts
    $scope.dataSources = {};

    var ClusterStateDataSource = Private(require('plugins/marvel/lib/cluster_state_data_source'));
    $scope.dataSources.clusterState = new ClusterStateDataSource({
      indexPattern: indexPattern,
      cluster: cluster
    });
    $scope.dataSources.clusterState.register(courier);

    // Map the metric keys to ChartDataSources and register them with
    // the courier. Once this is finished call courier fetch.
    Promise
      .all([])
      .then(function () {
        var dataSource = new ClusterStatusDataSource(indexPattern, cluster, clusters);
        dataSource.register(courier);
        $scope.dataSources.cluster_status = dataSource;
        return dataSource;
      })
      .then(function() {
        var dataSource = new TableDataSource({
          index: indexPattern,
          cluster: cluster,
          clusters: clusters,
          metrics: [
            'node_jvm_mem_percent',
            'node_space_free',
            'load_average_1m'
          ],
          type: 'node',
          duration: moment.duration(10, 'minutes')
        });
        dataSource.register(courier);
        $scope.dataSources.nodes_table = dataSource;
        return dataSource;
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

    $scope.$on('$destroy', function () {
      _.each($scope.dataSources, function (dataSource) {
        dataSource.destroy();
      });
    });
  });
});

