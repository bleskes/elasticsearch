

// Mind the gap... UMD Below
(function(define) {
  define(function (require, exports, module) {
    var Model = require('./model');
    var _ = require('lodash');
    var moment = require('moment');

    var lookup = {
      '<': { method: 'lt', message: _.template('is below <%= threshold %> at <%= value %>') },
      '<=': { method: 'lte', message: _.template('is below <%= threshold %> at <%= value %>') },
      '>': { method: 'gt', message: _.template('is above <%= threshold %> at <%= value %>')},
      '>=': { method: 'gte', message: _.template('is above <%= threshold %> at <%= value %>')}
    };

    function parseThreshold(threshold) {
      var parts = threshold.match(/([<>=]{1,2})([\d\.]+)/);
      return { exp: parts[1], limit: Number(parts[2]) };
    }

    function evalThreshold(value, threshold) {
      var parts = parseThreshold(threshold);
      if (lookup[parts.exp]) {
        return _[lookup[parts.exp].method](value, parts.limit);
      }
    }

    function createMessage(value, threshold) {
      var parts = parseThreshold(threshold);
      return lookup[parts.exp].message({ threshold: parts.limit, value: value });
    }

    function checkBuckets(metric, name) {
      return function (value) {
        return evalThreshold(value, metric.settings.get(name));
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
        statusObj.message = createMessage(last, this.settings.get('critical'));
      } else if (warning) {
        statusObj.status = 'yellow';
        statusObj.message = createMessage(last, this.settings.get('warning'));
      }
      return statusObj;
    };

    return Metric;

  });
}( // Help Node out by setting up define.
   typeof module === 'object' && typeof define !== 'function' ? function (factory) { module.exports = factory(require, exports, module); } : define
));

