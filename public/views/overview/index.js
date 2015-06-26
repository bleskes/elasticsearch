define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  // require('plugins/visualize/saved_visualizations/saved_visualizations');
  // require('components/timepicker/timepicker');
  require('marvel/services/settings');
  require('marvel/services/metrics');

  var module = require('modules').get('marvel', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics',
    'nvd3'
  ]);

  var initMarvelIndex = require('marvel/lib/marvel_index_init');

  require('routes')
  .when('/marvel', {
    template: require('text!marvel/views/overview/index.html'),
    resolve: {
      settings: function (marvelSettings) {
        return marvelSettings.fetch();
      },

      indexPattern: function(Promise, Private, indexPatterns) {
        var marvelIndex = null;
        return new Promise(function(resolve) {
          initMarvelIndex(indexPatterns, Private, function(indexPattern) {
            resolve(indexPattern);
          });
        });
      }
    }
  });

  module.controller('overview', function ($scope, timefilter, $route, courier, marvelMetrics, Private, Promise) {
    var ChartDataSource = Private(require('marvel/directives/chart/data_source'));
    var ClusterStatusDataSource = Private(require('marvel/directives/cluster_status/data_source'));
    var indexPattern = $route.current.locals.indexPattern;
    timefilter.enabled = true;

    // Define the metrics for the three charts at the top of the
    // page. Use the metric keys from the metrics hash.
    $scope.charts = [
      'search_request_rate',
      'index_request_rate',
      'query_latency'
    ];

    // Setup the data sources for the charts
    $scope.dataSources = {};

    // Map the metric keys to ChartDataSources and register them with
    // the courier. Once this is finished call courier fetch.
    Promise
      .all($scope.charts.map(function (name) {
        return marvelMetrics(name).then(function (metric) {
          var dataSource = new ChartDataSource(metric, indexPattern);
          dataSource.register(courier);
          $scope.dataSources[name] = dataSource;
          return dataSource;
        });
      }))
      .then(function () {
        var dataSource = new ClusterStatusDataSource(indexPattern);
        dataSource.register(courier);
        $scope.dataSources.cluster_status = dataSource;
        return dataSource;
      })
      .then(function () {
        courier.fetch();
      });

    $scope.issues = {
      cluster: [
        { status: 'red', field: 'Pending Tasks', message: 'is above 50 at 230' },
        { status: 'red', field: 'Pending Tasks', message: 'is above 50 at 230' },
        { status: 'yellow', field: 'Query Latency', message: 'is above 1000 ms at 1349' },
        { status: 'yellow', field: 'Query Latency', message: 'is above 1000 ms at 1349' },
        { status: 'yellow', field: 'Query Latency', message: 'is above 1000 ms at 1349' }
      ],
      nodes: [
        { status: 'red', field: 'host-01', message: 'is 99% CPU utilization above 80%' },
        { status: 'red', field: 'host-01', message: 'is 79% memory utilization above 75%' },
        { status: 'yellow', field: 'host-03', message: 'is 63% memory utilization above 50%' },
        { status: 'yellow', field: 'host-04', message: 'is 60% memory utilization above 50%' },
        { status: 'yellow', field: 'host-04', message: 'is 60% CPU utilization above 50%' },
      ],
      indices: [
        { status: 'red', field: 'logstash-2015.01.01', message: 'has 2 unassigned primary shard' },
        { status: 'red', field: 'logstash-2015.01.01', message: 'has an unassigned primary shard' },
        { status: 'yellow', field: 'logstash-2015.01.01', message: 'has 5 unassigned replica shards' },
        { status: 'yellow', field: 'logstash-2015.01.02', message: 'has 5 unassigned replica shards' },
        { status: 'yellow', field: 'logstash-2015.01.03', message: 'has 5 unassigned replica shards' }
      ]
    };

    });

});

