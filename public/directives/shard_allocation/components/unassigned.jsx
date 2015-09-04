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
  var Shard = require('./shard.jsx');
  var _ = require('lodash');

  return React.createClass({
    displayName: 'Unassigned',
    createShard: function (shard) {
      var type = shard.primary ? 'primary' : 'replica';
      var additionId = shard.state === 'UNASSIGNED' ? Math.random() : '';
      var key = shard.index + '.' + shard.node + '.' + type + '.' + shard.state + '.' + shard.shard + additionId;
      return (<Shard shard={ shard } key={ key }></Shard>);
    },
    render: function () {
      var shards = _.sortBy(this.props.shards, 'shard').map(this.createShard);
      return (
        <td className="unassigned">
          <div className="children">{ shards }</div>
        </td>
        );
    }
  });
});
