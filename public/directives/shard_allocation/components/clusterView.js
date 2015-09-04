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

  var TableHead = React.createFactory(require('./tableHead'));
  var TableBody = React.createFactory(require('./tableBody.jsx'));

  return React.createClass({
    displayName: 'ClusterView',
    getInitialState: function () {
      return { labels: this.props.scope.labels || [], showing: this.props.scope.showing || [], shardStats: this.props.shardStats };
    },
    setLabels: function (data) {
      if (data) {
        this.setState({ labels: data.slice(1) });
      }
    },
    setShowing: function (data) {
      if (data) {
        this.setState({ showing: data });
      }
    },
    setShardStats: function (stats) {
      this.setState({ shardStats: stats });
    },
    componentWillMount: function () {
      this.props.scope.$watch('labels', this.setLabels);
      this.props.scope.$watch('showing', this.setShowing);
      this.props.scope.$watch('shardStats', this.setShardStats);
    },
    hasUnassigned: function () {
      return this.state.showing.length &&
        this.state.showing[0].unassigned &&
        this.state.showing[0].unassigned.length;
    },
    render: function () {
      var tableHead = TableHead({ hasUnassigned: this.hasUnassigned(), columns: this.state.labels });
      var tableBody = TableBody({
        fitler: this.props.scope.filter,
        totalCount: this.props.scope.totalCount,
        rows: this.state.showing,
        cols: this.state.labels.length,
        shardStats: this.state.shardStats
      });
      return D.table(
        { cellPadding: 0, cellSpacing: 0, className: 'table' },
        tableHead,
        tableBody
      );
    }
  });
});
