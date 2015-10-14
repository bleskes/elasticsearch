define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var chrome = require('ui/chrome');
  var module = require('ui/modules').get('marvel', [
    'marvel/directives'
  ]);
  // var chrome = require('ui/chrome');
  require('ui/routes')
  .when('/home', {
    template: require('plugins/marvel/views/home/home_template.html'),
    resolve: {
      clusters: function (Private, marvelClusters, kbnUrl, globalState) {
        var phoneHome = Private(require('plugins/marvel/lib/phone_home'));
        return marvelClusters.fetch().then(function (clusters) {
          var cluster;
          if (!clusters.length) {
            kbnUrl.changePath('/no-data');
            return Promise.reject();
          }
          if (clusters.length === 1) {
            cluster = clusters[0];
            globalState.cluster = cluster.cluster_uuid;
            if (cluster.license.type === 'basic') {
              globalState.save();
              kbnUrl.changePath('/overview');
              return Promise.reject();
            }
          }
          chrome.setTabs([]);
          return clusters;
        }).then(function (clusters) {
          return phoneHome.sendIfDue(clusters).then(function () {
            return clusters;
          });
        });
      }
    }
  })
  .otherwise({ redirectTo: '/no-data' });

  module.controller('home', function ($route, $window, $scope, marvelClusters, timefilter, $timeout, $executor) {

    // Set the key for as the cluster_uuid. This is mainly for
    // react.js so we can use the key easily.
    function setKeyForClusters(cluster) {
      cluster.key = cluster.cluster_uuid;
      return cluster;
    }

    // Set the inital value for the clusters (and map it with setKeyForClusters)
    $scope.clusters = $route.current.locals.clusters.map(setKeyForClusters);

    // Enable the timefilter
    timefilter.enabled = true;

    // Register the marvelClusters service.
    $executor.register({
      execute: function () {
        return marvelClusters.fetch();
      },
      handleResponse: function (clusters) {
        $scope.clusters = clusters.map(setKeyForClusters);
      }
    });

    // Start the executor
    $executor.start();

    // Destory the executor
    $scope.$on('$destroy', $executor.destroy);

  });

});

