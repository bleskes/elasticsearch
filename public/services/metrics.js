define(function (require) {
  var Metric = require('marvel/lib/metric');
  var metrics = require('marvel/lib/metrics');
  require('marvel/services/settings');
  var module = require('modules').get('marvel/metrics', [ 'marvel/settings' ]);

  module.service('marvelMetrics', function (marvelSettings, $resource, Promise, Private) {
    return function (field) {
      return marvelSettings.fetch().then(function (settings) {
        var metric = new Metric(field, metrics[field], settings['metric-thresholds']);
        return metric;
      });
    };
  });

});
