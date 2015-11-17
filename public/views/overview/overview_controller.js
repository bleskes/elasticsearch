/**
 * Controller for Overview Page
 */
const mod = require('ui/modules').get('marvel', [ 'marvel/directives' ]);
const _ = require('lodash');

function getPageData(timefilter, globalState, $http) {
  const timeBounds = timefilter.getBounds();
  const url = `/api/marvel/v1/clusters/${globalState.cluster}`;
  return $http.post(url, {
    timeRange: {
      min: timeBounds.min.toISOString(),
      max: timeBounds.max.toISOString()
    },
    metrics: [
      'cluster_search_request_rate',
      'cluster_query_latency',
      'cluster_index_request_rate',
      'cluster_index_latency'
    ]
  }).then((response) => {
    return response.data;
  });
}

require('ui/routes')
.when('/overview', {
  template: require('plugins/marvel/views/overview/overview_template.html'),
  resolve: {
    marvel: function (Private) {
      const routeInit = Private(require('plugins/marvel/lib/route_init'));
      return routeInit();
    },
    pageData: getPageData
  }
});

mod.controller('overview', ($route, globalState, timefilter, $http, Private, $executor, marvelClusters, $scope) => {

  timefilter.enabled = true;

  function setClusters(clusters) {
    $scope.clusters = clusters;
    $scope.cluster = _.find($scope.clusters, { cluster_uuid: globalState.cluster });
  }
  setClusters($route.current.locals.marvel.clusters);

  $scope.pageData = $route.current.locals.pageData;

  var docTitle = Private(require('ui/doc_title'));
  docTitle.change(`Marvel - ${$scope.cluster.cluster_name}`, true);

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

