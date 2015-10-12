const _ = require('lodash');
const mod = require('ui/modules').get('marvel/directives', []);
const template = require('plugins/marvel/directives/welcome_msg/index.html');
mod.directive('marvelWelcomeMessage', function ($window, reportStats, features) {
  return {
    restrict: 'E',
    scope: {
      cluster: '=',
      clusters: '='
    },
    template: template,
    link: (scope, el, attrs) => {
      const hideBanner = $window.localStorage.getItem('marvel.hideBanner');
      scope.showBanner = (hideBanner) ? false : true;

      if (scope.showBanner && scope.cluster && scope.clusters) {
        const license = scope.cluster.license;
        if (license.type !== 'basic') {
          scope.showBanner = false;
        }
      }

      scope.hideBanner = function () {
        scope.showBanner = false;
      };

      scope.dontShowAgain = function () {
        scope.showBanner = false;
        $window.localStorage.setItem('marvel.hideBanner', 1);
      };

      scope.reportStats = reportStats;
      if (reportStats) {
        scope.allowReport = features.isEnabled('report', true);
        scope.toggleAllowReport = function () {
          features.update('report', !scope.allowReport);
          scope.allowReport = !scope.allowReport;
        };
      }

    }
  };
});
