define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var chrome = require('ui/chrome');
  var module = require('ui/modules').get('marvel', [
    'marvel/directives'
  ]);
  // var chrome = require('ui/chrome');
  require('ui/routes')
  .when('/no-data', {
    template: require('plugins/marvel/views/no_data/no_data_template.html'),
    resolve: {
      clusters: function (marvelClusters, kbnUrl, Promise, globalState) {
        return marvelClusters.fetch().then(function (clusters) {
          if (clusters.length) return Promise.reject();
          chrome.setTabs([]);
          return Promise.resolve();
        });
      }
    }
  })
  .otherwise({ redirectTo: '/home' });

  module.controller('noData', function (kbnUrl, $scope, marvelClusters, timefilter, $timeout) {

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
        if (clusters.length) {
          kbnUrl.changePath('/home');
        }
        startFetchInterval();
      });
    }

    startFetchInterval();
    $scope.$on('$destroy', () => {
      cancelFetchInterval();
    });

  });

});


