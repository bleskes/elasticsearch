define(function(require) {
  /*
   * Accepts a function that is expecting a kibana IndexPattern, but of course for marvel
   *
   */
  return function marvelIndexPatternProvider(Private, indexPatterns) {
    var refreshKibanaIndex = Private(require('plugins/settings/sections/indices/_refresh_kibana_index'));
    return function () {
      // Setting the index pattern
      // FIXME Use Dependency Injection instead!!!
      var MarvelConfig = {
        indexPattern: '[.marvel-]YYYY.MM.DD',
        timeField: '@timestamp',
        intervalNAme: 'days'
      };

      function makeMarvelIndex() {
        // Corresponds with kibana/plugins/setting/sections/indices/_create.js:46
        // Some kind of utility perhaps?
        // Create the marvel index pattern if it isn't already
        indexPatterns.get()
          .then(function(indexPattern) {
            indexPattern.id = indexPattern.title = MarvelConfig.indexPattern;
            indexPattern.timeFieldName = MarvelConfig.timeField;
            indexPattern.intervalName = MarvelConfig.intervalName;

            // associate fields
            indexPatterns.mapper.clearCache(indexPattern.name)
              .then(function() {
                return indexPatterns.mapper.getFieldsForIndexPattern(pattern, true);
              });

            indexPattern.create()
              .then(function(id) {
                if(id) {
                  refreshKibanaIndex().then(function() {
                    indexPatterns.cache.clear(indexPattern.id);
                    done(indexPattern);
                  });
                }
              });
          });
      }

      return indexPatterns
        .get(MarvelConfig.indexPattern)
        .catch(function() {
          // if there is no indexPattern, then create it.
          // make marvel create it's own index on bootup
          alert('Create a marvel index first!!!');
        });
    };
  };
});
