define(function (require) {
  var _ = require('lodash');
  var formatNumber = require('plugins/marvel/lib/format_number');
  var MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');

  return function clusterStatusDataSourceProvider() {
    function ClusterStatusDataSource(index, cluster, clusters) {
      MarvelDataSource.call(this, index, cluster);
      this.clusters = clusters;
    }

    ClusterStatusDataSource.prototype = new MarvelDataSource();

    ClusterStatusDataSource.prototype.initSearchSource = function () {
      this.searchSource.set('size', 1);
      this.searchSource.set('sort', {'@timestamp': { order: 'desc' }});
      this.searchSource.set('query', '_type:cluster_stats');
    };

    ClusterStatusDataSource.prototype.handleResponse = function (resp) {
      this.data = {};
      if (resp.hits.total === 0) return;
      var source = resp.hits.hits[0]._source;
      function get(key) {
        return _.deepGet(source, key);
      }
      var replication = get('indices.shard.replication');
      this.data.status = get('status');
      this.data.cluster_name = get('cluster_name');
      this.data.nodes_count = get('nodes.count.total');
      this.data.total_shards = get('indices.shards.total') || 0;
      this.data.primary_shards = get('indices.shards.primaries') || 0;
      this.data.replica_shards = this.data.total_shards - this.data.primary_shards;
      this.data.document_count = formatNumber(get('indices.docs.count'), 'int_commas');
      // TODO check this one, probably wrong
      this.data.data = formatNumber(get('nodes.fs.total_in_bytes'), 'byte');
      this.data.upTime = formatNumber(get('nodes.jvm.max_uptime_in_millis'), 'time_since');
      this.data.version = get('nodes.versions[0]');
    };
    return ClusterStatusDataSource;

  };
});
