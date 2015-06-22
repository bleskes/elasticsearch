define(['marvel/lib/format_number', 'text!marvel/plugins/directives/chart/index.html', 'nvd3_directives'],
function (formatNumber, template) {
  var module = require('modules').get('marvel/directives', []);
  var _ = require('lodash');

  var metricObjects = require('marvel/lib/metrics');
  // var formatNumber = require('marvel/lib/format_number');

  module.directive('marvelChart', function ($route, Private, courier, timefilter) {
    var marvelIndex = $route.current.locals.indexPattern;
    var Vis = Private(require('components/vis/vis'));
    var lineChartVis = Private(require('registry/vis_types')).byName.line;




    var getNextColor = colorGenerator();

    return {
      restrict: 'E',
      scope: {
        title: '@',
        total: '@',
        unit: '@'
      },
      template: template,
      link: function($scope, $elem, attrs) {
        // get the object with information about the metric
        var metricObj = metricObjects[attrs.metric];
        metricObj.key = attrs.metric;

        $scope.chart = makeChartObj(metricObj.units);

        var searchSource = makeSearchSource(marvelIndex, metricObj, function(values) {
          // show the average?
          $scope.total = formatNumber(getAverage(values));

          // draw the chart, with updating the data
          $scope.chart.data = [{
            values: values,
            key: $scope.title,
            color: getNextColor()
          }];
          $scope.error = null;
        }, function(err) {
          $scope.err = err;
          $scope.chart.data = [];
        });

        // Set the title and unit for now, since that's all we have
        $scope.title = metricObj.label;
        $scope.unit = metricObj.units;


          // listen for changes to the time for updates for now.
          $scope.$listen(timefilter, 'fetch', searchSource.fetch);

          searchSource.fetch();

      }
    };


    // make the searchSource!!
    function makeSearchSource(marvelIndex, metric, dataCb, errorCb) {
      // get the vis to be able convert aggs and get the QueryDSL
      var vis = new Vis(marvelIndex, {
        type: lineChartVis,
        aggs: makeAggs(metric.key)
      });


      // Create the Search Source Obj
      var searchSource = new courier.SearchSource()
        .set('index', marvelIndex)
        .set('size', 0) // This correct?
        // .set('aggs', vis.aggs.toDsl())
        .set('aggs', function() {
          vis.requesting();
          return vis.aggs.toDsl();
        });

      // Setup the Response
      searchSource.onResults(function(resp) {
        dataCb(flattenResp(resp));
      });
      searchSource.onError(function(err) {
        errorCb(err);
      });

      return searchSource;
    }
  });

  function makeAggs(metricKey) {
    var aggs = [{
      // correspond these ID's to the retrieved keys
      id: '1',
      type: 'avg',
      schema: 'metric',
      params: { field: metricKey }
    },
    {
      id: '2',
      type: 'date_histogram',
      schema: 'segment',
      params: {
        field: '@timestamp',
        interval: 'auto' // Might want to change this later.
      }
    }];

    return aggs;
  }

  // Gets a range of colors evenly divided 
  // TODO add a Range limit to the colors you can choose from 
  function getColors(num, range) {
    var maxNumber = Math.pow(16, 6);
    var dividend = maxNumber / (num + 1); // +1 because we don't want black or white.
    var colorArr = [];
    for(var idx = num; idx > 0; idx--) {
      colorArr.push("#" + (idx * dividend).toString(16));
    }
    return colorArr;
  }

  function getAverage(values) {
    // Calculate the Average
    var sum = values.reduce(function(sum, curr) { return sum + curr.y; }, 0);
    return sum / values.length;
  }


  // Simple wrapper for colors
  function colorGenerator() {
    var colorCount = 3;
    var currentIdx = 0;
    var colorOptionsArr = getColors(colorCount);

    return function() {
      if (current === colorOptionsArr.length) {
        currentIdx = 0;
      }
      return colorOptionsArr[currentIdx++];
    };
  }

  // TODO memoize...
  function makeChartObj(type) {
    return {
      options: {
        chart: {
          type: 'lineChart',
          height: 250,
          showLegend: false,
          showXAxis: false,
          showYAxis: false,
          useInteractiveGuideline: true,
          tooltips: true,
          // When colors accepts a range use this too..
          // color: ['#444', '#777', '#aaa'],
          pointSize: 0,
          strokeWidth: 10,
          margin: {
            top: 10,
            left: 0,
            right: 0,
            bottom: 20
          },
          xAxis: { tickFormat: function (d) { return formatNumber(d, 'time'); } },
          yAxis: { tickFormat: function (d) { return formatNumber(d, type); }, },
        }
      }
    };
  }


  function flattenResp(resp) {
    // TODO remove the hard coded 2, this is based on what you send, connect the two...
    var buckets = resp.aggregations[2].buckets;
    return _.map(buckets, function(bucket) {
      return {
        x: bucket.key,
        // FIXME same as the above TODO
        y: bucket[1].value || 0 // Why are one of these null?
      }
    });
  }
});

