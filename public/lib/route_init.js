define(function (require) {
  return function routeInitProvider(Notifier, marvelClusterState, marvelSettings, Private, marvelClusters, globalState, Promise, kbnUrl) {

    var initMarvelIndex = Private(require('plugins/marvel/lib/marvel_index_init'));
    return function (options) {
      options = _.defaults(options || {}, {
        force: {
          settings: true
        }
      });

      var marvel = {};
      var notify = new Notifier({ location: 'Marvel' });
      return marvelClusters.fetch(true)
        // Get the clusters
        .then(function (clusters) {
          var cluster;
          marvel.clusters = clusters;
          // Check to see if the current cluster is available
          if (globalState.cluster && !_.find(clusters, { _id: globalState.cluster })) {
            globalState.cluster = null;
          }
          // if there are no clusers choosen then set the first one
          if (!globalState.cluster) {
            cluster = _.first(clusters);
            if (cluster && cluster._id) {
              globalState.cluster = cluster._id;
              globalState.save();
            }
          }
          // if we don't have any clusters then redirect to setup
          if (!globalState.cluster) {
            notify.error('We can\'t seem to find any clusters in your Marvel data. Please check your Marvel agents');
            return kbnUrl.redirect('/marvel/setup?reason=missing_cluster');
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
        .then(function (pattern) {
          marvelClusterState.setIndex(pattern);
          return pattern;
        })
        // Finally return the Marvel object.
        .then(function () {
          return marvel;
        });
    };
  };
});
