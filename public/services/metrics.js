define(function (require) {
  var Metric = require('plugin/marvel/lib/metric');
  var metrics = require('plugin/marvel/lib/metrics');
  require('plugin/marvel/services/settings');
  var module = require('modules').get('marvel/metrics', [ 'marvel/settings' ]);

  module.service('marvelMetrics', function (marvelSettings, $resource, Promise, Private) {
    return function (cluster, field) {
      return marvelSettings.fetch().then(function (settings) {
        if (metrics[field]) {
          var metric = new Metric(field, metrics[field], settings[cluster + ':metric-thresholds']);
          return metric;
        }
      });
    };
  });

});
