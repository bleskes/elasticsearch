define(function (require) {
  var _ = require('lodash');
  var metrics = require('plugins/marvel/lib/metrics');
  var angular = require('angular');
  var Model = require('plugins/marvel/lib/model');
  require('angular-resource');

  // Get the module and declare the dependencies
  var module = require('ui/modules').get('marvel/settings', [ 'ngResource' ]);

  // Create the marvelSetting service which will fetch all the settings
  module.service('marvelSettings', function ($resource, Promise, Private) {

    var allSettings;
    var Settings = require('plugins/marvel/lib/settings')($resource);
    var defaults = { 'metric-thresholds': metrics };

    function fetch(cluster, force) {
      // If we have the settings and we don't need to refresh from server then
      // return the current settings. Otherwise make a request for the settings
      // from the server. The only time you should force the refresh is if you
      // are editing the settings and want to ensure you have the latest version.
      if (allSettings && !force) {
        return Promise.resolve(allSettings);
      }

      // Request the settings from the server.
      allSettings = {};
      return Settings.bulkFetch(cluster).then(function (docs) {
        _.each(docs, function (doc) {
          allSettings[doc.get('_id')] = doc;
        });
        return allSettings;
      });
    }
    return { fetch: fetch };
  });
});
