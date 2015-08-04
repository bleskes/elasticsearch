'use strict';
define(function(require) {
  var _ = require('lodash');
  var React = require('react');
  var make = React.DOM;

  function renderRow(dataKeys, obj, idx) {
    var $objTds = dataKeys.map(function(key, idx) {
      return make.td({key: idx}, _.get(obj, key.key));
    });
    return make.tr({key: idx}, $objTds);
  }
  function makeOneTd(text) {
    var $tr = make.tr(null, make.td(null, text));
    return make.tbody(null, $tr);
  }
  var TableBody = React.createClass({
    displayName: 'TableBody',
    render: function() {
      if (!this.props.tableData) {
        return makeOneTd('Loading...');
      }
      if (!this.props.tableData.length) {
        return makeOneTd('No Data!');
      }
      var that = this;

      // Sort the Data
      var sortColumn = this.props.sortColObj;
      var sortedData = this.props.tableData.sort(function(a, b) {
        var aVal = _.get(a, sortColumn.key);
        var bVal = _.get(b, sortColumn.key);
        var sortDir = sortColumn.sort > 0 ? (aVal < bVal): (aVal > bVal);
        return sortDir ? -1 : 1;
      });

      // Paginate the Data
      var start = this.props.pageIdx * this.props.itemsPerPage;
      var end = start + (this.props.itemsPerPage || sortedData.length);
      var paginatedData = sortedData.slice(start, end);


      // This takes either a React Factory, or Assumes the template from the dataKeys
      var rowMaker = this.props.template || _.partial(renderRow, this.props.dataKeys);

      // Draw the data
      return make.tbody({className: 'tbody'}, paginatedData.map(rowMaker));
    }
  });
  return TableBody;
});
