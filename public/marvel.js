// require.config({
//   paths: {
//     'lodash-deep': '/marvel/bower_components/lodash-deep/lodash-deep',
//     'angular-resource': '/marvel/bower_components/angular-resource/angular-resource',
//     'nvd3': 'marvel/bower_components/nvd3/build/nv.d3',
//     'nvd3_directives': 'marvel/bower_components/nvd3_directives/dist/angular-nvd3',
//     'marvel/react': 'marvel/bower_components/react/react'
//   },
//   shim: {
//     'nvd3': ['css!marvel/bower_components/nvd3/build/nv.d3.css', 'd3'],
//     'nvd3_directives': ['angular', 'd3', 'nvd3']
//   }
// });
// require('plugin/marvel/less/main.less');
// require('plugin/marvel/services/cluster_state');
// require('plugin/marvel/filters/index.js');
// require('plugin/marvel/directives/index.js');
// require('plugin/marvel/views/indices/index.js');
// require('plugin/marvel/views/overview/index.js');
// require('plugin/marvel/views/settings/index.js');
// require('plugin/marvel/views/issues/issues_controller.js');
// require('plugin/marvel/views/setup/setup_controller.js');
// require('plugin/marvel/views/shard_allocation/shard_allocation_controller.js');
// require('plugin/marvel/views/index/index_controller.js');
// require('plugin/marvel/views/nodes/nodes_controller.js');

var tabs = [
  { id: 'marvel', title: 'Overview' },
  { id: 'marvel/indices', title: 'Indices' },
  { id: 'marvel/nodes', title: 'Nodes' },
  { id: 'marvel/shard_allocation', title: 'Shard Allocation' }
];


require('ui/chrome')
  .setNavBackground('#222222')
  .setTabDefaults({
    resetWhenActive: true,
    trackLastPath: true,
    activeIndicatorColor: '#666'
  })
  .setTabs(tabs)
  .setRootController('marvel', function ($scope, courier) {
    $scope.$on('application.load', function () {
      courier.start();
    });
  });

