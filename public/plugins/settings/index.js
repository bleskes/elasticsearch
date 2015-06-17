define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var metrics = require('marvel/lib/metrics');

  var module = require('modules').get('marvel', [
    'marvel/directives'
  ]);

  require('routes')
  .when('/marvel/settings', {
    template: require('text!marvel/plugins/settings/index.html')
  });

  module.controller('settings', function ($scope) {
    $scope.metrics = _.map(metrics, function (val, key) {
      var row = _.cloneDeep(val);
      row.field = key;
      return row;
    });
  });

});
