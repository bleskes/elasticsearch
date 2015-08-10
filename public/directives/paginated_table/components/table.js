'use strict';
define(function(require) {
  var React = require('react');
  var TableHead = require('./tableHead');
  var TableBody = require('./tableBody');
  var Pagination = require('./pagination');
  var make = React.DOM;


  function getFilteredData(data, filter) {
    return data.filter(function(obj) {
      var concatValues = _.values(obj).join('|');
      return (concatValues.indexOf(filter) !== -1);
    });
  }

  var Table = React.createClass({
    displayName: 'Table',
    getInitialState: function () {
      return {
        itemsPerPage: 20,
        pageIdx: 0,
        dataKeys: [],
        sortColObj: null,
        filter: '',
        title: 'Kb Paginated Table!',
        template: null,
        tableData: null
      };
    },
    componentWillMount: function() {
      this.props.scope.$watch('data', this.setData);
      this.props.scope.$watch('options', this.setOptions);
    },
    setData: function(data) {
      if (data) {
        if(!data.length) {
          data = null;
        }
        this.setState({tableData: data});
      }
    },
    setOptions: function(opts) {
      if (opts) {
        if (opts.dataKeys) {
          opts.sortColObj = opts.dataKeys.reduce(function(prev, dataKey) {
            return prev || (dataKey.sort !== 0 ? dataKey : null);
          }, null);
        }
        this.setState(opts);
      }
    },
    setSortCol: function(colObj) {
      if (colObj) {
        if (this.state.sortColObj && colObj !== this.state.sortColObj) {
          this.state.sortColObj.sort = 0;
        }
        this.setState({sortColObj: colObj});
      }
    },
    setFilter: function(str) {
      str = str || '';
      this.setState({filter: str, pageIdx: 0});
    },
    setDataKeys: function(dataKeys) {
      this.setState({dataKeys: dataKeys});
    },
    setItemsPerPage: function(num) {
      // Must be all;
      if ( _.isNaN(+num) ) {
        num = 0;
      }
      this.setState({
        itemsPerPage: num,
        pageIdx: 0
      });
    },
    setCurrPage: function(idx) {
      this.setState({pageIdx: idx});
    },
    render: function() {
      var isLoading = (this.state.tableData === null);
      if (isLoading) {
        return make.div({className: 'paginated-table loading'}, 'Loading...');
      }

      // Make the Title Bar
      var $title = make.h3({className: 'pull-left title'}, this.state.title);
      var that = this;
      var $filter = make.input({
        type: 'text',
        className: 'pull-left filter-input',
        placeholder: 'Filter Indices',
        onKeyUp: function(evt) {
          that.setFilter(evt.target.value);
        }
      });
      var filteredTableData = getFilteredData(this.state.tableData, this.state.filter);
      var viewingCount = Math.min(filteredTableData.length, this.state.itemsPerPage);
      var $count = make.div(null, viewingCount + ' of ' + this.state.tableData.length);
      var $titleBar = make.div({className: 'title-bar'}, $title, $filter, $count, make.div({className: 'clearfix'}));


      // Make the Table
      var $tableHead = React.createElement(TableHead, {
        setSortCol: this.setSortCol,
        dataKeys: this.state.dataKeys,
        sortColObj: this.state.sortColObj
      });
      var $tableBody = React.createElement(TableBody, {
        tableData: filteredTableData,
        dataKeys: this.state.dataKeys,
        sortColObj: this.state.sortColObj,
        pageIdx: this.state.pageIdx,
        itemsPerPage: this.state.itemsPerPage,
        template: this.props.template
      });
      var $table = React.createElement('table', {className: 'table'},
        $tableHead,
        $tableBody);

      // Footer
      var $pagination = React.createElement(Pagination, {
        dataLength: filteredTableData.length,
        itemsPerPage: this.state.itemsPerPage,
        pageIdx: this.state.pageIdx,
        setCurrPage: this.setCurrPage,
        setItemsPerPage: this.setItemsPerPage
      });


      // Finally wrap it all up and add it to a wrapping div
      return React.createElement('div', {className: 'paginated-table'},
        $titleBar,
        $table,
        $pagination);
    }
  });
  return Table;
});
