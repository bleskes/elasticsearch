define(function (require) {
  var _ = require('lodash');
  var numeral = require('numeral');
  var moment = require('moment');
  var module = require('ui/modules').get('marvel/directives', []);
  var React = require('react');
  var make = React.DOM;

  var SparkLines = require('plugins/marvel/directives/marvel_sparkline');


  var Table = require('plugins/marvel/directives/paginated_table/components/table');


  module.directive('marvelIndexListing', function () {
    function makeSampleData() {
      var sampleData = (function () {
        var arr = [];
        var length = 30;
        var now = (new Date()).getTime();
        for (var i = 0; i < length; i++) {
          arr.push({y: Math.random() * 100, x: (now - ((length - i) * 10))});
        }
        return arr;
      }());
      return sampleData;
    }
    function makeTdWithPropKey(dataKey, idx) {
      var value = _.get(this.props, dataKey.key);
      if (dataKey.key === 'name') {
        value = make.a({ href: `#/index/${value}` }, value);
      }
      if (_.isObject(value) && value.metric) {
        value = (value.metric.format) ? numeral(value.last).format(value.metric.format) : value.last;
      }
      var chartData = _.get(this.props, dataKey.chart_data);
      var hasChart = !!dataKey.chart_data;
      return make.td({key: idx},
        (hasChart ? React.createElement(SparkLines, {data: chartData}) : null),
        make.div({className: (hasChart ? 'pull-right chart-val' : '')}, value)
      );
    }
    var initialTableOptions = {
      title: 'Indices',
      columns: [{
        key: 'name',
        sort: 1,
        title: 'Name'
      }, {
        key: 'metrics.index_document_count',
        sortKey: 'metrics.index_document_count.last',
        sort: 0,
        title: 'Document Count'
      }, {
        key: 'metrics.index_request_rate',
        sort: 0,
        sortKey: 'metrics.index_request_rate.last',
        // chart_data: 'metrics.index_request_rate.data',
        title: 'Index Rate'
      }, {
        key: 'metrics.index_search_request_rate',
        sort: 0,
        sortKey: 'metrics.index_search_request_rate.last',
        // chart_data: 'metrics.index_search_request_rate.data',
        title: 'Search Rate'
      }, {
        key: 'metrics.index_merge_rate',
        sort: 0,
        sortKey: 'metrics.index_merge_rate.last',
        title: 'Merge Rate',
      }]
    };
    return {
      restrict: 'E',
      scope: { data: '=' },
      link: function ($scope, $el) {
        var tableRowTemplate = React.createClass({
          render: function () {
            var boundTemplateFn = makeTdWithPropKey.bind(this);
            var dataProps = _.pluck(initialTableOptions.columns, 'key');
            var $tdsArr = initialTableOptions.columns.map(boundTemplateFn);
            return make.tr({key: this.props.name}, $tdsArr);
          }
        });

        var tableFactory = React.createFactory(Table);

        var table = React.render(tableFactory({
          scope: $scope,
          options: initialTableOptions,
          template: tableRowTemplate
        }), $el[0]);

        $scope.$watch('data', (data) => table.setData(data));
      }
    };
  });
});

