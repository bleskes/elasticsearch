define(function (require) {
  var template = require('text!marvel/directives/cluster_status/index.html');
  var module = require('modules').get('marvel/directives', []);
  var moment = require('moment');

  module.directive('marvelClusterStatus', function ($location, globalState, kbnUrl, marvelClusters) {
    return {
      restrict: 'E',
      template: template,
      scope: {
        source: '='
      },
      link: function ($scope) {
        var unsubscribe = $scope.$on('courier:searchRefresh', function () {
          marvelClusters.fetch(true).then(function (clusters) {
            $scope.source.clusters = clusters;
          });
        });
        $scope.$on('$destroy', unsubscribe);
        $scope.changeCluster = function (name) {
          if(globalState.cluster !== name) {
            globalState.cluster = name;
            globalState.save();
            kbnUrl.changePath($location.path());
          }
        };

        $scope.laggingCluster = false;

        $scope.$watch('source.clusters', function (clusters) {
          var cluster = _.find(clusters, { _id: $scope.source.cluster });
          $scope.lastUpdate = moment.utc(cluster.lastUpdate);
          var now = moment.utc();
          var diff = now.diff($scope.lastUpdate);
          $scope.lastSeen = moment.duration(diff, 'ms').humanize();
          if (diff > 120000) {
            $scope.laggingCluster = true;
          }
        });
      }
    };
  });

});
