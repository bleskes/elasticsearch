define(function (require) {
  var _ = require('lodash');
  var formatNumber = require('marvel/lib/format_number');
  var MarvelDataSource = require('marvel/lib/marvel_data_source');

  return function shardRecoveryDataSourceProvider() {
    function ShardRecoveryDataSource(index, cluster) {
      MarvelDataSource.call(this, index, cluster);
    }

    ShardRecoveryDataSource.prototype = new MarvelDataSource();

    ShardRecoveryDataSource.prototype.initSearchSource = function () {
      this.searchSource.set('size', 1);
      this.searchSource.set('sort', {'@timestamp': { order: 'desc' }});
      this.searchSource.set('query', '_type:recovery');
    };
    ShardRecoveryDataSource.prototype.getFilters = function () {
      // return [{ term: { 'cluster_name.raw': 'marvel-2.0' } }];
      return [];
    };

    ShardRecoveryDataSource.prototype.handleResponse = function (resp) {
      if (resp.hits.total === 0) return;
      debugger;
    };
    return ShardRecoveryDataSource;

  };
});
