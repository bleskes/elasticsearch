

// Mind the gap... UMD Below
(function(define) {
  define(function (require, exports, module) {
    var _ = require('lodash');
    var lookup = {
      '<': { method: 'lt', message: _.template('is below <%= threshold %> at <%= value %>') },
      '<=': { method: 'lte', message: _.template('is below <%= threshold %> at <%= value %>') },
      '>': { method: 'gt', message: _.template('is above <%= threshold %> at <%= value %>')},
      '>=': { method: 'gte', message: _.template('is above <%= threshold %> at <%= value %>')}
    };

    function evalThreshold(value, threshold) {
      var parts = threshold.match(/([<>=]{1,2})([\d\.]+)/);
      var exp = parts[1];
      var limit = Number(parts[2]);
      if (lookup[exp]) {
        if (_[lookup[exp].method](value, limit)) {
          return lookup[exp].message({ threshold: limit, value: value });
        }
      }
    }


    function Metric(field, options, settings) {
      this._field = field;
      this._settings = settings;
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
      var thresholds = this._settings.get(this._field);
      var statusObj = { status: 'green', field: this._field, message: 'Ok', value: value };
      var critical = evalThreshold(value, thresholds.critical);
      var warning = evalThreshold(value, thresholds.warning);
      if (critical) {
        statusObj.status = 'red';
        statusObj.message = critical;
      } else if (warning) {
        statusObj.status = 'yellow';
        statusObj.message = warning;
      }
      return statusObj;
    };

    return Metric;

  });
}( // Help Node out by setting up define.
   typeof module === 'object' && typeof define !== 'function' ? function (factory) { module.exports = factory(require, exports, module); } : define
));

