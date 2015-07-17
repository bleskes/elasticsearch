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
  'use strict';
  return function getStateSourceProvider(es) {
    return function (obj) {
      return es.get({
        index: obj._index,
        type: 'cluster_state',
        id: obj._id
      }).then(function (resp) {
        var state;
        if (resp && resp._source) {
          state = resp._source;
          state._id = obj._id;
          return state;
        }
        return false;
      });
    };
  };
});
