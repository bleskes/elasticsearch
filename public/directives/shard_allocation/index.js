const labels = require('plugins/marvel/directives/shard_allocation/lib/labels');
const indicesByNodes = require('plugins/marvel/directives/shard_allocation/transformers/indicesByNodes');
const nodesByIndices = require('plugins/marvel/directives/shard_allocation/transformers/nodesByIndices');
const countChildren = require('plugins/marvel/directives/shard_allocation/lib/countChildren');
const app = require('ui/modules').get('marvel/directives', []);
require('plugins/marvel/directives/shard_allocation/directives/clusterView');
app.directive('marvelShardAllocation', () => {
  return {
    restrict: 'E',
    template: require('plugins/marvel/directives/shard_allocation/index.html'),
    scope: {
      view: '@',
      shards: '=',
      nodes: '=',
      shardStats: '='
    },
    link: (scope, el, attrs) => {
      const transformer = (scope.view === 'index') ? indicesByNodes(scope) : nodesByIndices(scope);
      scope.$watch('shards', (shards) => {
        let view = scope.view;
        scope.totalCount = shards.length;
        scope.showing = transformer(scope.shards, scope.nodes);
        if (view === 'index' && shards.some((shard) => shard.state === 'UNASSIGNED')) {
          view += 'WithUnassigned';
        }
        scope.labels = labels[view];
      });
    }
  };
});
