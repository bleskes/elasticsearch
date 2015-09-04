define(function (require) {
  var d3 = require('d3');
  var React = require('react');
  var make = React.DOM;
  var _ = require('lodash');

  var defaultOptions = {
    data: [],
    width: 30,
    height: 20,
    getX: function (d) { return d.x; },
    getY: function (d) { return d.y; },
  };

  function drawSparkline($el, params) {
    var settings = _.assign(defaultOptions, params);

    var x = d3.scale.linear().range([0, settings.width]);
    var y = d3.scale.linear().range([settings.height, 0]);
    var line = d3.svg.line()
      .x(function (d) { return x(settings.getX(d)); })
      .y(function (d) { return y(settings.getY(d)); });

    x.domain(d3.extent(settings.data, settings.getX));
    y.domain(d3.extent(settings.data, settings.getY));

    var $svgCont = d3.select($el)
      .append('svg')
      .attr('class', 'marvel_sparkline')
      .attr('width', settings.width)
      .attr('height', settings.height);
    // Draw the line
    $svgCont.append('path')
      .datum(settings.data)
      .attr('class', 'sparkline')
      .attr('d', line);

    // Draw the point
    var lastDataPoint = settings.data[settings.data.length - 1];
    $svgCont.append('circle')
      .attr('class', 'point')
      .attr('cx', x(settings.getX(lastDataPoint)))
      .attr('cy', y(settings.getY(lastDataPoint)))
      .attr('r', 2);
  }

  return React.createClass({
    render: function () {
      return make.div({className: 'pull-right sparkline_cont'});
    },
    componentDidMount: function () {
      this.renderSparkline();
    },
    componentDidUpdate: function () {
      this.renderSparkline();
    },
    shouldComponentUpdate: function () {
      return false;
    },
    renderSparkline: function () {
      var $cont = this.getDOMNode();
      var $child = $cont.childNodes[0];
      if ($child) {
        $cont.removeChild($child);
      }
      // $cont.clear();
      drawSparkline($cont, {data: this.props.data});
    }
  });
});
