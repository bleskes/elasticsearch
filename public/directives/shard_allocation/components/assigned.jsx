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



/* jshint newcap: false */
define(function (require) {
  var React = require('react');
  var D = React.DOM;
  var Shard = require('./shard.jsx');
  var calculateClass = require('../lib/calculateClass');
  var _ = require('lodash');
  var generateQueryAndLink = require('../lib/generateQueryAndLink');

  function sortByName(item) {
    if (item.type === 'node') {
      return [ !item.master, item.name];
    }
    return [ item.name ];
  }

  return React.createClass({
    createShard: function (shard) {
      var type = shard.primary ? 'primary' : 'replica';
      var additionId = shard.state === 'UNASSIGNED' ? Math.random() : '';
      var key = shard.index + '.' + shard.node + '.' + type + '.' + shard.state + '.' + shard.shard + additionId;
      return (<Shard shard={ shard } key={ key }></Shard>);
    },
    createChild: function (data) {
      var key = data.id;
      var classes = ['child'];
      var shardStats = this.props.shardStats;
      if (shardStats && shardStats[key]) {
        classes.push(shardStats[key].status);
      }

      var name = (
        <a href={ generateQueryAndLink(data) }>
          <span>{ data.name }</span>
        </a>
      );
      var master;
      if (data.master) {
        master = (
          <i className="fa fa-star"></i>
        );
      }
      var shards = _.sortBy(data.children, 'shard').map(this.createShard);
      return (
        <div className={ calculateClass(data, classes.join(' ')) } key={ key }>
          <div className='title'>{ name }{ master }</div>
          { shards }
        </div>
      );
    },
    render: function () {
      var data = _.sortBy(this.props.data, sortByName).map(this.createChild);
      return (
        <td><div className='children'>{ data }</div></td>
      );
    }
  });
});
