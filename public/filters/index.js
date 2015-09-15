define(function (require) {
  var module = require('ui/modules').get('marvel/filters', []);
  var formatNumber = require('plugins/marvel/lib/format_number');
  var _ = require('lodash');

  module.filter('capitalize', function () {
    return function (input) {
      return _.capitalize(input);
    };
  });

  module.filter('formatNumber', function () {
    return function (input, format) {
      return formatNumber(input, format);
    };
  });
});

