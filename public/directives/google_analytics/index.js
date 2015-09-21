const mod = require('ui/modules').get('marvel/directives', []);
const template = require('plugins/marvel/directives/google_analytics/index.html');
mod.directive('googleAnalytics', () => {
  return {
    restrict: 'A',
    scope: {},
    template: template
  };
});
