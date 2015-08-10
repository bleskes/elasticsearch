define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');

  var module = require('ui/modules').get('marvel', []);

  require('ui/routes').when('/setup', {
    template: require('plugins/marvel/views/setup/setup_template.html')
  });

});
