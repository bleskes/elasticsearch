define(function (require) {
  var React = require('react');
  var make = React.DOM;
  var _ = require('lodash');
  var formatNumber = require('plugins/marvel/lib/format_number');
  var jubilee = require('jubilee/build/jubilee');

  var MarvelSparkLine = React.createClass({
    getInitialState: function () {
      var data = this.props.data;
      if (!data && this.props.source) {
        data = this.props.source.data;
      }
      return { chartData: data || [] };
    },
    componentDidMount: function () { this.drawJubileeChart(); },
    componentDidUpdate: function () { this.drawJubileeChart(); },
    render: function () {
      var metric = this.props.source.metric;
      var lastPoint = _.last(this.state.chartData);
      var titleStr = [metric.label + ':', lastPoint ? formatNumber(lastPoint.y, metric.format) : 0, metric.units].join(' ');
      var $title = make.h1(null, titleStr);
      var $chartWrapper = make.div({className: 'jubilee'});
      var attrs = {
        className: this.props.className || ''
      };

      return make.div(attrs, $title, $chartWrapper);
    },
    drawJubileeChart: function () {
      var children = React.findDOMNode(this).children;
      d3.select(children[children.length - 1])
        .datum([this.state.chartData])
        .call(this.jLineChart);
    },
    setData: function (data) {
      if (data && data.length) {
        var source = this.props.source;
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
    componentWillMount: function () {
      // var Tooltip = require('plugins/marvel/directives/tooltip');
      var lineChart = new jubilee.chart.line()
        .height(150)
        .yScale({nice: true})
        .margin({left: 50, right: 10})
        .defined(function (d) { return !_.isNull(d.y); })
        .lines({
          stroke: function () { return '#000'; },
          strokeWidth: '2px',
          interpolate: 'basis'
        })
        .yAxis({
          tick: {
            number: 4,
            outerTickSize: 0,
            showGridLines: true,
            text: { x: -5 }
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
          console.log('mouseenter');
          // Tooltip.showTooltip(evt.pageX, evt.pageY, 'dummy content');
        })
        .on('mousemove', function(evt) {
          console.log('mousemove');
          // Tooltip.showTooltip(evt.pageX, evt.pageY, 'dummy content')
        })
        .on('mouseleave', function(evt) {
          console.log('mouseleave');
          // Tooltip.emptyTooltip();
        });

      this.jLineChart = lineChart;
    }
  });


  return MarvelSparkLine;
});
