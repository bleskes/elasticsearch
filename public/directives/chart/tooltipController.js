define(function (require) {
  const formatNumber = require('plugins/marvel/lib/format_number');
  const _ = require('lodash');
  const Vector = require('plugins/marvel/directives/tooltip/vector');
  const TooltipInnerComponent = require('plugins/marvel/directives/chart/tooltipInnerComponent');
  const React = require('react');

  // To be used with MarvelMainChart
  function ChartTooltipController(thatArg, chartMargin) {
    const Tooltip = require('plugins/marvel/directives/tooltip/index');
    const $tooltipLine = document.createElement('div');
    $tooltipLine.style.zIndex = 1;
    $tooltipLine.style.borderLeft = '2px solid #a4a4ae';
    $tooltipLine.style.position = 'absolute';
    $tooltipLine.style.width = '1px';
    $tooltipLine.style.top = chartMargin.top + 'px';

    function getSvgElement(el) {
      if (el.tagName === 'svg') {
        return el;
      }
      return getSvgElement(el.parentNode);
    }
    // The show method for tooltips
    // To be used with an event from jubilee
    function show(evt, yValue, valueCoords) {
      const val = yValue[0];
      if (val === null) {
        return;
      }
      // Setup
      const niceVal = formatNumber(val.y, this.props.source.metric.format);
      const tooltipInnerProps = _.assign({label: this.props.source.metric.label}, val, {y: niceVal});

      const svgElement = getSvgElement(evt.target);
      const svgClientRect = svgElement.getBoundingClientRect();
      const tooltipPositionVector = new Vector(valueCoords[0], svgClientRect.height / 3);

      const tooltipOptions = {
        x: document.body.scrollLeft + svgClientRect.left + tooltipPositionVector.x + chartMargin.left,
        y: document.body.scrollTop + svgClientRect.top + svgClientRect.height / 3,
        content: React.createElement(TooltipInnerComponent, tooltipInnerProps),
        bounds: {
          x: document.body.scrollLeft + svgClientRect.left,
          y: document.body.scrollTop + svgClientRect.top,
          w: svgClientRect.width,
          h: svgClientRect.height
        }
      };
      Tooltip.showTooltip(tooltipOptions);

      // Position the tooltipline
      $tooltipLine.style.left = tooltipPositionVector.x + chartMargin.left + 'px';
      $tooltipLine.style.height = svgClientRect.height - (chartMargin.bottom + chartMargin.top) + 'px';

      // Show the tooltip line
      if (!$tooltipLine.parentNode) {
        svgElement.parentNode.appendChild($tooltipLine);
      }
    }
    function hide() {
      Tooltip.removeTooltip();
      if ($tooltipLine.parentNode) {
        $tooltipLine.parentNode.removeChild($tooltipLine);
      }
    }
    return {
      showTooltip: show.bind(thatArg),
      hideTooltip: hide.bind(thatArg)
    };
  }

  return ChartTooltipController;
});
