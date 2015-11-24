/*global d3 */
/*eslint new-cap: 0*/
define(function (require) {
  var React = require('react');
  var make = React.DOM;
  var _ = require('lodash');
  var moment = require('moment');
  var formatNumber = require('plugins/marvel/lib/format_number');
  var jubilee = require('jubilee/build/jubilee');
  var LoadingComponent = require('plugins/marvel/directives/chart/loadingComponent');
  var ChartTooltipController = require('plugins/marvel/directives/chart/tooltipController');



  var MarvelMainChart = React.createClass({
    getInitialState: function () {
      var data = this.props.data;
      if (!data && this.props.source) {
        data = this.props.source.data;
      }
      return {
        chartData: data || [],
        loading: false
      };
    },
    componentDidUnmount: function () {
      window.removeEventListener('resize', this.drawJubileeChart);
    },
    componentDidMount: function () {
      window.addEventListener('resize', _.throttle(this.drawJubileeChart, 10));
      this.drawJubileeChart();
    },
    componentDidUpdate: function () { this.drawJubileeChart(); },
    render: function () {
      // Draw everything around the chart, and
      // create a blank div to be filled with the chart later
      var metric = this.props.source.metric;
      var lastPoint = _.last(this.state.chartData);
      var titleStr = [metric.label + ':', lastPoint ? formatNumber(lastPoint.y, metric.format) : 0, metric.units].join(' ');
      var $title = make.h1(null, titleStr);
      var $chartWrapper = make.div({className: 'jubilee'});
      var attrs = { className: this.props.className || '' };

      return make.div(attrs, $title, $chartWrapper);
    },
    shouldComponentUpdate: function (nextProps, nextState) {
      return this.state.loading === nextState.loading;
    },
    removeTooltip: function () {
      this.tooltipController.hideTooltip();
    },
    drawJubileeChart: function () {
      this.removeTooltip();
      var data = this.state.chartData;
      var children = React.findDOMNode(this).children;
      var lastChild = children[children.length - 1];
      if (!data || !data.length) {
        React.render(React.createElement(LoadingComponent), lastChild);
        return;
      }
      if (this.state.loading) {
        lastChild.removeChild(lastChild.childNodes[0]);
        this.setState({loading: false});
      }
      // Hide the tooltip so you don't get old data with it.
      d3.select(lastChild)
        .datum([this.state.chartData])
        .call(this.jLineChart);
    },
    setData: function (data) {
      if (data && data.length) {
        var source = this.props.source;
        var metric = source.metric;
        var last = data[data.length - 1];
        var total = last ? formatNumber(last.y, metric.format) : 0;

        this.setState({chartData: data, total: total});
      }
    },
    componentWillMount: function () {
      // Reference for use with posistioning things, and also the options
      var chartMargin = {
        left: 60,
        right: 10,
        top: 20,
        bottom: 20
      };
      this.tooltipController = ChartTooltipController(this, chartMargin);
      // Finally make the line chart
      var lineChart = new jubilee.chart.line()
        .height(150)
        .yScale({nice: true})
        .margin(chartMargin)
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
            text: { x: -5 },
            format: function (val) { return formatNumber(val, this.props.source.metric.format); }.bind(this)
          }
        })
        .xAxis({
          tick: {
            number: 6,
            outerTickSize: 0,
            showGridLines: true,
            format: function (val) { return moment(val).format('HH:mm'); }
          }
        })
        .zeroLine({ add: false })
        .on('mouseenter', this.tooltipController.showTooltip)
        .on('mousemove', _.throttle(this.tooltipController.showTooltip, 10))
        .on('mouseleave', this.tooltipController.hideTooltip);

      this.jLineChart = lineChart;
    }
  });

  return MarvelMainChart;
});
