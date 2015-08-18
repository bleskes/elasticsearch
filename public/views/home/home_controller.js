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
      tabs: function () {
        chrome.setTabs([]);
      }
    }
  })
  .otherwise({ redirectTo: '/home' });

  module.controller('home', function ($window, $scope, marvelClusters, timefilter, $timeout) {

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

    function fetch() {
      marvelClusters.fetch().then((clusters) => {
        $scope.clusters = clusters;
      });
      $timeout(fetch, timefilter.refreshInterval.value);
    }

    fetch();

  });

});

