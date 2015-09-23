const MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');
const _ = require('lodash');
module.exports = function nodeSummaryDataSourceProvider() {

  return class NodeSummaryDataSource extends MarvelDataSource {

    constructor(options) {
      super(options.indexPattern, options.cluster);
      this.node = options.node;
      this.data = {};
    }

    initSearchSource() {
      this.searchSource.set('size', 1);
      this.searchSource.set('sort', { timestamp: { order: 'desc' } });
      this.searchSource.set('query', '_type:node_stats');
      this.searchSource.set('filter', () => this.getFilters());
    }

    getFilters() {
      const id = (_.isObject(this.cluster)) ? this.cluster.cluster_uuid : this.cluster;
      return [
        { term: { 'node_stats.node_id': this.node.id } },
        { term: { cluster_uuid: id } }
      ];
    }

    handleResponse(resp) {
      if (resp && resp.hits && resp.hits.total)  {
        const source = resp.hits.hits[0]._source;
        this.data.documents = _.get(source, 'node_stats.indices.docs.count');
        this.data.data_size = _.get(source, 'node_stats.indices.store.size_in_bytes');
      }
    }

  };

};
