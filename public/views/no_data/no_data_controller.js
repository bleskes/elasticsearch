const _ = require('lodash');
const angular = require('angular');
const chrome = require('ui/chrome');
const mod = require('ui/modules').get('marvel', [
  'marvel/directives'
]);
require('ui/routes')
.when('/no-data', {
  template: require('plugins/marvel/views/no_data/no_data_template.html'),
  resolve: {
    clusters: (marvelClusters, kbnUrl, Promise, globalState) => {
      return marvelClusters.fetch().then((clusters) => {
        if (clusters.length) {
          kbnUrl.changePath('/home');
          return Promise.reject();
        }
        chrome.setTabs([]);
        return Promise.resolve();
      });
    }
  }
})
.otherwise({ redirectTo: '/home' });

mod.controller('noData', (kbnUrl, $scope, marvelClusters, timefilter, $timeout) => {

  timefilter.enabled = true;
  if (timefilter.refreshInterval.value === 0) {
    timefilter.refreshInterval.value = 10000;
    timefilter.refreshInterval.display = '10 Seconds';
  }

  let fetchTimer;
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

