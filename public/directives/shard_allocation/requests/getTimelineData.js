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



define(function (require) {
  'use strict';
  var _ = require('lodash');
  var getValueFromArrayOrString = require('../lib/getValueFromArrayOrString');
  var moment = require('moment');
  return function getTimelineDataProvider(es, timefilter) {

    return function getTimelineData(indexPattern, cluster, size, timeRange, data) {
      var newPosition = false;
      var bounds = timefilter.getBounds();
      size = _.isUndefined(size) ? 300 : size;
      data = _.isUndefined(data) ? [] : data;

      if (_.isUndefined(timeRange)) {
        timeRange = {
          gte: bounds.min.valueOf(),
          lte: bounds.max.valueOf(),
          format: 'epoch_millis'
        };
      }

      var header = { index: indexPattern.toIndexList(moment(timeRange.gte), moment(timeRange.lte)), type: 'cluster_state' };
      var body = {
        size: size,
        from: 0,
        fields: ['@timestamp', 'message', 'status'],
        sort: {
          '@timestamp': { order: 'desc' }
        },
        query: {
          filtered: {
            filter: {
              bool: {
                must: [
                  {
                    range: {
                      '@timestamp': timeRange
                    }
                  },
                  {
                    term: { 'cluster_name.raw': cluster }
                  }
                ]
              }
            }
          }
        }
      };

      return es.msearch({ body: [ header, body ] })
        .then(function (resp) {
          var nextTimeRange;
          var hits = resp.responses[0].hits;
          var to = timeRange.lte;

          if (hits.hits.length > 0) {
            _.each(hits.hits, function (hit) {
              if (!_.find(data, { _id: hit._id })) data.push(hit);
            });
            to = moment(getValueFromArrayOrString(_.last(hits.hits).fields['@timestamp'])).valueOf();
          }

          if (hits.total > data.length && hits.hits.length === size) {
            nextTimeRange = {
              gte: timeRange.gte,
              lte: to,
              format: 'epoch_millis'
            };
            return getTimelineData(indexPattern, cluster, size, nextTimeRange, data); // call again
          }
          // flip data back to normal order
          return data.reverse();
        })
        .catch(function (resp) {
          return data.reverse();
        });
    };

  };

});
