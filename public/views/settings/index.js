define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var metrics = require('plugins/marvel/lib/metrics');

  require('plugins/marvel/services/settings');
  require('ui/notify/notify');

  var module = require('ui/modules').get('marvel', [
    'kibana/notify',
    'marvel/directives',
    'marvel/settings'
  ]);

  // require('ui/routes')
  // .when('/settings', {
  //   template: require('plugins/marvel/views/settings/index.html'),
  //   resolve: {
  //     marvel: function (Private) {
  //       var routeInit = Private(require('plugins/marvel/lib/route_init'));
  //       return routeInit({ force: { settings: true } });
  //     }
  //   }
  // });

  module.controller('settings', function (timefilter, courier, $scope, $route, Notifier, Private, globalState) {
    // var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));

    var notify = new Notifier({ location: 'Marvel Settings' });
    var settings = $route.current.locals.marvel.settings[globalState.cluster + ':metric-thresholds'];
    var indexPattern = $route.current.locals.marvel.indexPattern;
    var clusters = $route.current.locals.marvel.clusters;

    $scope.metrics = metrics;
    $scope.dataSources = {};

    // var dataSource = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    // $scope.dataSources.cluster_status = dataSource;
    // dataSource.register(courier);
    // courier.fetch();

    // $scope.$on('$destroy', function () {
    //   _.each($scope.dataSources, function (dataSource) {
    //     dataSource.destroy();
    //   });
    // });

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
