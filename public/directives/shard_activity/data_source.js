define(function (require) {
  var _ = require('lodash');
  var formatNumber = require('marvel/lib/format_number');
  var MarvelDataSource = require('marvel/lib/marvel_data_source');

  return function shardRecoveryDataSourceProvider() {

    function ShardRecoveryDataSource(index, cluster) {
      MarvelDataSource.call(this, index, cluster);
      this.data = [];
    }

    ShardRecoveryDataSource.prototype = new MarvelDataSource();

    ShardRecoveryDataSource.prototype.initSearchSource = function () {
      this.searchSource.set('size', 1);
      this.searchSource.set('sort', {'@timestamp': { order: 'desc' }});
      this.searchSource.set('query', '_type:recovery');
    };

    ShardRecoveryDataSource.prototype.handleResponse = function (resp) {
      if (resp.hits.total < 1) return;
      var self = this;
      self.data = _.reduce(resp.hits.hits[0]._source, function (accum, details, index) {
         if (index === 'cluster_name' || index === '@timestamp') return accum;
         if (details.shards && details.shards.length) {
          return accum.concat(details.shards.map(function (row) {
            row.index.name = index;
            return row;
          }));
         } else {
          return accum;
         }
      }, []);
      self.data.sort(function (a, b) {
        return b.start_time_in_millis - a.start_time_in_millis;
      });
    };

    return ShardRecoveryDataSource;

  };
});
