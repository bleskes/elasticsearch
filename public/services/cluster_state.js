define(function (require) {
  var angular = require('angular');
  var module = require('ui/modules').get('marvel/services', [])
  var calculateShardStats = require('plugins/marvel/lib/calculate_shard_stats');

  module.factory('marvelClusterState', function (courier, $rootScope, globalState) {
    var searchSource = new courier.SearchSource();
    searchSource.set('sort', { 'timestamp': { order: 'desc' } });
    searchSource.set('size', 1);
    searchSource.set('filter', function () {
      return [
        { term: { 'cluster_name': globalState.cluster } },
        { term: { _type: 'marvel_cluster_state' } }
      ];
    });
    searchSource.onResults(function (resp) {
      if (resp.hits.total) {
        var newState = resp.hits.hits[0]._source;
        if (clusterState.timestamp !== newState.timestamp) {
          clusterState.timestamp = newState.timestamp;
          _.assign(clusterState, newState.cluster_state);
          clusterState.shardStats = calculateShardStats(newState);
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
