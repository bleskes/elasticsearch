define(function (require) {
  var _ = require('lodash');

  return function issuesDataSourceProvider(marvelClusters, marvelMetrics, $resource) {

    var Issues = $resource('/marvel/api/v1/issues/:cluster/:type');
    function IssueDataSource(cluster, type) {
      this.cluster = cluster;
      this.type = type;
      this.data = [];
    }

    IssueDataSource.prototype.register = function ($scope) {
      var self = this;
      return $scope.$on('courier:searchRefresh', function () {
        self.fetch();
      });
    };

    IssueDataSource.prototype.fetch = function () {
      var self = this;
      return  Issues.query({ cluster: this.cluster, type: this.type })
        .$promise
        .then(function (data) {
          self.data = data;
          return data;
        })
      .then(function (data) {
        return Promise.all(_.map(data, function (issue) {
          return marvelMetrics(self.cluster, issue.id)
            .then(function (metric) {
              issue.metric = metric;
              return issue;
            });
        }));
      })
      .then(function (issues) {
        return marvelClusters.fetch().then(function (clusters) {
          var cluster = _.find(clusters, { cluster_uuid: self.cluster });
          return Promise.all(_.map(issues, function (issue) {
            if (issue.node && cluster.nodes && cluster.nodes[issue.node]) {
              issue.node = cluster.nodes[issue.node];
            }
            return issue;
          }));
        });
      });
    };

    return IssueDataSource;

  };
});
