define(function (require) {
  const React = require('react');
  const make = React.DOM;
  const moment = require('moment');


  const TooltipInnerComponent = React.createClass({
    render: function () {
      const time = moment(this.props.x);
      const $dateDiv = make.div(null,
          make.i({className: 'fa fa-calendar-o'}),
          ' ',
          time.format('YYYY-MM-DD'),
          '   ',
          make.i({className: 'fa fa-clock-o'}),
          ' ',
          time.format('HH:mm:ss'));
      const value = [this.props.label, this.props.y].join(' ');
      return make.div(null, $dateDiv, value);
    }
  });

  return TooltipInnerComponent;
});
