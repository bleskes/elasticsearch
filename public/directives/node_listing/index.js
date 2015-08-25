define(function (require) {
  var _ = require('lodash');
  var numeral = require('numeral');
  var moment = require('moment');
  var module = require('ui/modules').get('marvel/directives', []);
  var React = require('react');
  var make = React.DOM;
  var metrics = require('plugins/marvel/lib/metrics');


  var Table = require('plugins/marvel/directives/paginated_table/components/table');
  var MarvelChart = require('plugins/marvel/directives/chart/chart_component');
  var ToggleOnClickComponent = require('plugins/marvel/directives/node_listing/toggle_on_click_component');


  // change the node to actually display the name
  module.directive('marvelNodesListing', function () {
    // makes the tds for every <tr> in the table
    function makeTdWithPropKey(dataKey, idx) {
      var value = _.get(this.props, dataKey.key);
      var $content = null;
      if (dataKey.key === 'name') {
        $content = make.div(null,
          make.a({href: '#/node/' + value}, value),
          make.div({className: 'small'}, '192.168.1.1'));
      }
      if (_.isObject(value) && value.metric) {
        var formatNumber = (function(metric) {
          return function(val) {
            if (!metric.format) { return val; }
            return numeral(val).format(metric.format) + metric.units;
          }
        }(value.metric));
        var metric = value.metric;
        var rawValue = formatNumber(value.last);
        $content = make.div(null,
          make.div({className: 'big inline'}, rawValue),
          make.i({className: 'inline big fa fa-long-arrow-' + (value.slope > 0 ? 'up' : 'down')}),
          make.div({className: 'inline'},
            make.div({className: 'small'}, formatNumber(value.max) + ' max'),
            make.div({className: 'small'}, formatNumber(value.min) + ' min')));
      }
      return make.td({key: idx}, $content);
    }
    var initialTableOptions = {
      title: 'Nodes',
      columns: [{
        key: 'name',
        sort: 1,
        title: 'Name'
      }, {
        key: 'metrics.node_jvm_mem_percent',
        sortKey: 'metrics.node_jvm_mem_percen.lastt',
        sort: 0,
        title: 'Load 1m'
      }, {
        key: 'metrics.load_average_1m',
        sortKey: 'metrics.load_average_1m.last',
        sort: 0,
        title: 'RAM used'
      }, {
        key: 'metrics.node_space_free',
        sortKey: 'metrics.node_space_free.last',
        sort: 0,
        title: 'Disk Free Space GB'
      }]
    };
    function makeChart(data, metric) {
      return React.createElement(MarvelChart, {
        className: 'col-md-4 marvel-chart no-border',
        data: data,
        source: {metric: metric}
      });
    }
    return {
      restrict: 'E',
      scope: { data: '=' },
      link: function ($scope, $el) {
        var tableRowTemplate = React.createClass({
          render: function() {
            var boundTemplateFn = makeTdWithPropKey.bind(this);
            var $tdsArr = initialTableOptions.columns.map(boundTemplateFn);
            var trAttrs = {
              key: 'stats',
              className: 'big'
            };
            var that = this;
            var $chartsArr = _.keys(this.props.metrics).map(function(key) {
              var source = that.props.metrics[key];
              return makeChart(source.data, source.metric);
            });
            var numCols = initialTableOptions.columns.length;
            return make.tr({className: 'big no-border', key: 'row-' + this.props.name},
              $tdsArr);
            /*
            return make.tr({className: 'big no-border', key: 'row-' + this.props.name},
              make.td({colSpan: numCols, key: 'table-td-wrap'},
                make.table({className: 'nested-table', key: 'table'},
                  React.createElement(ToggleOnClickComponent, {
                    elWrapper: 'tbody',
                    activator: make.tr(trAttrs, $tdsArr),
                    content: make.tr({key: 'charts'}, make.td({colSpan: numCols}, $chartsArr))
                  }))));*/
          }
        });

        var $table = React.createElement(Table, {
          options: initialTableOptions,
          data: $scope.data,
          template: tableRowTemplate
        });

        var TableInstance = React.render($table, $el[0]);

        $scope.$watch('data', function(data, oldVal) {
          TableInstance.setData(data);
        });
      }
    };
  });
});

