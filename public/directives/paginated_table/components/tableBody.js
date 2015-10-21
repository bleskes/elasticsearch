var _ = require('lodash');
var React = require('react');
var make = React.DOM;
var Loading = require('./loading.jsx');
var NoData = require('./no_data.jsx');

module.exports = React.createClass({
  displayName: 'TableBody',
  render: function () {
    if (!this.props.tableData) {
      return React.createFactory(Loading)({ columns: this.props.columns });
    }
    if (!this.props.tableData.length) {
      return React.createFactory(NoData)({ columns: this.props.columns });
    }
    var that = this;

    // Sort the Data
    var sortColumn = this.props.sortColObj;
    var sortedData = this.props.tableData.sort(function (a, b) {
      var aVal = _.get(a, sortColumn.sortKey || sortColumn.key);
      var bVal = _.get(b, sortColumn.sortKey || sortColumn.key);
      var sortDir = sortColumn.sort > 0 ? (aVal < bVal) : (aVal > bVal);
      return sortDir ? -1 : 1;
    });

    // Paginate the Data
    var start = this.props.pageIdx * this.props.itemsPerPage;
    var end = start + (this.props.itemsPerPage || sortedData.length);
    var paginatedData = sortedData.slice(start, end);
    var template = React.createFactory(this.props.template);

    var createRow = function (row, idx) {
      return template(row, idx);
    };

    // Draw the data
    return make.tbody({className: 'tbody'}, paginatedData.map(createRow));
  }
});
