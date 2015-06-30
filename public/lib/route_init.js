define(function (require) {
  return function routeInitProvider(marvelSettings, Private, marvelClusters, globalState, Promise) {

    var initMarvelIndex = Private(require('marvel/lib/marvel_index_init'));
    return function (options) {
      options = _.defaults(options || {}, {
        force: {
          settings: false
        }
      });

      var marvel = {};
      return marvelClusters.fetch()
        // Get the clusters
        .then(function (clusters) {
          if (!globalState.cluster) {
            globalState.cluster = _.first(clusters)._id;
            globalState.save();
          }
          marvel.clusters = clusters;
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
        // Finally return the Marvel object.
        .then(function () {
          return marvel;
        });
    };
  };
});
