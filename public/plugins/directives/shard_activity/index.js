define(function (require) {
  var template = require('text!marvel/plugins/directives/shard_activity/index.html');
  var module = require('modules').get('marvel/directives', []);
  module.directive('marvelShardActivity', function () {
    return {
      restrict: 'E',
      scope: {},
      template: template
    };
  });
});



