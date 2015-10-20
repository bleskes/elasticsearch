const MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');
const _ = require('lodash');
module.exports = function indexSummaryDataSourceProvider() {

  return class IndexSummaryDataSource extends MarvelDataSource {

    constructor(options) {
      super(options.indexPattern, options.cluster);
      this.indexName = options.indexName;
      this.data = {};
    }

    initSearchSource() {
      this.searchSource.set('size', 1);
      this.searchSource.set('sort', { timestamp: { order: 'desc' } });
      this.searchSource.set('query', '_type:index_stats');
      this.searchSource.set('filter', () => this.getFilters());
    }

    getFilters() {
      const id = (_.isObject(this.cluster)) ? this.cluster.cluster_uuid : this.cluster;
      return [
        { term: { 'index_stats.index': this.indexName } },
        { term: { cluster_uuid: id } }
      ];
    }

    handleResponse(resp) {
      if (resp && resp.hits && resp.hits.total)  {
        const source = resp.hits.hits[0]._source;
        this.data.documents = _.get(source, 'index_stats.primaries.docs.count');
        this.data.data_size = _.get(source, 'index_stats.total.store.size_in_bytes');
      }
    }

  };

};

