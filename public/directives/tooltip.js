define(function (require) {
  var React = require('react');
  var make = React.DOM;
  var _ = require('lodash');
  function Vector(x, y) {
    // Ensure the proper this
    if (!(this instanceof Vector)) {
      return new Vector(x, y);
    }
    var props = {
      x: x || 0,
      y: y || 0
    };
    // TODO Make the accessors and getters one method
    this._x = function (val) {
      var old = props.x;
      if (!_.isUndefined(val)) { props.x = val; }
      return old;
    };
    this._y = function (val) {
      var old = props.y;
      if (!_.isUndefined(val)) { props.y = val; }
      return old;
    };
  }
  Vector.prototype.add = function (vector) {
    this._x(this._x() + vector._x());
    this._y(this._y() + vector._y());
    return this;
  };
  Vector.prototype.subtract = function (vector) {
    this._x(this._x() - vector._x());
    this._y(this._y() - vector._y());
    return this;
  };
  Vector.prototype.multiply = function (vector) {
    this._x(this._x() * vector._x());
    this._y(this._y() * vector._y());
    return this;
  };
  Vector.prototype.clone = function () {
    return new Vector(this._x(), this._y());
  };




  Vector.prototype.toArray = function () {
    return [this._x(), this._y()];
  };
  Vector.prototype.toString = function () {
    return ('x:' + this._x() + ', y:' + this._y());
  };
  Vector.prototype._xPx = function () { return this._x() + 'px'; };
  Vector.prototype._yPx = function () { return this._y() + 'px'; };
  function Bounds(vector1, vector2) {
    if (!(this instanceof Bounds)) {
      return new Bounds(vector1, vector2);
    }
    var props = {
      nw: vector1 || new Vector(),
      se: vector2 || new Vector()
    };

    this._nw = function (val) {
      const old = props.nw;
      if (!_.isUndefined(val)) { props.nw = val; }
      return old;
    };
    this._se = function (val) {
      const old = props.se;
      if (!_.isUndefined(val)) { props.se = val; }
      return old;
    };
  }
  Bounds.prototype.contains = function (vector) {
    var v = vector.toArray();
    var nw = this._nw().toArray();
    var se = this._se().toArray();
    return nw[0] < v[0] &&
      v[0] < se[0] &&
      nw[1] < v[1] &&
      v[1] < se[1];
  };

  var TooltipComponent = React.createClass({
    render: function () {
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
  function el(type, attrs) {
    var attrArr = type.split('.');
    var $node = document.createElement(attrArr.shift());
    $node.className = attrArr.join(' ');
    if (!_.isUndefined(attrs)) {
      _.assign($node.attributes, attrs);
    }
    return $node;
  }
  function Tooltip() {
    this.$tooltipPortal = el('div.tooltip.in.top');
  }
  Tooltip.prototype.removeTooltip = function () {
    // Do we really need this here? I think not
    React.unmountComponentAtNode(this.$tooltipPortal);
    if (this._isMounted()) {
      this._unmountTooltip();
    }
  };
  /*
   *  Opts = {
   *    x: xPosition,
   *    y: yPosition,
   *    content: The content React compoenent or string
   *    bounds: { The bounds to constrain the tooltip by
   *      x: xPosition,
   *      y: yPosition,
   *      w: width,
   *      h: height
   *    }
   *
   *  }
   *
   */
  Tooltip.prototype.showTooltip = function (opts) {

    // Create our React element with the proper content, and template for the tooltip
    var tooltipElement = React.createElement(TooltipComponent, {
      content: opts.content
    });

    // Add the tooltip to the DOM
    React.render(tooltipElement, this.$tooltipPortal);
    var boundsObj = (function (bounds) {
      if (!bounds) { return false; }
      var boundsPos = new Vector(bounds.x, bounds.y);
      var boundDimensions = new Vector(bounds.w, bounds.h);
      return new Bounds(boundsPos, boundsPos.clone().add(boundDimensions));
    }(opts.bounds));
    // save and prepare the options given
    this.state = {
      position: new Vector(opts.x, opts.y),
      bounds: boundsObj
    };
    if (!this._isMounted()) {
      this._mountTooltip(this._positionTooltip.bind(this));
    } else {
      this._positionTooltip();
    }
  };
  // Always call this funciton after you've added things to the tooltip
  // so that way this can properly calcuate the hiehgt and widht necessary
  //
  // Might have to defer this with setTimeout if it doesn't work
  Tooltip.prototype._positionTooltip = function () {
    var clientRect = this.$tooltipPortal.getBoundingClientRect();
    var dimensionVector = new Vector(clientRect.width, clientRect.height);
    var position = this.state.position;
    var direction = determineBestCardinalDirection(this.state.position, dimensionVector, this.state.bounds);
    if (!direction) {
      // if the mouse is somewhere where the tooltip can't be drawn anywhere.
      // When you're too close to the corners
      return;
    }
    this._setDirection(direction.key);
    // Get the position argument from the showTooltip function
    this.$tooltipPortal.style.left = direction.position._xPx();
    this.$tooltipPortal.style.top = direction.position._yPx();
  };
  /*
   *  @param dir String which should be 'left', 'right', 'top', 'bottom'
   */
  Tooltip.prototype._setDirection = function (dir) {
    var currClasses = Array.prototype.slice.apply(this.$tooltipPortal.classList);
    // remove any other conflicting classes
    const classList = ['left', 'right', 'top', 'bottom'];
    _.remove(currClasses, (c) => classList.indexOf(c) > -1);
    // Add the direction we want
    currClasses.push(dir);
    this.$tooltipPortal.className = currClasses.join(' ');
  };
  Tooltip.prototype._isMounted = function () { return !!this.$tooltipPortal.parentNode; };
  Tooltip.prototype._mountTooltip = function (cb) {
    document.body.appendChild(this.$tooltipPortal);
    setTimeout(cb, 1);
  };
  Tooltip.prototype._unmountTooltip = function () {
    document.body.removeChild(this.$tooltipPortal);
  };

  function determineBestCardinalDirection(position, dimensions, bounds) {
    var direction = (function (basePosition, baseDimensions) {
      return function (opts) {
        function convertRelativeVector(v) { return baseDimensions.clone().multiply(v).add(basePosition); };
        return {
          extents: opts.extents.map(convertRelativeVector),
          position: convertRelativeVector(opts.position),
          key: opts.key
        };
      };
    }(position, dimensions));
    const west = direction({
      extents: [new Vector(-1.00, -.5), new Vector(-1.00, .5)],
      key: 'left',
      position: new Vector(-1.00, -.5)
    });
    const east = direction({
      extents: [new Vector(1.00, -.5), new Vector(1.00, .5)],
      key: 'right',
      position: new Vector(.00, -.5)
    });
    const north = direction({
      extents: [new Vector(-.5, -1.00), new Vector(.5, -1.00)],
      key: 'top',
      position: new Vector(-.5, -1.00)
    });
    const south = direction({
      extents: [new Vector(-.5, 1.00), new Vector(.5, 1.00)],
      key: 'bottom',
      position: new Vector(-.5, 0.00)
    });

    var directions = [west, east, north, south];
    if (!bounds) {
      return directions[0];
    }
    // TODO make this an option, so people can have a choice of where the tooltip chooses to draw
    return directions.reduce(function (prev, side, idx, arr) {
      const sideExtentsFit = (_.every(side.extents, bounds.contains, bounds));
      return prev || (sideExtentsFit ? side : false);
    }, false);
  }

  return new Tooltip();
});
