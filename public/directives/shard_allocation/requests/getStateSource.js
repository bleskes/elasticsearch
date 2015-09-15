/**
 * ELASTICSEARCH CONFIDENTIAL
 * _____________________________
 *
 *  [2014] Elasticsearch Incorporated All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

define(function () {
  var _ = require('lodash');
  var getValueFromArrayOrString = require('../lib/getValueFromArrayOrString');
  return function getStateSourceProvider($http, es) {
    return function (obj, id, type) {
      var stateUuid = getValueFromArrayOrString(obj.fields['cluster_state.state_uuid']);
      var clusterUuid = getValueFromArrayOrString(obj.fields.cluster_uuid);
      return es.get({ index: obj._index, type: 'marvel_cluster_state', id: obj._id })
        .then((stateResp) => {
          var url = `/marvel/api/v1/clusters/${clusterUuid}/state/${stateUuid}/shards`;
          if (id && type) url += `?${type}=${id}`;
          return $http.get(url).then((shardResp) => {
            if (stateResp && stateResp._source) {
              var state = stateResp._source;
              if (shardResp && shardResp.data) {
                _.set(state, 'cluster_state.shards', shardResp.data || []);
              }
              return state;
            }
          });
        });
    };
  };
});
