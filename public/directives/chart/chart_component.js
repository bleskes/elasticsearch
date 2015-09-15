/*global d2 */
/*eslint new-cap: 0*/
define(function (require) {
  var React = require('react');
  var make = React.DOM;
  var _ = require('lodash');
  var moment = require('moment');
  var formatNumber = require('plugins/marvel/lib/format_number');
  var jubilee = require('jubilee/build/jubilee');


  var LoadingComponent = React.createClass({
    render: function () {
      var $icon = make.i({className: 'fa fa-spinner fa-pulse'});
      return make.div({
        className: 'loading'
      }, $icon, ' Loading');
    }
  });
  var Tooltip = require('plugins/marvel/directives/tooltip');
  var TooltipInnerComponent = React.createClass({
    render: function () {
      var time = moment(this.props.x);
      var $dateDiv = make.div(null,
          make.i({className: 'fa fa-calendar-o'}),
          ' ',
          time.format('YYYY-MM-DD HH:mm:ss'));
      var value = [this.props.label, this.props.y].join(' ');
      return make.div(null, $dateDiv, value);
    }
  });

  var MarvelSparkLine = React.createClass({
    getInitialState: function () {
      var data = this.props.data;
      if (!data && this.props.source) {
        data = this.props.source.data;
      }
      return {
        chartData: data || [],
        loading: true
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
    drawJubileeChart: function () {
      Tooltip.removeTooltip();
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
      window.d3.select(lastChild)
        .datum([this.state.chartData])
        .call(this.jLineChart);
    },
    setData: function (data) {
      if (data && data.length) {
        var source = this.props.source;
        var metric = source.metric;
        if (metric.units === '/s') {
          _.each(data, function (row) {
            row.y = row.y / source.bucketSize;
          });
          data = _.filter(data, function (row) {
            return row.y >= 0;
          });
        }
        var last = data[data.length - 1];
        var total = last ? formatNumber(last.y, metric.format) : 0;

        this.setState({chartData: data, total: total});
      }
    },
    componentWillMount: function () {
      function getSvgElement(el) {
        if (el.tagName === 'svg') {
          return el;
        }
        return getSvgElement(el.parentNode);
      }
      var chartMargin = {
        left: 50,
        right: 10
      };
      var $tooltipLine = document.createElement('div');
      $tooltipLine.style.zIndex = 1;
      $tooltipLine.style.borderLeft = '1px solid #000';
      $tooltipLine.style.position = 'absolute';
      $tooltipLine.style.width = '1px';
      $tooltipLine.style.top = '0';
      var that = this;
      function showTooltip(evt, yValue, valueCoords) {
        var val = yValue[0];
        if (val !== null) {
          var niceVal = formatNumber(val.y, that.props.source.metric.format);
          var tooltipInnerProps = _.assign({label: that.props.source.metric.label}, val, {y: niceVal});
          var tooltipInnerComponentInstance = React.createElement(TooltipInnerComponent, tooltipInnerProps);
          var svgElement = getSvgElement(evt.target);
          var svgClientRect = svgElement.getBoundingClientRect();
          var tooltipOptions = {
            x: svgClientRect.left + valueCoords[0] + chartMargin.left,
            y: svgClientRect.top + svgClientRect.height / 2,
            content: tooltipInnerComponentInstance,
            bounds: {
              x: svgClientRect.left,
              y: svgClientRect.top,
              w: svgElement.clientWidth,
              h: svgElement.clientHeight
            }
          };
          Tooltip.showTooltip(tooltipOptions);

          $tooltipLine.style.height = svgClientRect.height + 'px';
          $tooltipLine.style.left = valueCoords[0] + chartMargin.left + 'px';
          // $tooltipLine.style.top = svgClientRect.top + 'px';
          // Show the tooltip line
          if (!$tooltipLine.parentElement) {
            svgElement.parentElement.appendChild($tooltipLine);
          }
        }
      }
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
        .zeroLine({ add: false })
        .on('mouseenter', showTooltip)
        .on('mousemove', _.throttle(showTooltip, 10))
        .on('mouseleave', function (evt, yValue, chartIndex) {
          var svgElement = getSvgElement(evt.target);
          if ($tooltipLine.parentElement) {
            svgElement.parentElement.removeChild($tooltipLine);
          }
          Tooltip.removeTooltip();
        });

      this.jLineChart = lineChart;
    }
  });

  return MarvelSparkLine;
});
