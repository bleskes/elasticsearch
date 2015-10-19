define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var chrome = require('ui/chrome');
  var moment = require('moment');
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

  module.controller('home', function ($route, $window, $scope, marvelClusters, timefilter, $timeout) {

    function setKeyForClusters(cluster) {
      cluster.key = cluster.cluster_uuid;
      return cluster;
    }

    function isClusterCurrent(cluster) {
      var lastUpdate = moment(cluster.state_timestamp);
      return lastUpdate.isAfter(moment().subtract(10, 'minutes'));
    }

    $scope.clusters = $route.current.locals.clusters
      .filter(isClusterCurrent)
      .map(setKeyForClusters);

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
        $scope.clusters = clusters
          .filter(isClusterCurrent)
          .map(setKeyForClusters);
        startFetchInterval();
      });
    }

    startFetchInterval();
    $scope.$on('$destroy', () => {
      cancelFetchInterval();
    });

  });

});

