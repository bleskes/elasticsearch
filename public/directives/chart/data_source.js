define(function (require) {
  var _ = require('lodash');
  var moment = require('moment');
  var MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');

  return function chartDataSourceProvider(timefilter, Private) {
    var calcAuto = Private(require('ui/time_buckets/calc_auto_interval'));

    function ChartDataSource(options) {
      MarvelDataSource.call(this, options.indexPattern, options.cluster);
      this.metric = options.metric;
      this.filters = options.filters || [];
    }

    ChartDataSource.prototype = new MarvelDataSource();

    ChartDataSource.prototype.initSearchSource = function () {
      this.searchSource.set('size', 0);
      this.searchSource.set('aggs', _.bindKey(this, 'toAggsObject'));
    };

    ChartDataSource.prototype.getFilters = function () {
      var filters = [ { term: { 'cluster_uuid': this.cluster } } ];
      filters = filters.concat(this.metric.filters || []);
      return filters.concat(this.filters);
    };

    ChartDataSource.prototype.toAggsObject = function () {
      var bounds = timefilter.getBounds();
      var duration = moment.duration(bounds.max - bounds.min, 'ms');
      this.bucketSize = calcAuto.near(100, duration).asSeconds();
      var aggs = {
        check: {
          date_histogram: {
            field: this.index.timeFieldName,
            min_doc_count: 0,
            interval: this.bucketSize + 's',
            extended_bounds: {
              min: bounds.min,
              max: bounds.max
            }
          },
          aggs: { metric: { } }
        }
      };
      aggs.check.aggs.metric[this.metric.metricAgg] = {
        field: this.metric.field
      };
      if (this.metric.derivative) {
        aggs.check.aggs.metric_deriv = {
          derivative: { buckets_path: 'metric', gap_policy: 'skip' }
        };
      }
      if (this.metric.aggs) {
        _.assign(aggs.check.aggs, this.metric.aggs);
      }
      return aggs;
    };

    ChartDataSource.prototype.handleResponse = function (resp) {
      if (!resp.aggregations) return;
      var self = this;
      var defaultCalculation = function (bucket) {
        var key = (self.metric.derivative) ? 'metric_deriv' : 'metric';
        return bucket[key] && bucket[key].value || 0;
      };

      var calculation = this.metric && this.metric.calculation || defaultCalculation;
      var buckets = resp.aggregations.check.buckets;
      this.data = _.map(buckets, function (bucket) {
        return {
          x: bucket.key,
          y: calculation(bucket) // Why are one of these null?
        };
      });
    };
    return ChartDataSource;

  };

});
