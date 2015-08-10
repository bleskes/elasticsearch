define(function (require) {
  var module = require('ui/modules').get('marvel/filters', []);
  var _ = require('lodash');

  module.filter('capitalize', function () {
    return function (input) {
      return _.capitalize(input);
    };
  });

});

