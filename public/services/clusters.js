define(function (require) {
  var angular = require('angular');
  require('angular-resource');

  var module = require('ui/modules').get('marvel/clusters', [ 'ngResource' ]);
  module.service('marvelClusters', function ($resource, Promise) {

    var Clusters = $resource('/marvel/api/v1/clusters/:id', { id: '@_id' });
    var cache;

    function fetch(force) {
      if (!force && cache) return Promise.resolve(cache);
      return Clusters.query().$promise.then(function (clusters) {
        cache = clusters;
        return clusters;
      });
    }

    return { fetch: fetch };

  });
});
