const mod = require('ui/modules').get('marvel/directives', []);
const template = require('plugins/marvel/directives/index_summary/index.html');
mod.directive('marvelIndexSummary', () => {
  return {
    restrict: 'E',
    template: template,
    scope: { summary: '=' }
  };
});

