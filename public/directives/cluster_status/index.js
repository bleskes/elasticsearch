define(function (require) {
  var template = require('text!marvel/directives/cluster_status/index.html');
  var module = require('modules').get('marvel/directives', []);

  module.directive('marvelClusterStatus', function ($location, globalState, kbnUrl) {
    return {
      restrict: 'E',
      template: template,
      scope: {
        source: '='
      },
      link: function ($scope) {
        $scope.changeCluster = function (name) {
          if(globalState.cluster !== name) {
            globalState.cluster = name;
            globalState.save();
            kbnUrl.changePath($location.path());
          }
        };
      }
    };
  });

});
