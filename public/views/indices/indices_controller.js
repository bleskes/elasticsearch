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
    }
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
  $scope.pageData.clusterStatus.status = 'yellow';

  $executor.register({
    execute: function () {
      return getPageData(timefilter, globalState, $http);
    },
    handleResponse: function (response) {
      $scope.pageData = response;
      $scope.pageData.clusterStatus.status = 'yellow';
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
