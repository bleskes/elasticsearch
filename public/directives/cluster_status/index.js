define(function (require) {
  var template = require('plugins/marvel/directives/cluster_status/index.html');
  var module = require('ui/modules').get('marvel/directives', []);

  module.directive('marvelClusterStatus', function () {
    return {
      restrict: 'E',
      template: template
    };
  });
});
