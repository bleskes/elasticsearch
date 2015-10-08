define(function (require) {
  const React = require('react');
  const make = React.DOM;

  const LoadingComponent = React.createClass({
    render: function () {
      const $icon = make.i({className: 'fa fa-spinner fa-pulse'});
      return make.div({
        className: 'loading'
      }, $icon, ' Loading');
    }
  });

  return LoadingComponent;
});
