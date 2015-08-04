define(function (require) {
  var _ = require('lodash');
  var numeral = require('numeral');
  var moment = require('moment');
  var module = require('modules').get('marvel/directives', []);
  var React = require('react');
  var make = React.DOM;

  var SparkLines = require('plugin/marvel/directives/marvel_sparkline');


  var Table = require('plugin/marvel/directives/paginated_table/components/table');


  module.directive('marvelIndexListing', function () {
    function makeSampleData() {
      var sampleData = (function() {
        var arr = [];
        var length = 30;
        var now = (new Date()).getTime();
        for( var i = 0; i < length; i++ ) {
          arr.push({y: Math.random() * 100, x: (now - ((length-i) * 10))});
        }
        return arr;
      }());
      return sampleData;
    }
    function calcSlope(data) {
      var length = data.length;
      var xSum = data.reduce(function(prev, curr) { return prev + curr.x; }, 0);
      var ySum = data.reduce(function(prev, curr) { return prev + curr.y; }, 0);
      var xySum = data.reduce(function(prev, curr) { return prev + (curr.y * curr.x); }, 0);
      var xSqSum = data.reduce(function(prev, curr) { return prev + (curr.x * curr.x); }, 0);
      var numerator = (length * xySum) - (xSum * ySum);
      var denominator = (length * xSqSum) - (xSum * ySum);
      return numerator / denominator;
    }
    function makeTdWithPropKey(dataKey, idx) {
      var value = _.get(this.props, dataKey.key);
      var chartData = _.get(this.props, dataKey.chart_data);
      var has_chart = !!dataKey.chart_data;
      if (has_chart) {
        value = calcSlope(chartData);
      }
      return make.td({key: idx},
        make.div({className: (has_chart ? 'pull-right': '')}, value),
        (has_chart ? React.createElement(SparkLines, {data: chartData}) : null));
    }
    var initialTableOptions = {
      title: 'Indices',
      dataKeys: [{
        key: 'name',
        sort: 1,
        title: 'Name'
      }, {
        key: 'metrics.index_document_count.last',
        sort: 0,
        title: 'Document Count'
      }, {
        key: 'metrics.index_request_rate.last',
        sort: 0,
        chart_data: 'metrics.index_request_rate.data',
        title: 'Index Rate'
      }, {
        key: 'metrics.index_search_request_rate.last',
        sort: 0,
        chart_data: 'metrics.index_search_request_rate.data',
        title: 'Search Rate'
      }, {
        key: 'metrics.index_merge_rate.last',
        sort: 0,
        title: 'Merge Rate',
      }]
    };
    return {
      restrict: 'E',
      scope: { data: '=' },
      link: function ($scope, $el) {
        var tableRowTemplate = React.createFactory(React.createClass({
          render: function() {
            var boundTemplateFn = makeTdWithPropKey.bind(this);
            var dataProps = _.pluck(initialTableOptions.dataKeys, 'key');
            var $tdsArr = initialTableOptions.dataKeys.map(boundTemplateFn);
            return make.tr({key: this.props.name}, $tdsArr);
          }
        }));

        $scope.options = initialTableOptions;

        var $table = React.createElement(Table, {
          scope: $scope,
          template: tableRowTemplate
        });

        React.render($table, $el[0]);
      }
    };
  });
});

