define(function (require) {
  var React = require('react');
  var _ = require('lodash');
  var TableHead = require('./tableHead');
  var TableBody = require('./tableBody');
  var Pagination = require('./pagination.jsx');
  var make = React.DOM;


  function getFilteredData(data, filter) {
    return data.filter(function (obj) {
      var concatValues = _.values(obj)
        .filter(function (val) { return typeof val === 'string'; })
        .join('|')
        .toLowerCase();
      return (concatValues.indexOf(filter.toLowerCase()) !== -1);
    });
  }

  var Table = React.createClass({
    displayName: 'Table',
    getInitialState: function () {
      var sortColObj = null;
      if (this.props.options.columns) {
        sortColObj = this.props.options.columns.reduce((prev, dataKey) => {
          return prev || (dataKey.sort !== 0 ? dataKey : null);
        }, null);
      }
      return {
        itemsPerPage: 20,
        pageIdx: 0,
        sortColObj: sortColObj,
        filter: '',
        title: 'Kb Paginated Table!',
        template: null,
        tableData: null
      };
    },
    setData: function (data) {
      if (data) {
        if (!data.length) {
          data = null;
        }
        this.setState({tableData: data});
      }
    },
    setSortCol: function (colObj) {
      if (colObj) {
        if (this.state.sortColObj && colObj !== this.state.sortColObj) {
          this.state.sortColObj.sort = 0;
        }
        this.setState({sortColObj: colObj});
      }
    },
    setFilter: function (str) {
      str = str || '';
      this.setState({filter: str, pageIdx: 0});
    },
    setItemsPerPage: function (num) {
      // Must be all;
      if (_.isNaN(+num)) {
        num = 0;
      }
      this.setState({
        itemsPerPage: num,
        pageIdx: 0
      });
    },
    setCurrPage: function (idx) {
      this.setState({pageIdx: idx});
    },
    render: function () {
      var isLoading = (this.state.tableData === null);
      if (isLoading) {
        let nodes = [
          make.i({ className: 'fa fa-spinner fa-pulse' }),
          make.span(null, 'Loading Data...')
        ];
        return make.div({className: 'paginated-table loading'}, nodes);
      }

      // Make the Title Bar
      var $title = make.h3({className: 'pull-left title'}, this.props.options.title);
      var that = this;
      var $filter = make.input({
        type: 'text',
        className: 'pull-left filter-input',
        placeholder: this.props.options.searchPlaceholder,
        onKeyUp: function (evt) {
          that.setFilter(evt.target.value);
        }
      });
      var filteredTableData = getFilteredData(this.state.tableData, this.state.filter);
      var viewingCount = Math.min(filteredTableData.length, this.state.itemsPerPage);
      var $count = make.div(null, viewingCount + ' of ' + this.state.tableData.length);
      var $titleBar = make.div({className: 'title-bar'}, $title, $filter, $count, make.div({className: 'clearfix'}));


      // Make the Table
      var $tableHead = React.createFactory(TableHead);
      var $tableBody = React.createFactory(TableBody);
      var $table = make.table({ key: 'table', className: 'table' },
        $tableHead({
          key: 'table.head',
          setSortCol: this.setSortCol,
          columns: this.props.options.columns,
          sortColObj: this.state.sortColObj
        }),
        $tableBody({
          key: 'table.body',
          tableData: filteredTableData,
          columns: this.props.options.columns,
          sortColObj: this.state.sortColObj,
          pageIdx: this.state.pageIdx,
          itemsPerPage: this.state.itemsPerPage,
          template: this.props.template
        }));

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
