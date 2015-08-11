define(function (require) {
  var React = require('react');
  var _ = require('lodash');
  var formatNumber = require('marvel/lib/format_number');
  var jubilee = require('jubilee');
  var module = require('modules').get('marvel/directives', []);

  module.directive('marvelChart', function (marvelMetrics, $route, Private, courier, timefilter) {
    return {
      restrict: 'E',
      scope: {
        source: '='
      },
      link: function($scope, $elem, attrs) {
        var $chart = React.createElement(MarvelSparkLine, {
          scope: $scope
        });

        React.render($chart, $elem[0]);

        $scope.$watch('source.error', function (err) {
          $scope.err = err;
        });
      }
    };

  });

  var make = React.DOM;
  var MarvelSparkLine = React.createClass({
    componentDidMount: function() { this.drawJubileeChart(); },
    componentDidUpdate: function() { this.drawJubileeChart(); },
    componentShouldUpdate: function() { return false; },
    render: function() {
      var metric = this.props.scope.source.metric;
      var lastPoint = _.last(this.props.scope.source.data);
      var titleStr = [metric.label + ':', lastPoint ? lastPoint.y : 0, metric.units].join(' ');
      var $title = make.h1(null, titleStr);
      var $chartWrapper = make.div({className: 'jubilee'});

      return make.div(null, $title, $chartWrapper);
    },
    drawJubileeChart: function() {
      // All of the options are set in componentWillMount, however
      // all of the other options have to be done up here.
      //
      // Convince Shelby that perhaps the width auto would be good based off
      // Actuall using the charting library
      // Also, Why not use _.assign and such especially for options?
      // What about prototype and such for a reduction in duplication of efforts
      // Whenever you add an option you have to
      // write it in 3 different places, including the accessor

      this.jLineChart.width(this.getDOMNode().getBoundingClientRect().width - 40);
      d3.select(this.getDOMNode())
        .datum(this.props.scope.source.data || [])
        .call(this.jLineChart);
    },
    setData: function(data) {
      if (data) {
        var source = this.props.scope.source;
        var metric = source.metric;
        if (metric.units === '/s') {
          _.each(data, function (row) {
            row.y = row.y/source.bucketSize;
          });
          data = _.filter(data, function (row) {
            return row.y >= 0;
          });
        }
        var last = data[data.length-1];
        var total = last ? formatNumber(last.y, metric.format) : 0;

        this.setState({chartData: data, total: total});
      }
    },
    componentWillMount: function() {
      this.props.scope.$watch('source.data', this.setData);
      var lineChart = new jubilee.chart.line()
        .height(300)
        .yScale({nice: true})
        .margin({left: 20, right: 0})
        .lines({
          stroke: '#000',
          strokeWidth: '2px',
          interpolate: 'basis'
        })
        .yAxis({
          tick: {
            outerTickSize: 0,
            showGridLines: true,
            text: {
              x: -5
            }
          }
        })
        .xAxis({
          tick: {
            number: 6,
            outerTickSize: 0,
            showGridLines: true
          }
        })
        .circles({
          show: false
        })
        .zeroLine({
          add: false
        })
        .on('mouseenter', function(evt) {
          console.log('Mouse Enter');
          console.log(evt);
        });

      this.jLineChart = lineChart;
    }
  });

});

