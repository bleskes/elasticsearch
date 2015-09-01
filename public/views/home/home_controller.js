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
          if (clusters.length === 1) {
            globalState.cluster = clusters[0].cluster_uuid;
            globalState.save();
            kbnUrl.changePath('/overview');
            return Promise.reject();
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

    var hideBanner = $window.localStorage.getItem('marvel.hideBanner');
    $scope.showBanner = (hideBanner) ? false : true;

    $scope.hideBanner = function () {
      $scope.showBanner = false;
    };

    $scope.dontShowAgain = function () {
      $scope.showBanner = false;
      $window.localStorage.setItem('marvel.hideBanner', 1);
    };

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

