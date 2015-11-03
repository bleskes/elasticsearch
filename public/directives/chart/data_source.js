define(function (require) {
  var _ = require('lodash');
  var moment = require('moment');
  var MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');

  /* calling .subtract or .add on a moment object mutates the object
   * so this function shortcuts creating a fresh object */
  function getTime(bucket) {
    return moment(bucket.key);
  }

  /* find the milliseconds of difference between 2 moment objects */
  function getDelta(t1, t2) {
    return moment.duration(t1 - t2).asMilliseconds();
  }

  return function chartDataSourceProvider(timefilter, Private, minIntervalSeconds) {
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
      this.bucketSize = Math.max(minIntervalSeconds, calcAuto.near(100, duration).asSeconds());
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
          aggs: { metric: { } },
          meta: {
            timefilterMin: bounds.min.valueOf(),
            timefilterMax: bounds.max.valueOf(),
            bucketSize: this.bucketSize
          }
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
      var aggCheck = resp.aggregations.check;
      var buckets = aggCheck.buckets;
      var boundsMin = moment(aggCheck.meta.timefilterMin);
      var boundsMax = moment(aggCheck.meta.timefilterMax);
      var bucketSize = aggCheck.meta.bucketSize;
      this.data = _.chain(buckets)
        .filter(function (bucket) {
          if (getDelta(getTime(bucket).subtract(bucketSize, 'seconds'), boundsMin) < 0) {
            return false;
          }
          if (getDelta(boundsMax, getTime(bucket).add(bucketSize, 'seconds')) < 0) {
            return false;
          }
          return true;
        })
        .map(function (bucket) {
          return {
            x: bucket.key,
            y: calculation(bucket) // Why are one of these null?
          };
        })
        .value();
    };
    return ChartDataSource;

  };

});
