/**
 * Controller for Node Listing
 */
const _ = require('lodash');
const mod = require('ui/modules').get('marvel', [ 'plugins/marvel/directives' ]);

function getPageData(timefilter, globalState, $http) {
  const timeBounds = timefilter.getBounds();
  const url = `/api/marvel/v1/clusters/${globalState.cluster}/nodes`;
  return $http.post(url, {
    timeRange: {
      min: timeBounds.min.toISOString(),
      max: timeBounds.max.toISOString()
    },
    listingMetrics: [
      'node_cpu_utilization',
      'node_jvm_mem_percent',
      'node_load_average',
      'node_free_space'
    ]
  }).then((response) => {
    return response.data;
  });
}

require('ui/routes')
.when('/nodes', {
  template: require('plugins/marvel/views/nodes/nodes_template.html'),
  resolve: {
    marvel: function (Private) {
      var routeInit = Private(require('plugins/marvel/lib/route_init'));
      return routeInit();
    },
    pageData: getPageData
  }
});

mod.controller('nodes', ($route, timefilter, globalState, Private, $executor, $http, marvelClusters, $scope) => {

  timefilter.enabled = true;

  function setClusters(clusters) {
    $scope.clusters = clusters;
    $scope.cluster = _.find($scope.clusters, { cluster_uuid: globalState.cluster });
  }
  setClusters($route.current.locals.marvel.clusters);

  $scope.pageData = $route.current.locals.pageData;

  const docTitle = Private(require('ui/doc_title'));
  docTitle.change('Marvel', true);

  $executor.register({
    execute: () => getPageData(timefilter, globalState, $http),
    handleResponse: (response) => $scope.pageData = response
  });

  $executor.register({
    execute: () => marvelClusters.fetch(),
    handleResponse: setClusters
  });

  // Start the executor
  $executor.start();

  // Destory the executor
  $scope.$on('$destroy', $executor.destroy);

});
