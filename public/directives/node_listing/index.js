define(function (require) {
  var _ = require('lodash');
  var numeral = require('numeral');
  var module = require('ui/modules').get('marvel/directives', []);
  var React = require('react');
  var make = React.DOM;
  var extractIp = require('plugins/marvel/lib/extract_ip');
  var lookups = require('plugins/marvel/lib/lookups');

  var Table = require('plugins/marvel/directives/paginated_table/components/table');

  var nodeTypeClass = lookups.nodeTypeClass;
  var nodeTypeLabel = lookups.nodeTypeLabel;

  // change the node to actually display the name
  module.directive('marvelNodesListing', function () {
    // makes the tds for every <tr> in the table
    function makeTdWithPropKey(dataKey, idx) {
      var value = _.get(this.props, dataKey.key);
      var $content = null;
      if (dataKey.key === 'name') {
        var title = this.props.nodeTypeLabel;
        var classes = 'fa ' + this.props.nodeTypeClass;
        var state = this.state || {};
        $content = make.div(null,
          make.span({
            style: { paddingRight: 5 }
          }, make.i({
            title: title,
            className: classes },
            null)
          ),
          make.a({href: '#/node/' + state.id}, state.name),  // <a href="#/node/:node_id>
          make.div({className: 'small'}, extractIp(state.transport_address))); //   <div.small>
      }
      if (_.isObject(value) && value.metric) {
        var formatNumber = (function (metric) {
          return function (val) {
            if (!metric.format) { return val; }
            return numeral(val).format(metric.format) + metric.units;
          };
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
      if (!$content && !_.isUndefined(value)) {
        $content = make.div(null, make.div({className: 'big inline'}, value));
      }
      return make.td({key: idx}, $content);
    }
    var initialTableOptions = {
      title: 'Nodes',
      searchPlaceholder: 'Filter Nodes',
      /* "key" should be an object
       *   - unless it's the "name" key
       *   - the key object should have:
       *      - "metric" object
       *      - "last" scalar
       * "sortKey" should be a scalar */
      columns: [
        {
          key: 'name',
          sortKey: 'nodeName',
          sort: 1,
          title: 'Name'
        },
        {
          key: 'metrics.node_cpu_utilization',
          sortKey: 'metrics.node_cpu_utilization.last',
          sort: 0,
          title: 'CPU Usage'
        },
        {
          key: 'metrics.node_jvm_mem_percent',
          sortKey: 'metrics.node_jvm_mem_percent.last',
          sort: 0,
          title: 'JVM Memory'
        },
        {
          key: 'metrics.node_load_average',
          sortKey: 'metrics.node_load_average.last',
          sort: 0,
          title: 'Load Average'
        },
        {
          key: 'metrics.node_free_space',
          sortKey: 'metrics.node_free_space.last',
          sort: 0,
          title: 'Disk Free Space'
        },
        {
          key: 'metrics.shard_count',
          sortKey: 'metrics.shard_count',
          sort: 0,
          title: 'Shards'
        }
      ]
    };
    return {
      restrict: 'E',
      scope: { cluster: '=', data: '=', nodes: '='},
      link: function ($scope, $el) {
        var tableRowTemplate = React.createClass({
          getInitialState: function () {
            return $scope.nodes[this.props.id] || null;
          },
          componentWillReceiveProps: function (newProps) {
            this.setState($scope.nodes[newProps.id]);
          },
          render: function () {
            var boundTemplateFn = makeTdWithPropKey.bind(this);
            var $tdsArr = initialTableOptions.columns.map(boundTemplateFn);
            return make.tr({
              className: 'big no-border',
              key: 'row-' + this.props.id
            }, $tdsArr);
          }
        });

        var $table = React.createElement(Table, {
          options: initialTableOptions,
          template: tableRowTemplate
        });

        var tableInstance = React.render($table, $el[0]);

        $scope.$watch('data', function (data) {
          var tableData = data.filter(function (row) {
            return $scope.nodes[row.id];
          }).map(function (row) {
            var node = $scope.nodes[row.id];
            row.metrics.shard_count = node.shardCount;
            row.nodeName = node.name;
            row.nodeType = node.type;
            row.isMaster = node.master;
            var type = row.isMaster && 'master' || row.nodeType;
            row.nodeTypeClass = nodeTypeClass[type];
            row.nodeTypeLabel = nodeTypeLabel[type];
            if (!$scope.cluster.nodes[row.id]) {
              row.nodeTypeLabel = nodeTypeLabel.invalid;
              row.nodeTypeClass = nodeTypeClass.invalid;
            }
            return row;
          });
          tableInstance.setData(tableData);
        });
      }
    };
  });
});
