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
  'use strict';
  var React = require('react');
  var D = React.DOM;
  var Unassigned = React.createFactory(require('./unassigned'));
  var Assigned = React.createFactory(require('./assigned'));
  var ParentItem = React.createFactory(require('./parentItem'));

  return React.createClass({
    displayName: 'TableBody',
    createRow: function (data) {
      return D.tr(null,
        ParentItem({ data: data, key: data.name, shardStats: this.props.shardStats }),
        data.unassigned && data.unassigned.length > 0 ? Unassigned({ shards: data.unassigned }) : this.props.cols === 3 ? D.td() : null,
        Assigned({ shardStats: this.props.shardStats, data: data.children })
      );
    },
    render: function () {
      var rows, message;
      if (this.props.totalCount === 0) {
        message = D.div(null,
          D.p({ style: { margin: '10px 0 0 0'  }, className: 'text-center lead'  },
            'Where\'s the data? It looks like you don\'t have any indexes in your cluster (or they are not visible).'
          ),
          D.div({ className: 'text-center', style: { margin: '0 0 10px 0'  }  },
            'Marvel indexes are hidden by default, click the "cog" icon on this panel and ensure "show hidden indices" is checked.'
          )
        );
        return D.tbody(null,
          D.tr(null, D.td({ colSpan: this.props.cols }, message))
        );
      }

      if (this.props.shardStats) {
        rows = this.props.rows.map(this.createRow);
        if (rows.length) {
          return D.tbody(null, rows);
        }
      }

      return D.tbody(null,
        D.tr(null, D.td({ colSpan: this.props.cols }))
      );

    }
  });
});

