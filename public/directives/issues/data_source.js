define(function (require) {
  var _ = require('lodash');

  return function issuesDataSourceProvider($resource) {

    var Issues = $resource('/marvel/api/v1/issues/:cluster/:type');
    function IssueDataSource(cluster, type) {
      this.cluster = cluster;
      this.type = type;
      this.data = [];
    }

    IssueDataSource.prototype.register = function ($scope) {
      var self = this;
      $scope.$on('courier:searchRefresh', function () {
        self.fetch();
      });
    };

    IssueDataSource.prototype.fetch = function () {
      var self = this;
      Issues.query({ cluster: this.cluster, type: this.type }).$promise.then(function (data) {
        self.data = data;
      });
    };

    return IssueDataSource;

  };
});
