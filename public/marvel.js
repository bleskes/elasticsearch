require('plugins/marvel/less/main.less');
require('plugins/marvel/filters/index.js');
require('plugins/marvel/directives/index.js');
require('plugins/marvel/views/home/home_controller.js');
require('plugins/marvel/views/indices/index.js');
require('plugins/marvel/views/nodes/nodes_controller.js');
require('plugins/marvel/views/node/node_controller.js');
require('plugins/marvel/views/overview/index.js');
require('plugins/marvel/views/settings/index.js');
require('plugins/marvel/views/issues/issues_controller.js');
require('plugins/marvel/views/setup/setup_controller.js');
require('plugins/marvel/views/shard_allocation/shard_allocation_controller.js');
require('plugins/marvel/views/index/index_controller.js');
require('ui/modules').get('kibana').config(function (PrivateProvider) {
  PrivateProvider.swap(require('ui/config/defaults'), function ($injector) {
    var defaults = $injector.invoke(require('ui/config/defaults'));
    defaults['timepicker:timeDefaults'] = {
      type: 'json',
      value: [
        '{',
        '  "from": "now-1h",',
        '  "to": "now",',
        '  "mode": "quick"',
        '}'
      ].join('\n')
    };
    defaults['timepicker:refreshIntervalDefaults'] = {
      type: 'json',
      value: [
        '{',
        '  "display": "10 seconds",',
        '  "pause": false,',
        '  "value": 10000',
        '}'
      ].join('\n')
    };
    return defaults;
  });
});


require('ui/chrome')
  .setNavBackground('#222222')
  .setTabDefaults({
    resetWhenActive: true,
    trackLastPath: true,
    activeIndicatorColor: '#666'
  })
  .setRootController('marvel', function ($scope, courier) {
    $scope.$on('application.load', function () {
      courier.start();
    });
  });

