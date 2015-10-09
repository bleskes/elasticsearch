const _ = require('lodash');
const mod = require('ui/modules').get('marvel/features', []);

mod.service('features', function ($window) {
  function getData() {
    const marvelData = $window.localStorage.getItem('marvel_data');
    return (marvelData && JSON.parse(marvelData)) || {};
  }

  function update(featureName, value) {
    const marvelDataObj = getData();
    marvelDataObj[featureName] = value;
    $window.localStorage.setItem('marvel_data', JSON.stringify(marvelDataObj));
  }

  function isEnabled(featureName, defaultSetting) {
    const marvelDataObj = getData();
    if (_.has(marvelDataObj, featureName)) {
      return marvelDataObj[featureName];
    }

    if (_.isUndefined(defaultSetting)) {
      return false;
    }

    return defaultSetting;
  }

  return {
    isEnabled,
    update
  };
});
