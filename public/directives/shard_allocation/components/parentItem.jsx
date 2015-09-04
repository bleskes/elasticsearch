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



define(function (require) {
  var _ = require('lodash');
  var React = require('react');
  var generateQueryAndLink = require('../lib/generateQueryAndLink');

  function calculateChildStatus(shardStats, items) {
    if (!shardStats) return;
    var status = 'green';
    _.each(items, function (item) {
      if (shardStats[item.id]) {
        if (status !== 'red' && shardStats[item.id] !== 'green') {
          status =  shardStats[item.id].status;
        }
      }
    });
    return status;
  }
  return React.createClass({
    displayName: 'ParentItem',
    render: function () {
      var data = this.props.data;
      var className = ['parentName'];
      var shardStats = this.props.shardStats;
      var status = 'green';
      if (data.type === 'index') {
        status = shardStats[data.name] && shardStats[data.name].status;
      } else {
        status = calculateChildStatus(shardStats, this.props.data.children);
      }


      var cellClassName = ['parent', status];
      if (data.unassignedPrimaries) {
        className.push('text-error');
      }
      var masterIcon;
      if (data.master) {
        masterIcon = (<span className="fa fa-star"></span>);
      }
      var details;
      if (data.details) {
        details = (<div className="details">{ data.details }</div>);
      }
      var name = (
        <a href={ generateQueryAndLink(data) }><span>{ data.name }</span></a>
      );
      return (
        <td nowrap="true" className={ cellClassName.join(' ') }>
          <div className={ className.join(' ') }>
            { name }
            { masterIcon }
          </div>
          { details }
        </td>
      );
    }
  });
});
