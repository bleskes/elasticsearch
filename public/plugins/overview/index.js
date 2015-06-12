define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var injectCss = require('marvel/lib/inject_css');

  require('plugins/visualize/saved_visualizations/saved_visualizations');
  require('components/timepicker/timepicker');
  injectCss(require('text!marvel/css/main.css'));

  var module = require('modules').get('marvel', [
    'marvel/directives'
  ]);

  var apps = require('registry/apps');
  apps.register(function () {
    return {
      id: 'marvel',
      name: 'Marvel',
      order: 1000
    };
  });


  require('routes')
  .when('/marvel', {
    template: require('text!marvel/plugins/overview/index.html')
  });

  module.controller('overview', function ($scope, timefilter, savedVisualizations, courier) {
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
    // savedVisualizations.get({ indexPattern: '[.marvel-]YYYY.MM.DD' }).then(function (newVis) {

      // $scope.timefilter = timefilter;
      // $scope.timefilter.enabled = true;

      // var vis = newVis.vis;
      // var searchSource = $scope.searchSource = newVis.searchSource;
      // searchSource.set('filter', [
      //   { term: { 'cluster_name.raw': 'slapbook-2' } }
      // ]);

      // searchSource.aggs(function () {
      //   vis.requesting();
      //   var dsl = vis.aggs.toDsl();
      //   return dsl;
      // });

      // searchSource.onResults().then(function (resp) {
      // });

      // courier.fetch();


    // });
  });

});

