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
      this.searchSource.set('sort', {'timestamp': { order: 'desc' }});
      this.searchSource.set('query', '_type:marvel_cluster_stats');
    };


    ClusterStatusDataSource.prototype.handleResponse = function (resp) {
      this.data = {};
      if (resp.hits.total === 0) return;
      var source = resp.hits.hits[0]._source;
      function get(key) {
        return _.get(source, key);
      }
      this.data.cluster_uuid = get('cluster_uuid');
      this.data.nodes_count = get('cluster_stats.nodes.count.total');
      this.data.indices_count = get('cluster_stats.indices.count');
      this.data.total_shards = get('cluster_stats.indices.shards.total') || 0;
      this.data.document_count = formatNumber(get('cluster_stats.indices.docs.count'), 'int_commas');
      this.data.data = formatNumber(get('cluster_stats.indices.store.size_in_bytes'), 'byte');
      this.data.upTime = formatNumber(get('cluster_stats.nodes.jvm.max_uptime_in_millis'), 'time_since');
      this.data.version = get('cluster_stats.nodes.versions');
      this.data.memUsed = get('cluster_stats.nodes.jvm.mem.heap_used_in_bytes');
      this.data.memMax = get('cluster_stats.nodes.jvm.mem.heap_max_in_bytes');
    };
    return ClusterStatusDataSource;

  };
});
