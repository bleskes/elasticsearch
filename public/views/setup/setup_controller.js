define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  var module = require('modules').get('marvel', []);

  require('routes').when('/marvel/setup', {
    template: require('text!marvel/views/setup/setup_template.html')
  });

});
