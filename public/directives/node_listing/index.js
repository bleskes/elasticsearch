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


  module.directive('marvelNodesListing', function () {
    function makeTdWithPropKey(dataKey, idx) {
      var value = _.get(this.props, dataKey.key);
      var $content = null;
      if (dataKey.key === 'name') {
        $content = make.div(null,
          make.div(null, value),
          make.div({className: 'small'}, '192.168.1.1'));
      }
      if (_.isObject(value) && value.metric) {
        var metric = value.metric;
        var rawValue = (value.metric.format) ? numeral(value.last).format(value.metric.format) : value.last;
        $content = make.div(null,
          make.div({className: 'big inline'}, rawValue),
          make.i({className: 'inline big fa fa-long-arrow-' + (value.slope > 0 ? 'up' : 'down')}),
          make.div({className: 'inline'},
            make.div({className: 'small'}, value.max + ' max'),
            make.div({className: 'small'}, value.min + ' min')));
      }
      return make.td({key: idx}, $content);
    }
    var initialTableOptions = {
      title: 'Nodes',
      dataKeys: [{
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
      }]
    };
    function makeCharts() {
      var count = 3;
      var $charts = [];
      while(--count >= 0) {
        $charts.push(React.createElement(MarvelChart, {
          className: 'col-md-4',
          data: makeSampleData(),
          source: {metric: metrics.load_average_1m}
        }));
      }
      return $charts;
    }
    function makeSampleData() {
      var count = 60;
      var data = [];
      var date = (new Date()).getTime();
      while(--count > 0) {
        data.push({
          x: date + (1000*60)*count,
          y: Math.round(100 * Math.random())
        });
      }
      return data;
    }
    return {
      restrict: 'E',
      scope: { data: '=' },
      link: function ($scope, $el) {
        var tableRowTemplate = React.createFactory(React.createClass({
          render: function() {
            var boundTemplateFn = makeTdWithPropKey.bind(this);
            var $tdsArr = initialTableOptions.dataKeys.map(boundTemplateFn);
            var trAttrs = {
              key: 'stats',
              className: 'big'
            };
            var numCols = initialTableOptions.dataKeys.length;
            var $chartsArr = makeCharts();
            return make.tr({className: 'big no-border', key: 'row-' + this.props.name},
              make.td({colSpan: numCols, key: 'table-td-wrap'},
                make.table({className: 'nested-table', key: 'table'},
                  React.createElement(ToggleOnClickComponent, {
                    elWrapper: 'tbody',
                    activator: make.tr(trAttrs, $tdsArr),
                    content: make.tr({key: 'charts'}, make.td({colSpan: numCols}, $chartsArr))
                  }))));
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
  var ToggleOnClickComponent = React.createClass({
    getInitialState: function() {
      return { visible: this.props.initiallyVisible || false };
    },
    toggleVisibility: function() {
      this.setState({visible: !this.state.visible});
    },
    render: function() {
      var activator = this.props.activator;
      var visible = this.state.visible;
      var content = visible ? this.props.content : null;

      var wrapperStr = this.props.elWrapper || null;
      wrapperStr = wrapperStr.split('.');

      var wrapper = wrapperStr.shift();
      return make[wrapper]({
        className: wrapperStr.join(' '),
        onClick: this.toggleVisibility
      }, activator, content);
    }
  });
});

