/**
 * Controller for single index detail
 */
const _ = require('lodash');
const mod = require('ui/modules').get('marvel', []);

require('ui/routes')
.when('/index/:index', {
  template: require('plugins/marvel/views/index/index_template.html'),
  resolve: {
    marvel: function (Private) {
      const routeInit = Private(require('plugins/marvel/lib/route_init'));
      return routeInit();
    },
    pageData: getPageData
  }
});

function getPageData(timefilter, globalState, $route, $http) {
  const timeBounds = timefilter.getBounds();
  const url = `/api/marvel/v1/clusters/${globalState.cluster}/indices/${$route.current.params.index}`;
  return $http.post(url, {
    timeRange: {
      min: timeBounds.min.toISOString(),
      max: timeBounds.max.toISOString()
    },
    metrics: [
      'index_search_request_rate',
      'index_request_rate',
      'index_size',
      'index_lucene_memory',
      'index_document_count',
      'index_fielddata'
    ]
  }).then((response) => {
    return response.data;
  });
}

mod.controller('indexView', (timefilter, $route, Private, globalState, $executor, $http, marvelClusters, $scope) => {
  timefilter.enabled = true;

  function setClusters(clusters) {
    $scope.clusters = clusters;
    $scope.cluster = _.find($scope.clusters, { cluster_uuid: globalState.cluster });
  }
  setClusters($route.current.locals.marvel.clusters);

  $scope.pageData = $route.current.locals.pageData;
  $scope.indexName = $route.current.params.index;

  var docTitle = Private(require('ui/doc_title'));
  docTitle.change(`Marvel - ${$scope.indexName}`, true);

  $executor.register({
    execute: () => getPageData(timefilter, globalState, $route, $http),
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
