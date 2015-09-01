define(function(require) {
  var React = require('react');
  var make = React.DOM;
  var _ = require('lodash');
  function Vector(x, y) {
    var props = {
      x: x || 0,
      y: y || 0
    };
    // TODO Make the accessors and getters one method
    this._x = function(val) {
      if (!_.isUndefined(val)) { props.x = val; }
      return props.x;
    };
    this._y = function(val) {
      if (!_.isUndefined(val)) { props.y = val; }
      return props.y;
    };
  }
  Vector.prototype.add = function(vector) {
    this._x(this._x() + vector._x());
    this._y(this._y() + vector._y());
    return this;
  };
  Vector.prototype.subtract = function(vector) {
    this._x(this._x() - vector._x());
    this._y(this._y() - vector._y());
    return this;
  };
  Vector.prototype.clone = function() {
    return new Vector(this._x(), this._y());
  };
  Vector.prototype._xPx = function() { return this._x() + 'px'; };
  Vector.prototype._yPx = function() { return this._y() + 'px'; };

  var TooltipComponent = React.createClass({
    render: function() {
      // make the contents of the tooltip
      var $arrow = make.div({className: 'tooltip-arrow'});
      var contentArr = [this.props.content, $arrow];
      if (!this.props.content) {
        contentArr = [make.div({key: 'placeholde'})];
      }

      return make.div({
        className: 'tooltip-inner',
        key: 'marvel-tooltip'
      }, contentArr);
    }
  });

  // Accepts something like ('div.class1.class2', {id: 'id'})
  // and returns a DOM node
  function El(type, attrs) {
    var attrArr = type.split('.');
    var $node = document.createElement(attrArr.shift());
    $node.className = attrArr.join(' ');
    if( !_.isUndefined(attrs) ) {
      _.assign($node.attributes, attrs);
    }
    return $node;
  }
  function determineBestPosition(posVector, dimensionVector, limitVector) {
  }
  function Tooltip() {
    this.$tooltipPortal = El('div.tooltip.in.top');
  }
  Tooltip.prototype.removeTooltip = function() {
    // Do we really need this here? I think not
    React.unmountComponentAtNode(this.$tooltipPortal);
    if( this._isMounted() ) {
      this._unmountTooltip();
    }
  };
  Tooltip.prototype.showTooltip = function(x, y, content) {
    var position = new Vector(x, y);

    this.lastPosition = position;
    var tooltipElement = React.createElement(TooltipComponent, {
      position: position,
      content: content
    });

    React.render(tooltipElement, this.$tooltipPortal);
    if( !this._isMounted() ) {
      this._mountTooltip(this._positionTooltip);
    } else {
      this._positionTooltip();
    }
  };
  Tooltip.prototype._positionTooltip = function() {
      var dimensions = this.$tooltipPortal.getBoundingClientRect();
      var dimensionVector = new Vector(dimensions.width / 2, dimensions.height+5);
      var position = this.lastPosition.clone();
      position.subtract(dimensionVector);
      this.$tooltipPortal.style.left = position._xPx();
      this.$tooltipPortal.style.top = position._yPx();
  };
  Tooltip.prototype._isMounted = function() { return !!this.$tooltipPortal.parentNode; };
  Tooltip.prototype._mountTooltip = function(cb) {
    document.body.appendChild(this.$tooltipPortal);
    setTimeout(cb.bind(this), 1);
  };
  Tooltip.prototype._unmountTooltip = function() {
    document.body.removeChild(this.$tooltipPortal);
  };

  return new Tooltip();
});
