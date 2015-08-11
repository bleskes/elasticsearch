define(function (require) {
  var _ = require('lodash');
  var numeral = require('numeral');
  var moment = require('moment');
  var module = require('modules').get('marvel/directives', []);
  var React = require('react');
  var make = React.DOM;


  var Table = require('marvel/directives/paginated_table/components/table');
  var SparkLines = require('marvel/directives/marvel_sparkline');


  module.directive('marvelNodesListing', function () {
    function makeTdWithPropKey(dataKey, idx) {
      var value = _.get(this.props, dataKey.key);
      var chartData = _.get(this.props, dataKey.chart_data);
      var has_chart = !!dataKey.chart_data;
      return make.td({key: idx},
        make.div({className: (has_chart ? 'pull-right': '')}, value),
        (has_chart ? React.createElement(SparkLines, {data: chartData}) : null));

    }
    // FIXME make it so this pulls from the metrics list
    var initialTableOptions = {
      title: 'Nodes',
      dataKeys: [{
        key: 'name',
        sort: 1,
        title: 'Name'
      }, {
        key: 'metrics.node_cpu_usage.last',
        chart_data: 'metrics.node_cpu_usage.data',
        sort: 0,
        title: 'CPU Usage'
      }, {
        key: 'metrics.node_heap_used.last',
        chart_data: 'metrics.node_heap_used.data',
        sort: 0,
        title: 'Heap Used'
      }, {
        key: 'metrics.node_load.last',
        chart_data: 'metrics.node_load.data',
        sort: 0,
        title: 'CPU Usage'
      }]
    };
    return {
      restrict: 'E',
      scope: { data: '=' },
      link: function ($scope, $el) {
        var tableRowTemplate = React.createFactory(React.createClass({
          render: function() {
            var boundTemplateFn = makeTdWithPropKey.bind(this);
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

