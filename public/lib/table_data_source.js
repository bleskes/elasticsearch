define(function (require) {
  var MarvelDataSource = require('plugins/marvel/lib/marvel_data_source');
  var _ = require('lodash');
  var moment = require('moment');
<<<<<<< HEAD:app/lib/table_data_source.js
  var metrics = require('marvel/lib/metrics');
  var numeral = require('numeral');
=======
  var metrics = require('plugins/marvel/lib/metrics');

  function calcSlope(data) {
    var length = data.length;
    var xSum = data.reduce(function(prev, curr) { return prev + curr.x; }, 0);
    var ySum = data.reduce(function(prev, curr) { return prev + curr.y; }, 0);
    var xySum = data.reduce(function(prev, curr) { return prev + (curr.y * curr.x); }, 0);
    var xSqSum = data.reduce(function(prev, curr) { return prev + (curr.x * curr.x); }, 0);
    var numerator = (length * xySum) - (xSum * ySum);
    var denominator = (length * xSqSum) - (xSum * ySum);
    return numerator / denominator;
  }
>>>>>>> upstream/master:public/lib/table_data_source.js

  return function tableDataSourceProvider(timefilter, Private) {
    var calcAuto = Private(require('ui/time_buckets/calc_auto_interval'));

    function TableDataSource(options) {
      MarvelDataSource.call(this, options.index, options.cluster);
      this.clusters = options.clusters;
      this.type = options.type || 'index';
      this.metrics = options.metrics;
      this.duration = options.duration;
    }

    TableDataSource.prototype = new MarvelDataSource();

    TableDataSource.prototype.initSearchSource = function () {
      if (this.duration) {
        this.searchSource.inherits(false);
      }
      this.searchSource.set('size', 0);
      this.searchSource.set('aggs', _.bindKey(this, 'toAggsObject'));
    };

    TableDataSource.prototype.getFilters = function () {
      var filters = MarvelDataSource.prototype.getFilters.call(this);
      if (this.duration) {
        filters.push({
          range: {
            'timestamp': {
              gte: moment().subtract(this.duration.asMilliseconds(), 'ms').valueOf(),
              lte: moment().valueOf(),
              format: "epoch_millis"
            }
          }
        })
      }
      return filters;
    };

    TableDataSource.prototype.getCluster = function () {
      if (_.isObject(this.cluster)) return this.cluster;
      return _.find(this.clusters, { _id: this.cluster });
    };

    TableDataSource.prototype.createTermAgg = function () {
      var cluster = this.getCluster();
      if (this.type === 'index') {
        return {
          field: 'index_stats.index',
          size: cluster.counts.indices
        }
      }
      if (this.type === 'node') {
        return {
          field: 'node_stats.name',
          size: cluster.counts.nodes
        }
      }
    }

    TableDataSource.prototype.toAggsObject = function () {
      var self = this;
      var bounds = timefilter.getBounds();
      if (!this.duration) {
        this.duration = moment.duration(bounds.max - bounds.min, 'ms');
      }
      this.bucketSize = calcAuto.near(50, this.duration).asSeconds();

      var aggs = {
        items: {
          terms: this.createTermAgg(),
          aggs: {  }
        }
      };

      _.each(this.metrics, function (id) {
        var metric = metrics[id];
        if (!metric) return;
        if (!metric.aggs) {
          metricAgg = {
            metric: {},
            metric_deriv: {
              derivative: { buckets_path: 'metric' }
            }
          };
          metricAgg.metric[metric.metricAgg] = {
            field: metric.field
          };
        }

        aggs.items.aggs[id] = {
          date_histogram: {
            field: self.index.timeFieldName,
            interval: self.bucketSize + 's'
          },
          aggs: metric.aggs || metricAgg
        };

      });

      return aggs;
    };

    function mapChartData(metric) {
      return function (row) {
        var data = {x: row.key};
        data.y = (metric.derivative ? row.metric_deriv && row.metric_deriv.value || 0 : row.metric.value);
        return data;
      };
    }

    TableDataSource.prototype.handleResponse = function (resp) {
      this.data = [];
      var items;
      var self = this;
      if (resp.hits.total) {
        items = resp.aggregations.items.buckets;
        this.data = _.map(items, function (item) {
          var row = { name: item.key, metrics: {} };
          _.each(self.metrics, function (id) {
            var metric = metrics[id];
            var data = _.map(item[id].buckets, mapChartData(metric));
<<<<<<< HEAD:app/lib/table_data_source.js
            var min = _.min(data, 1);
            var max = _.max(data, 1);
            var last = numeral(_.last(data).y).format(metric.format) + metric.units;
            row.metrics[id] = {
              metric: metric,
              data: data,
              min: min && min[1] || 0,
              max: max && max[1] || 0,
              last: last || 0
=======
            var min = _.min(_.pluck(data, 'y'));
            var max = _.max(_.pluck(data, 'y'));
            var last = _.last(_.pluck(data, 'y'));
            var slope = calcSlope(data);
            row.metrics[id] = {
              metric: metric,
              data: data,
              min: min  || 0,
              max: max || 0,
              last: last || 0,
              slope: slope
>>>>>>> upstream/master:public/lib/table_data_source.js
            };
          }); // end each
          return row;
        }); // end map
      } // end if
    };

    return TableDataSource;
  };
});
