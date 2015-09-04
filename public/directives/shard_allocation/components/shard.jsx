/**
 * ELASTICSEARCH CONFIDENTIAL
 * _____________________________
 *
 *  [2014] Elasticsearch Incorporated All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */



/* jshint newcap:false  */
define(function (require) {
  var _ = require('lodash');
  var React = require('react');
  var calculateClass = require('../lib/calculateClass');
  var vents = require('../lib/vents');

  function sortByShard(shard) {
    if (shard.node) {
      return shard.shard;
    }
    return [!shard.primary, shard.shard];
  }

  return React.createClass({
    displayName: 'Shard',

    getInitialState: function () {
      return { tooltip: false };
    },

    componentDidMount: function () {
      var key;
      var element;
      var shard = this.props.shard;
      var self = this;
      var placement = shard.state === 'INITIALIZING' ? 'bottom' : 'top';
      if (shard.relocating_message) {
        key = this.generateKey();
        element = this.getDOMNode();
        vents.on(key, function (action) {
          self.setState({ tooltip: action === 'show' });
        });
      }
    },

    generateKey: function (relocating) {
      var shard = this.props.shard;
      var shardType = shard.primary ? 'primary' : 'replica';
      var additionId = shard.state === 'UNASSIGNED' ? Math.random() : '';
      var node = relocating ? shard.relocating_node : shard.node;
      return shard.index + '.' + node + '.' + shardType + '.' + shard.shard + additionId;
    },

    componentWillUnmount: function () {
      var key;
      var element;
      var shard = this.props.shard;
      if (shard.relocating_message) {
        element = this.getDOMNode();
        key = this.generateKey();
        vents.clear(key);
      }
    },

    toggle: function (event) {
      if (this.props.shard.relocating_message) {
        var action = (event.type === 'mouseenter') ? 'show' : 'hide';
        var key = this.generateKey(true);
        this.setState({ tooltip: action === 'show' });
        vents.trigger(key, action);
      }
    },

    render: function () {
      var shard = this.props.shard;
      var options = {
        className: calculateClass(shard, 'shard')
      };
      var tooltip = this.state.tooltip;
      if (tooltip) {
        tooltip = (<div className="shard-tooltip">{ this.props.shard.relocating_message }</div>);
      }
      return (<div
          onMouseEnter={ this.toggle }
          onMouseLeave={ this.toggle }
          className={ calculateClass(shard, 'shard') }>{ tooltip }{ shard.shard }</div>);
    }
  });

});


