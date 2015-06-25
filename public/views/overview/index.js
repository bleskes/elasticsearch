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

  module.controller('overview', function ($scope, timefilter, savedVisualizations, courier, marvelMetrics) {
    // turn on the timepicker;
    timefilter.enabled = true;

    // marvelMetrics('os.cpu.user').then(function (metric) {
      // console.log(metric.threshold(3));
    // });

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

