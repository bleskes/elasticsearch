define(function (require) {
  var _ = require('lodash');
  var MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');

  return function nodesDataSourceProvider() {
    function NodesDataSource(index, cluster, clusters) {
      MarvelDataSource.call(this, index, cluster);
      this.clusters = clusters;
    }

    NodesDataSource.prototype = new MarvelDataSource();

    NodesDataSource.prototype.initSearchSource = function () {
      this.searchSource.set('size', 10000);
      this.searchSource.set('sort', {'timestamp': { order: 'desc' }});
      this.searchSource.set('query', '_type:node_stats');
    };

    NodesDataSource.prototype.handleResponse = function (resp) {
      if (resp.hits.total === 0) return;
      this.data = resp.hits.hits.map(function (hit) {
        var source = hit._source;
        return {
          name: _.get(source, 'node_stats.node_id'),
          address: _.get(source, 'node_stats.node_id'),
          ram: _.get(source, 'node_stats.os.mem.actual_used_in_bytes'),
          upTime: _.get(source, 'node_stats.os.uptime_in_millis')
        };
      });
    };
    return NodesDataSource;

  };
});
