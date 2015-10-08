define(function (require) {
  const React = require('react');
  const make = React.DOM;
  const _ = require('lodash');
  const Vector = require('./vector');
  const Bounds = require('./bounds');

  const TooltipComponent = React.createClass({
    render: function () {
      // make the contents of the tooltip
      const $arrow = make.div({className: 'tooltip-arrow'});
      let contentArr = [this.props.content, $arrow];
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
    let attrArr = type.split('.');
    let $node = document.createElement(attrArr.shift());
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
    const tooltipElement = React.createElement(TooltipComponent, {
      content: opts.content
    });

    // Add the tooltip to the DOM
    React.render(tooltipElement, this.$tooltipPortal);
    const boundsObj = (function (bounds) {
      if (!bounds) { return false; }
      const boundsPos = new Vector(bounds.x, bounds.y);
      const boundDimensions = new Vector(bounds.w, bounds.h);
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
    const clientRect = this.$tooltipPortal.getBoundingClientRect();
    const dimensionVector = new Vector(clientRect.width, clientRect.height);
    const direction = determineBestCardinalDirection(this.state.position, dimensionVector, this.state.bounds);
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
    let currClasses = Array.prototype.slice.apply(this.$tooltipPortal.classList);
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
    const makeDirection = (function (basePosition, baseDimensions) {
      return function (opts) {
        function convertRelativeVector(v) { return baseDimensions.clone().multiply(v).add(basePosition); };
        return {
          extents: opts.extents.map(convertRelativeVector),
          position: convertRelativeVector(opts.position),
          key: opts.key
        };
      };
    }(position, dimensions));
    const west = makeDirection({
      extents: [new Vector(-1.00, -.5), new Vector(-1.00, .5)],
      key: 'left',
      position: new Vector(-1.00, -.5)
    });
    const east = makeDirection({
      extents: [new Vector(1.00, -.5), new Vector(1.00, .5)],
      key: 'right',
      position: new Vector(.00, -.5)
    });
    const north = makeDirection({
      extents: [new Vector(-.5, -1.00), new Vector(.5, -1.00)],
      key: 'top',
      position: new Vector(-.5, -1.00)
    });
    const south = makeDirection({
      extents: [new Vector(-.5, 1.00), new Vector(.5, 1.00)],
      key: 'bottom',
      position: new Vector(-.5, 0.00)
    });

    const directions = [west, east, north, south];
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
