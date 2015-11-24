define(function (require) {
  var _ = require('lodash');
  return function SettingModelProvider($resource) {

    var Resource = $resource('/api/marvel/v1/settings/:id', { id: '@_id' });
    var Model = require('plugins/marvel/lib/model');

    function Settings(data, options) {
      Model.call(this, data, options);
      this.options.stripEmpties = true;
    }

    Settings.prototype = new Model();

    Settings.prototype.save = function () {
      var self = this;
      var resource = new Resource(this.data);
      return resource.$save().then(function () {
        return self;
      });
    };

    Settings.fetchById = function (id) {
      return Resource.get({ id: id }).$promise.then(function (data) {
        return new Settings(data);
      });
    };

    Settings.bulkFetch = function (cluster) {
      return Resource.query().$promise.then(function (data) {
        return _.map(data, function (doc) {
          return new Settings(doc);
        });
      });
    };

    return Settings;

  };
});
