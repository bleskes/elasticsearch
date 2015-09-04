define(function (require) {
  var React = require('react');
  var make = React.DOM;

  var ToggleOnClickComponent = React.createClass({
    getInitialState: function () {
      return { visible: this.props.initiallyVisible || false };
    },
    toggleVisibility: function () {
      this.setState({visible: !this.state.visible});
    },
    render: function () {
      var activator = this.props.activator;
      var visible = this.state.visible;
      var content = visible ? this.props.content : null;

      var wrapperStr = this.props.elWrapper || null;
      wrapperStr = wrapperStr.split('.');

      var wrapper = wrapperStr.shift();
      return make[wrapper]({
        className: wrapperStr.join(' '),
        onClick: this.toggleVisibility
      }, activator, content);
    }
  });

  return ToggleOnClickComponent;
});
