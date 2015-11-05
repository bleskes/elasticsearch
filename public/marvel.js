require('plugins/marvel/less/main.less');
require('plugins/marvel/filters/index.js');
require('plugins/marvel/directives/index.js');
require('plugins/marvel/services/features.js');
require('plugins/marvel/services/executor.js');
require('plugins/marvel/views/no_data/no_data_controller.js');
require('plugins/marvel/views/home/home_controller.js');
require('plugins/marvel/views/indices/indices_controller.js');
require('plugins/marvel/views/index/index_controller.js');
require('plugins/marvel/views/nodes/nodes_controller.js');
require('plugins/marvel/views/node/node_controller.js');
require('plugins/marvel/views/overview/overview_controller.js');
require('plugins/marvel/views/settings/index.js');
require('plugins/marvel/views/issues/issues_controller.js');
require('plugins/marvel/views/setup/setup_controller.js');
require('plugins/marvel/views/expired_license/index.js');
require('ui/modules').get('kibana').config(function (PrivateProvider) {
  PrivateProvider.swap(require('ui/config/defaults'), function ($injector) {
    var defaults = $injector.invoke(require('ui/config/defaults'));
    defaults['timepicker:timeDefaults'] = {
      type: 'json',
      value: JSON.stringify({
        from: 'now-1h',
        to: 'now',
        mode: 'quick'
      })
    };
    defaults['timepicker:refreshIntervalDefaults'] = {
      type: 'json',
      value: JSON.stringify({
        display: '10 seconds',
        pause: false,
        value: 10000
      })
    };
    return defaults;
  });
});


require('ui/chrome')
  .setNavBackground('#222222')
  .setTabDefaults({
    resetWhenActive: true,
    trackLastPath: true,
    activeIndicatorColor: '#EFF0F1'
  })
  .setRootController('marvel', function ($scope, courier) {
    $scope.$on('application.load', function () {
      courier.start();
    });
  });

