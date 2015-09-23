define(function (require) {
  var _ = require('lodash');
  var MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');

  return function indicesDataSourceProvider() {
    function IndicesDataSource(index, cluster, clusters) {
      MarvelDataSource.call(this, index, cluster);
      this.clusters = clusters;
    }

    IndicesDataSource.prototype = new MarvelDataSource();

    IndicesDataSource.prototype.initSearchSource = function () {
      this.searchSource.set('size', 1000);
      this.searchSource.set('sort', {'timestamp': { order: 'desc' }});
      this.searchSource.set('query', '_type:index_stats');
    };

    IndicesDataSource.prototype.handleResponse = function (resp) {
      if (resp.hits.total === 0) return;
      this.data = resp.hits.hits.map(function (hit) {
        var source = hit._source;

        var primaries = source.primaries;
        function safeRateDivide(total, timeInMillis) {
          var timeInSeconds = timeInMillis / 1000;
          return timeInSeconds ? total / timeInSeconds : 0;
        }
        return {
          name: hit._source.index,
          docCount: primaries.docs.count,
          indexRate: safeRateDivide(primaries.indexing.index_total, primaries.indexing.index_time_in_millis),
          searchRate: safeRateDivide(primaries.search.fetch_total, primaries.search.query_time_in_millis),
          mergeRate: safeRateDivide(primaries.merges.total_docs, primaries.merges.total_time_in_millis),
          fieldData: primaries.fielddata.evictions
        };
      });
    };
    return IndicesDataSource;

  };
});
