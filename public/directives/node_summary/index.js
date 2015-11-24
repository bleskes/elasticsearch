const mod = require('ui/modules').get('marvel/directives', []);
const template = require('plugins/marvel/directives/node_summary/index.html');
mod.directive('marvelNodeSummary', () => {
  return {
    restrict: 'E',
    template: template,
    scope: { node: '=' }
  };
});
