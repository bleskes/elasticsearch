const _ = require('lodash');
const mod = require('ui/modules').get('marvel/directives', []);
const template = require('plugins/marvel/directives/welcome_msg/index.html');
mod.directive('marvelWelcomeMessage', function ($window, $http, reportStats, features) {
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
        scope.toggleAllowReport = function () {
          features.update('report', !scope.allowReport);
          scope.allowReport = !scope.allowReport;
        };
      }

      scope.hideReport = true;
      scope.viewReport = function () {
        const clusterUuid = _.get(scope, 'clusters[0].cluster_uuid');
        scope.hideReport = !scope.hideReport;
        if (!scope.statReportData) {
          $http.get(`/marvel/api/v1/clusters/${clusterUuid}/info`).then(function (response) {
            scope.statReportData = JSON.stringify(response.data, null, ' ');
          });
        }
      };
    }
  };
});
