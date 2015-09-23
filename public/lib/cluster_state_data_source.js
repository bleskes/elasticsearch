var _ = require('lodash');
var MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');
var calculateShardStats = require('./calculate_shard_stats');
module.exports = function clusterStateDataSourceProvider(courier) {

  function ClusterStateDataSource(options) {
    MarvelDataSource.call(this, options.indexPattern, options.cluster);
    this.metric = options.metric;
    this.filters = options.filters || [];
    this.data = {};
  }

  ClusterStateDataSource.prototype = new MarvelDataSource();

  ClusterStateDataSource.prototype.initSearchSource = function () {
    this.searchSource.set('sort', { 'timestamp': { order: 'desc' } });
    this.searchSource.set('size', 1);
  };

  ClusterStateDataSource.prototype.getFilters = function () {
    var id = (_.isObject(this.cluster)) ? this.cluster.cluster_uuid : this.cluster;
    return [
      { term: { 'cluster_uuid': id } },
      { term: { _type: 'cluster_state' } }
    ];
  };

  ClusterStateDataSource.prototype.handleResponse = function (resp) {
    if (resp.hits.total) {
      var newState = resp.hits.hits[0]._source;
      if (this.data.timestamp !== newState.timestamp) {
        this.data.timestamp = newState.timestamp;
        _.assign(this.data, newState.cluster_state);
      }
    }
  };

  ClusterStateDataSource.prototype.handleError = function (err) {
    this.data = {};
  };

  return ClusterStateDataSource;

};
