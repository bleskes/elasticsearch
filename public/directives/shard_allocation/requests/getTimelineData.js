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
  return function getTimelineDataProvider(es, timefilter) {

    return function getTimelineData(config, size, timeRange, data, position) {
      var newPosition = false;
      var bounds = timefilter.getBounds();
      size = _.isUndefined(size) ? 300 : size;
      data = _.isUndefined(data) ? [] : data;
      position = _.isUndefined(position) ? 0 : position;

      if (_.isUndefined(timeRange)) {
        timeRange = {
          gte: bounds.min.valueOf(),
          lte: bounds.max.valueOf(),
          format: 'epoch_millis'
        };
      }

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
              range: {
                '@timestamp': timeRange
              }
            }
          }
        }
      };

      return es.search({
        index: 'marvel-*',
        type: 'cluster_state',
        body: body
      })
        .then(function (resp) {
          var nextTimeRange;
          var hits = resp.data.hits;
          data.push.apply(data, hits.hits);

          if (hits.hits.length === hits.total) {
            position++;
            newPosition = dashboard.indices[position] ? true : false;
          }

          var to = timeRange.lte;
          if (hits.hits.length > 0) {
            to = moment(getValueFromArrayOrString(hits.hits[hits.hits.length-1].fields['@timestamp'])).valueOf();
          }

          if ((hits.total > size && hits.hits.length === size) || newPosition) {
            nextTimeRange = {
              lte: to,
              gte: timeRange.gte,
              format: 'epoch_millis'
            };
            return getTimelineData(config, size, nextTimeRange, data, position); // call again
          }
          // flip data back to normal order
          return data.reverse();
        })
        .catch(function (resp) {
          $scope.panel.error = resp.data.error;
          position++;
          if (dashboard.indices[position]) {
            return getTimelineData(config, size, timeRange, data, position); // call again
          }
          return data.reverse();
        });
    };

  };

});
