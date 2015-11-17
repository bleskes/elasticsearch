define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  require('angular-resource');

  var module = require('ui/modules').get('marvel/clusters', [ 'ngResource' ]);
  module.service('marvelClusters', function ($resource, Promise) {

    var Clusters = $resource('/api/marvel/v1/clusters/:id', { id: '@cluster_uuid' });
    var cache;

    function fetch() {
      return Clusters.query().$promise.then(function (clusters) {
        return clusters;
      });
    }

    return { fetch: fetch };

  });
});
