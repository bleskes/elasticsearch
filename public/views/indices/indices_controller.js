/**
 * Controller for Index Listing
 */
const mod = require('ui/modules').get('marvel', [ 'marvel/directives' ]);
const _ = require('lodash');

require('ui/routes')
.when('/indices', {
  template: require('plugins/marvel/views/indices/index.html'),
  resolve: {
    marvel: function (Private) {
      var routeInit = Private(require('plugins/marvel/lib/route_init'));
      return routeInit();
    }
  }
});

mod.controller('indices', (globalState, timefilter, $http, $executor, $scope) => {

  $executor.register({
    execute: function () {
      const timeBounds = timefilter.getBounds();
      const url = `/marvel/api/v1/clusters/${globalState.cluster}/indices`;
      return $http.post(url, {
        timeRange: {
          min: timeBounds.min.toISOString(),
          max: timeBounds.max.toISOString()
        }
      });
    },
    handleResponse: function (response) {
      const data = response.data;
      $scope.clusterStatus = data.clusterStatus;
      $scope.metrics = data.metrics;
      $scope.rows = data.rows;
    }
  });

  // Start the executor
  $executor.start();

  // Destory the executor
  $scope.$on('$destroy', $executor.destroy);

});
