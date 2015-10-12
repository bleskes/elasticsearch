define(function (require) {
  var _ = require('lodash');
  var chrome = require('ui/chrome');
  var tabs = require('./tabs');
  return function routeInitProvider(Notifier, marvelSettings, Private, marvelClusters, globalState, Promise, kbnUrl) {

    var initMarvelIndex = Private(require('plugins/marvel/lib/marvel_index_init'));
    var phoneHome = Private(require('plugins/marvel/lib/phone_home'));
    return function (options) {
      options = _.defaults(options || {}, {
        force: {
          settings: true
        }
      });

      var marvel = {};
      var notify = new Notifier({ location: 'Marvel' });
      return marvelClusters.fetch(true)
        .then(function (clusters) {
          return phoneHome.sendIfDue(clusters).then(() => {
            return clusters;
          });
        })
        // Get the clusters
        .then(function (clusters) {
          var cluster;
          marvel.clusters = clusters;
          // Check to see if the current cluster is available
          if (globalState.cluster && !_.find(clusters, { cluster_uuid: globalState.cluster })) {
            globalState.cluster = null;
          }
          // if there are no clusers choosen then set the first one
          if (!globalState.cluster) {
            cluster = _.first(clusters);
            if (cluster && cluster.cluster_uuid) {
              globalState.cluster = cluster.cluster_uuid;
              globalState.save();
            }
          }
          // if we don't have any clusters then redirect to setup
          if (!globalState.cluster) {
            notify.error('We can\'t seem to find any clusters in your Marvel data. Please check your Marvel agents');
            return kbnUrl.redirect('/home');
          }
          return globalState.cluster;
        })
        // Get the Marvel Settings
        .then(function (cluster) {
          return marvelSettings.fetch(cluster, options.force.settings)
            .then(function (settings) {
              marvel.settings = settings;
              return settings;
            });
        })
        // Get the Marvel Index Pattern
        .then(function (settings) {
          return initMarvelIndex().then(function (indexPattern) {
            marvel.indexPattern = indexPattern;
            return indexPattern;
          });
        })
        // Finally filter the cluster from the nav if it's light then return the Marvel object.
        .then(function () {
          var cluster = _.find(marvel.clusters, { cluster_uuid: globalState.cluster });
          var license = cluster.license;
          if ((new Date()).getTime() > license.expiry_date_in_millis) {
            kbnUrl.redirect('expired-license');
          }
          chrome.setTabs(tabs.filter(function (tab) {
            if (tab.id !== 'home') return true;
            if (license.type !== 'basic') return true;
            return false;
          }));
          return marvel;
        });
    };
  };
});
