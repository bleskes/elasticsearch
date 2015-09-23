const mod = require('ui/modules').get('marvel/directives', []);
const template = require('plugins/marvel/directives/google_analytics/index.html');
mod.directive('googleAnalytics', (reportStats, features) => {
  return {
    restrict: 'E',
    scope: {},
    template: template,
    link(scope) {
      if (reportStats && features.isEnabled('report', true)) {
        scope.allowReport = true;
      }
    }
  };
});
