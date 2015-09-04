var _ = require('lodash');

function Model(data, options) {
  this.options = options || {};
  this.data = Model.explode(data);
}

Model.prototype.get = function (key) {
  return _.get(this.data, key);
};

Model.prototype.set = function (key, val) {
  var self = this;
  if (_.isPlainObject(key)) {
    _.each(Model.flatten(key), function (val, key) {
      _.get(self.data, key, val);
    });
  } else {
    _.set(this.data, key, val);
  }
};

Model.prototype.toObject = function (options) {
  options = _.defaults({}, options, this.options);
  var data = this.data;
  if (options.flatten) data = Model.flatten(data);
  return data;
};

Model.prototype.toJSON = function () {
  return this.data;
};

Model.stripEmpties = function (obj) {
  for (var i in obj) {
    if (_.isEmpty(obj[i])) {
      delete obj[i];
    } else if (typeof obj[i] === 'object') {
      Model.stripEmpties(obj[i]);
    }
  }
  return obj;
};

Model.flatten = function flatten(obj, path, newObj) {
  newObj = newObj || {};
  path = path || [];
  for (var i in obj) {
    if (_.isPlainObject(obj[i]) && !_.isArray(obj[i])) {
      flatten(obj[i], path.concat(i), newObj);
    } else {
      newObj[path.concat(i).join('.')] = obj[i];
    }
  }
  return newObj;
};

Model.explode = function explode(obj) {
  var newObj = {};
  _.each(obj, function (val, key) {
    _.set(newObj, key, val);
  });
  return newObj;
};

module.exports = Model;
