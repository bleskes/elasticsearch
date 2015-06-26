define(function (require) {
  var template = require('text!marvel/directives/cluster_status/index.html');
  var module = require('modules').get('marvel/directives', []);

  module.directive('marvelClusterStatus', function () {
    return {
      restrict: 'E',
      template: template,
      scope: {
        source: '='
      }
    };
  });

});
