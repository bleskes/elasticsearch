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
      clusters: function (marvelClusters, kbnUrl, globalState) {
        return marvelClusters.fetch().then(function (clusters) {
          var license;
          var cluster;
          if (clusters.length === 1) {
            cluster = clusters[0];
            globalState.cluster = cluster.cluster_uuid;
            license = _.find(cluster.licenses, { feature: 'marvel' });
            if (license.type === 'lite') {
              globalState.save();
              kbnUrl.changePath('/overview');
              return Promise.reject();
            }
          }
          chrome.setTabs([]);
          return clusters;
        });
      }
    }
  })
  .otherwise({ redirectTo: '/home' });

  module.controller('home', function ($route, $window, $scope, marvelClusters, timefilter, $timeout) {

    function setKeyForClusters(cluster) {
      cluster.key = cluster.cluster_uuid;
      return cluster;
    }

    $scope.clusters = $route.current.locals.clusters.map(setKeyForClusters);

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    var fetchTimer;
    function startFetchInterval() {
      if (!timefilter.refreshInterval.pause) {
        fetchTimer = $timeout(fetch, timefilter.refreshInterval.value);
      }
    }
    function cancelFetchInterval() {
      $timeout.cancel(fetchTimer);
    }

    timefilter.on('update', (time) => {
      cancelFetchInterval();
      startFetchInterval();
    });

    function fetch() {
      marvelClusters.fetch().then((clusters) => {
        $scope.clusters = clusters.map(setKeyForClusters);
        startFetchInterval();
      });
    }

    startFetchInterval();
    $scope.$on('$destroy', () => {
      cancelFetchInterval();
    });

  });

});

