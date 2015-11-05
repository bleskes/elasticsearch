/**
 * Controller for Index Listing
 */
const mod = require('ui/modules').get('marvel', [ 'marvel/directives' ]);
const _ = require('lodash');

function getPageData(timefilter, globalState, $http) {
  const timeBounds = timefilter.getBounds();
  const url = `/marvel/api/v1/clusters/${globalState.cluster}/indices`;
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
    ],
    listingMetrics: [
      'index_document_count',
      'index_size',
      'index_search_request_rate',
      'index_request_rate'
    ]
  }).then((response) => {
    return response.data;
  });
}

require('ui/routes')
.when('/indices', {
  template: require('plugins/marvel/views/indices/index.html'),
  resolve: {
    marvel: function (Private) {
      var routeInit = Private(require('plugins/marvel/lib/route_init'));
      return routeInit();
    },
    pageData: getPageData
  }
});

mod.controller('indices', ($route, globalState, timefilter, $http, $executor, marvelClusters, $scope) => {

  timefilter.enabled = true;

  function setClusters(clusters) {
    $scope.clusters = clusters;
    $scope.cluster = _.find($scope.clusters, { cluster_uuid: globalState.cluster });
  }
  setClusters($route.current.locals.marvel.clusters);

  $scope.pageData = $route.current.locals.pageData;

  $executor.register({
    execute: function () {
      return getPageData(timefilter, globalState, $http);
    },
    handleResponse: function (response) {
      $scope.pageData = response;
    }
  });

  $executor.register({
    execute: function () {
      return marvelClusters.fetch();
    },
    handleResponse: setClusters
  });


  // Start the executor
  $executor.start();

  // Destory the executor
  $scope.$on('$destroy', $executor.destroy);

});
