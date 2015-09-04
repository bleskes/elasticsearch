define(function (require) {
  var _ = require('lodash');
  function addOne(obj, key) {
    var value = _.get(obj, key);
    _.set(obj, key, ++value);
  }

  return function calculateShardStats(state) {
    var data = { totals: { primary: 0, replica: 0, unassigned: { replica: 0, primary: 0 } } };
    var processShards = function (shard) {
      var metrics = data[shard.index] || { status: 'green', primary: 0, replica: 0, unassigned: { replica: 0, primary: 0 } };
      var key = '';
      if (shard.state !== 'STARTED') {
        key = 'unassigned.';
        if (metrics.status !== 'red') {
          metrics.status = (shard.primary && shard.state === 'UNASSIGNED') ? 'red' : 'yellow';
        }
      }
      key += shard.primary ? 'primary' : 'replica';
      addOne(metrics, key);
      addOne(data.totals, key);
      data[shard.index] = metrics;
    };
    if (state) {
      var clusterName = _.get(state, 'cluster_uuid');
      var shards = _.get(state, 'cluster_state.shards');
      _.each(shards, processShards);
    }
    return data;
  };

});
