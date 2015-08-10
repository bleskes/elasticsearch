define(function (require) {
  var Metric = require('plugins/marvel/lib/metric');
  var metrics = require('plugins/marvel/lib/metrics');
  require('plugins/marvel/services/settings');
  var module = require('ui/modules').get('marvel/metrics', [ 'marvel/settings' ]);

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
