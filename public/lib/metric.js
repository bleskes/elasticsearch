var numeral = require('numeral');

// Mind the gap... UMD Below
(function (define) {
  define(function (require, exports, module) {
    var Model = require('./model');
    var _ = require('lodash');
    var moment = require('moment');

    var lookup = {
      '<': { method: 'lt', message: _.template('is below <%= threshold %><%= units %> at <%= value %><%= units %>') },
      '<=': { method: 'lte', message: _.template('is below <%= threshold %><%= units %> at <%= value %><%= units %>') },
      '>': { method: 'gt', message: _.template('is above <%= threshold %><%= units %> at <%= value %><%= units %>')},
      '>=': { method: 'gte', message: _.template('is above <%= threshold %><%= units %> at <%= value %><%= units %>')}
    };

    function parseThreshold(threshold) {
      var parts = threshold.match(/([<>=]{1,2})([\d\.]+)/);
      return { exp: parts[1], limit: Number(parts[2]) };
    }

    var durations = {
      y: 'years',
      M: 'months',
      w: 'weeks',
      d: 'days',
      h: 'hours',
      m: 'minutes',
      s: 'seconds'
    };

    function calculateValue(metric, value) {
      if (metric.units === '/s') {
        var bucketParts = metric.settings.get('interval').match(/([\d]+)([yMwdhms])/);
        if (bucketParts) {
          var duration = moment.duration(Number(bucketParts[1]), durations[bucketParts[2]]);
          if (duration.asSeconds() > 0) value = value / duration.asSeconds();
        }
      }
      return value;
    }

    function evalThreshold(metric, value, threshold) {
      value = calculateValue(metric, value);
      var parts = parseThreshold(threshold);
      if (lookup[parts.exp]) {
        return _[lookup[parts.exp].method](value, parts.limit);
      }
    }

    function createMessage(metric, value, threshold, format, units) {
      value = calculateValue(metric, value);
      var parts = parseThreshold(threshold);
      value = (value && format) ? numeral(value).format(format) : value;
      return lookup[parts.exp].message({ units: units, threshold: numeral(parts.limit).format(format), value: value });
    }

    function checkBuckets(metric, name) {
      return function (value) {
        return evalThreshold(metric, value, metric.settings.get(name));
      };
    }

    function Metric(id, options, settings) {
      this.id = id;
      this.field = options.field || id;
      this.settings = new Model(settings.get(this.id));
      _.defaults(this, options);
    }

    /**
     * Returns an object representation of the metrics state
     *
     * This will return a object that represents the current state of
     * the metric based on the value passed to it. It will look like the
     * follow:
     *
     * { status: 'green', field: 'os.cup.user', message: 'is OK' }
     * { status: 'yellow', field: 'os.cup.user', message: 'is above 5 at 5.6' }
     * { status: 'red', field: 'os.cup.user', message: 'is above 5 at 5.6' }
     *
     * @param number value The value of the metric  to evaluate
     * @returns object
     */
    Metric.prototype.threshold = function (value) {
      var self = this;
      var buckets = _.flatten([value]);
      var last = _.last(buckets);
      var statusObj = {
        id: this.id,
        status: 'green',
        field: this.field,
        message: 'Ok',
        value: last,
        timestamp: moment.utc()
      };
      var critical = _.every(buckets, checkBuckets(this, 'critical'));
      var warning = _.every(buckets, checkBuckets(this, 'warning'));
      if (critical) {
        statusObj.status = 'red';
        statusObj.message = createMessage(this, last, this.settings.get('critical'), this.format, this.units);
      } else if (warning) {
        statusObj.status = 'yellow';
        statusObj.message = createMessage(this, last, this.settings.get('warning'), this.format, this.units);
      }
      return statusObj;
    };

    return Metric;

  });
}(// Help Node out by setting up define.
   typeof module === 'object' &&
   typeof define !== 'function' ? function (factory) { module.exports = factory(require, exports, module); } : define
));

