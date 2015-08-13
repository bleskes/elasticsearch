define(function (require) {
  var _ = require('lodash');
  var numeral = require('numeral');
  var moment = require('moment');
  var module = require('ui/modules').get('marvel/directives', []);
  var React = require('react');
  var make = React.DOM;


  var Table = require('plugins/marvel/directives/paginated_table/components/table');


  module.directive('marvelNodesListing', function () {
    function makeTdWithPropKey(propKey, idx) {
      return make.td({key: idx}, this.props[propKey]);
    }
    var initialTableOptions = {
      title: 'Nodes',
      dataKeys: [{
        key: 'name',
        sort: 1,
        title: 'Name'
      }, {
        key: 'address',
        sort: 0,
        title: 'IP Address'
      }, {
        key: 'ram',
        sort: 0,
        title: 'RAM used'
      }, {
        key: 'upTime',
        sort: 0,
        title: 'Time Alive'
      }]
    };
    return {
      restrict: 'E',
      scope: { data: '=' },
      link: function ($scope, $el) {
        var tableRowTemplate = React.createFactory(React.createClass({
          render: function() {
            var dataProps = _.pluck(initialTableOptions.dataKeys, 'key');
            var boundTemplateFn = makeTdWithPropKey.bind(this);
            var $tdsArr = dataProps.map(boundTemplateFn);
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

