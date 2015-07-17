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
  'use strict';
  var _ = require('lodash');
  var React = require('marvel/react');
  var calculateClass = require('../lib/calculateClass');

  function sortByShard(shard) {
    if (shard.node) {
      return shard.shard;
    }
    return [!shard.primary, shard.shard];
  }

  var Shard = React.createClass({
    displayName: 'Shard',
    render: function () {
      var shard = this.props.shard;
      var options = {
        className: calculateClass(shard, 'shard')
      };
      return (React.createElement("div", {className:  calculateClass(shard, 'shard') },  shard.shard));
    }
  });

  return React.createClass({
    createShard: function (shard) {
      var type = shard.primary ? 'primary' : 'replica';
      var additionId = shard.state === 'UNASSIGNED' ? Math.random() : '';
      var key = shard.index + '.' + shard.node + '.' + type + '.' + shard.state + '.' + shard.shard + additionId;
      return (React.createElement(Shard, {shard:  shard, key:  key }));
    },
    render: function () {
      var shards = _.sortBy(this.props.shards, sortByShard).map(this.createShard);
      return (React.createElement("div", {className: "shards"},  shards ));
    }
  });

});

