define(function (require) {
  var formatNumber = require('plugins/marvel/lib/format_number');
  var template = require('plugins/marvel/directives/chart/index.html');
  require('angular-nvd3');
  var module = require('ui/modules').get('marvel/directives', []);
  var _ = require('lodash');
  var moment = require('moment');

  var metricObjects = require('plugins/marvel/lib/metrics');

  module.directive('marvelChart', function (marvelMetrics, $route, Private, courier, timefilter) {
    var marvelIndex = $route.current.locals.indexPattern;
    var calcAuto = Private(require('ui/time_buckets/calc_auto_interval'));

    return {
      restrict: 'E',
      scope: {
        source: '='
      },
      template: template,
      link: function($scope, $elem, attrs) {
        var source = $scope.source;
        var metric = source.metric;

        $scope.chart = makeChartObj($scope.source.metric.unit);
        $scope.title = metric.label;
        $scope.unit = metric.units;

        $scope.$watch('source.data', function (data) {
          if (data.length) {
            if (metric.units === '/s') {
              _.each(data, function (row) {
                row.y = row.y/source.bucketSize;
              });
              data = _.filter(data, function (row) {
                return row.y >= 0;
              });
            }
            var last = data[data.length-1];
            $scope.total = formatNumber(last.y, metric.format);
            $scope.chart.data = [{
              values: data,
              key: metric.label
            }];
          }
        });

        $scope.$watch('source.error', function (err) {
          $scope.err = err;
        });
      }
    };

  });

  function makeChartObj(type) {
    return {
      options: {
        chart: {
          type: 'lineChart',
          height: 200,
          showLegend: false,
          showXAxis: true,
          showYAxis: true,
          useInteractiveGuideline: true,
          interactive: false,
          tooltips: true,
          color: ['#000'],
          pointSize: 0,
          margin: {
            top: 10,
            left: 40,
            right: 0,
            bottom: 20
          },
          xAxis: {
            tickFormat: function (d) {
              return formatNumber(d, 'time');
            }
          },
          yAxis: {
            tickFormat: function (d) {
              return formatNumber(d, type);
            },
          },
        }
      }
    };
  }

});

