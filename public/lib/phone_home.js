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

var _ = require('lodash');
module.exports = function phoneHomeProvider(Promise, es, $http, statsReportUrl, reportStats, features) {

  const defaults = {
    report: true,
    status: 'trial'
  };

  class PhoneHome {

    constructor() {
      this.attributes = {};
      try {
        var marvelData = localStorage.getItem('marvel_data');
        let attributes = marvelData && JSON.parse(marvelData) || {};
        _.defaults(this.attributes, attributes, defaults);
      } catch (e) {
        _.defaults(this.attributes, defaults);
      }
    }

    set(key, value) {
      var self = this;
      var previous;
      if (typeof key === 'object') {
        previous = _.pick(this.attributes, _.keys(key));
        this.attributes = _.assign(this.attributes, key);
      } else {
        previous = this.attributes[key];
        this.attributes[key] = value;
      }
    }

    get(key) {
      if (_.isUndefined(key)) {
        return this.attributes;
      } else {
        return this.attributes[key];
      }
    }

    saveToBrowser() {
      localStorage.setItem('marvel_data', JSON.stringify(this.attributes));
    }

    checkReportStatus() {
      var reportInterval = 86400000;
      var sendReport     = false;

      if (reportStats && features.isEnabled('report', true)) {
        // If the last report is empty it means we've never sent an report and
        // now is the time to send it.
        if (!this.get('lastReport')) {
          sendReport = true;
        }
        // If it's been a day since we last sent an report, send one.
        if (new Date().getTime() - parseInt(this.get('lastReport'), 10) > reportInterval) {
          sendReport = true;
        }
      }

      // If we need to send a report then we need to record the last time we
      // sent it and store it
      if (sendReport) {
        return true;
      }

      // If all else fails... don't send
      return false;
    }

    getClusterInfo(clusterUUID) {
      let url = `/api/marvel/v1/clusters/${clusterUUID}/info`;
      return $http.get(url).then((resp) => {
        return resp.data;
      });
    }

    sendIfDue(clusters) {
      var self = this;
      if (!this.checkReportStatus()) return Promise.resolve();
      return Promise.all(clusters.map((cluster) => {
        return this.getClusterInfo(cluster.cluster_uuid).then((info) => {
          const req = {
            method: 'POST',
            url: statsReportUrl,
            data: info,
            kbnXsrfToken: false
          };
          return $http(req);
        });
      })).then(() => {
        this.set('lastReport', Date.now());
        this.saveToBrowser();
      })
      .catch((err) => {
        // swallow!
        return Promise.resolve();
      });
    }

  }; // end class

  return (new PhoneHome());

};
