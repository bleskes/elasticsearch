define(function (require) {
  var angular = require('angular');
  var module = require('modules').get('marvel/services', [])
  var calculateShardStats = require('plugin/marvel/lib/calculate_shard_stats');

  module.factory('marvelClusterState', function (courier, $rootScope, globalState) {
    var searchSource = new courier.SearchSource();
    searchSource.set('sort', { '@timestamp': { order: 'desc' } });
    searchSource.set('size', 1);
    searchSource.set('filter', function () {
      return [
        { term: { 'cluster_name.raw': globalState.cluster } },
        { term: { _type: 'cluster_state' } }
      ];
    });
    searchSource.onResults(function (resp) {
      if (resp.hits.total) {
        var newState = resp.hits.hits[0]._source;
        if (clusterState.version !== newState.version) {
          _.assign(clusterState, resp.hits.hits[0]._source);
          clusterState.shardStats = calculateShardStats(clusterState);
        }
      }
    });

    function setIndexPattern(pattern) {
      searchSource.set('index', pattern);
    }

    function fetch() {
      return courier.fetch();
    }

    var clusterState = {
      setIndex: setIndexPattern,
      fetch: fetch
    };

    return clusterState;

  });
});
