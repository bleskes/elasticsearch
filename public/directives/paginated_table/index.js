'use strict';
define(function(require) {
  var React = require('react');
  var _ = require('lodash');
  var $ = require('jquery');

  var Table = require('plugin/marvel/directives/paginated_table/components/table');

  var module = require('modules').get('marvel/directives', []);

  module.directive('kbPaginatedTable', function() {
    var directiveDefinition = {
      restrict: 'E',
      scope: {
        data: '=',
        options: '='
      },
      link: function($scope, $el, attrs) {
        var $table = React.createElement(Table, {scope: $scope});
        React.render($table, $el[0]);
        return;
      }
    };
  });
});
