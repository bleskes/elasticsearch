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



/* jshint maxlen:false, white:false, newcap:false  */
define(function (require) {
  var React = require('react');
  var D = React.DOM;
  var Unassigned = require('./unassigned.jsx');
  var Assigned = require('./assigned.jsx');
  var ParentItem = require('./parentItem');

  class ShardRow extends React.Component {
    render() {
      var unassigned;
      if (this.props.data.unassigned && this.props.data.unassigned.length) {
        unassigned = (
          <Unassigned shards={ this.props.data.unassigned }></Unassigned>
        );
      } else {
        if (this.props.cols === 3) {
          unassigned = (<td></td>);
        }
      }
      return (
        <tr>
          { unassigned }
          <Assigned shardStats={ this.props.shardStats } data={ this.props.data.children }></Assigned>
        </tr>
      );
    }
  }

  return React.createClass({
    displayName: 'TableBody',
    createRow: function (data) {
      return (<ShardRow key={ data.name } data={ data } {...this.props}></ShardRow>);
    },
    render: function () {
      if (this.props.totalCount === 0) {
        return (
          <tbody>
            <tr>
              <td colSpan={ this.props.cols }>
                <div>
                  <p style={{ margin: '10px 0' }} className="text-center lead">
                    There are no shards allocated.
                  </p>
                </div>
              </td>
            </tr>
          </tbody>
        );
      }

      if (this.props.shardStats) {
        if (this.props.rows.length) {
          return (
            <tbody>{ this.props.rows.map(this.createRow) }</tbody>
          );
        }
      }

      return (
        <tbody>
          <tr>
            <td colSpan={ this.props.cols }></td>
          </tr>
        </tbody>
      );

    }
  });
});

