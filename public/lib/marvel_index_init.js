define(function (require) {
  /*
   * Accepts a function that is expecting a kibana IndexPattern, but of course for marvel
   *
   */
  return function marvelIndexPatternProvider(marvelIndexPrefix, Private, indexPatterns) {
    return function () {
      var MarvelConfig = {
        indexPattern: `[${marvelIndexPrefix}]YYYY.MM.DD`,
        timeField: 'timestamp',
        intervalName: 'days'
      };
      return indexPatterns.get(MarvelConfig.indexPattern);
    };
  };
});
