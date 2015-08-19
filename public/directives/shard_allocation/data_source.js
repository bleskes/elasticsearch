define(function (require) {
  var _ = require('lodash');
  var formatNumber = require('plugins/marvel/lib/format_number');
  var MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');

  return function timelineDataSourceProvider() {

    function TimelineDataSource(index, cluster) {
      MarvelDataSource.call(this, index, cluster);
      this.data = [];
    }

    TimelineDataSource.prototype = new MarvelDataSource();

    TimelineDataSource.prototype.initSearchSource = function () {
      this.searchSource.set('size', 1);
      this.searchSource.set('sort', {'timestamp': { order: 'desc' }});
      this.searchSource.set('query', '_type:recovery');
    };

    TimelineDataSource.prototype.handleResponse = function (resp) {
      if (resp.hits.total < 1) return;
      var self = this;
      self.data = resp.hits.hits[0]._source.shards || [];
      self.data.sort(function (a, b) {
        return b.start_time_in_millis - a.start_time_in_millis;
      });
    };

    return TimelineDataSource;

  };
});

