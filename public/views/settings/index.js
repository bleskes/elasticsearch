define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var metrics = require('marvel/lib/metrics');

  require('marvel/services/settings');
  require('components/notify/notify');

  var module = require('modules').get('marvel', [
    'kibana/notify',
    'marvel/directives',
    'marvel/settings'
  ]);

  require('routes')
  .when('/marvel/settings', {
    template: require('text!marvel/views/settings/index.html'),
    resolve: {
      settings: function ($route, marvelSettings) {
        return marvelSettings.fetch(true); // force fetch
      }
    }
  });


  module.controller('settings', function ($scope, $route, Notifier) {
    var notify = new Notifier({ location: 'Marvel Settings' });
    var settings = $route.current.locals.settings['metric-thresholds'];
    $scope.metrics = metrics;

    // Create a model for the view to easily work with
    $scope.model = {};
    _.each(metrics, function (val, key) {
      $scope.model[key] = settings.get(key);
    });

    // Set the settings from the model and save.
    $scope.save = function () {
      $scope.saving = true;
      settings.set($scope.model);
      settings.save().then(function () {
        notify.info('Settings saved successfully.');
        $scope.saving = false;
      });
    };
  });

});
