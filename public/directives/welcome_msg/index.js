const _ = require('lodash');
const mod = require('ui/modules').get('marvel/directives', []);
const template = require('plugins/marvel/directives/welcome_msg/index.html');
mod.directive('marvelWelcomeMessage', function ($window, $rootScope, reportStats, features) {
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
        const license = _.find(scope.cluster.licenses, { feature: 'marvel' });
        if (license.type !== 'lite') {
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
        scope.toggleAllowReport = function (data) {
          features.update('report', data.allowReport);
          scope.$rootScope = scope.allowReport = data.allowReport;
        };
      }

    }
  };
});
