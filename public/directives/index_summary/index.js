const mod = require('ui/modules').get('marvel/directives', []);
const template = require('plugins/marvel/directives/index_summary/index.html');
mod.directive('marvelIndexSummary', () => {
  return {
    restrict: 'E',
    template: template,
    scope: { source: '=', cluster: '=' },
    link: function (scope, el, attr) {
      scope.$watch('cluster', function (cluster) {
        var shardStats;
        if (cluster) {
          shardStats = cluster.shardStats[scope.source.indexName];
          if (shardStats) {
            scope.status = shardStats.status;
            scope.totalShards = shardStats.primary + shardStats.replica;
            scope.unassignedShards = shardStats.unassigned.primary + shardStats.unassigned.replica;

          }
        }
      });
    }
  };
});

