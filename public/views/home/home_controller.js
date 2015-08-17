define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var module = require('ui/modules').get('marvel', [
    'marvel/directives'
  ]);
  var chrome = require('ui/chrome');
  require('ui/routes')
  .when('/home', {
    template: require('plugins/marvel/views/home/home_template.html'),
    resove: {
      tabs: function (arg) {
        chrome.setTabs([]);
      }
    }
  })
  .otherwise({ redirectTo: '/home' });

  module.controller('home', function ($scope, marvelClusters, timefilter, $timeout) {

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }

    function fetch() {
      marvelClusters.fetch().then((clusters) => {
        $scope.clusters = clusters;
      });
      $timeout(fetch, timefilter.refreshInterval.value);
    }

    fetch();

  });

});

