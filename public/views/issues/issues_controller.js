define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var compareIssues = require('marvel/lib/compare_issues');
  require('marvel/services/settings');
  require('marvel/services/metrics');
  require('marvel/services/clusters');

  var module = require('modules').get('marve', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics',
    'pasvaz.bindonce'
  ]);

  require('routes').when('/marvel/issues', {
    template: require('text!marvel/views/issues/issues_template.html'),
    resolve: {
      marvel: function (Private) {
        var routeInit = Private(require('marvel/lib/route_init'));
        return routeInit();
      }
    }
  });

  module.controller('issues', function (courier, $http, $route, $scope, Promise, Private, timefilter, globalState) {
    var clusters = $route.current.locals.marvel.clusters;
    var indexPattern = $route.current.locals.marvel.indexPattern;
    var IssueDataSource = Private(require('marvel/directives/issues/data_source'));
    var ClusterStatusDataSource = Private(require('marvel/directives/cluster_status/data_source'));

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }


    // Fetch the cluster status
    var dataSource = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    $scope.cluster_status = dataSource;
    dataSource.register(courier);
    courier.fetch();

    // Fetch the issues
    $scope.issues = [];
    $scope.allIssues = [];
    function fetch() {
      return $http.get('/marvel/api/v1/issues/' + globalState.cluster).then(function (resp) {
        var data = [];
        var body = resp.data;
        _.each(body, function (rows, type) {
          data = data.concat(_.map(rows, function (row) {
            row.type = type;
            return row;
          }));
        });
        data.sort(compareIssues);
        $scope.issues = filterIssues(data);
        $scope.allIssues = data;
        return data;
      });
    }
    fetch();

    $scope.$on('courier:searchRefresh', function () {
      fetch();
    });

    $scope.filters = [ ];

    function filterIssues(issues) {
      var types = _.filter($scope.filters, function (obj) {
        return _.has(obj, 'type');
      });
      var statuses = _.filter($scope.filters, function (obj) {
        return _.has(obj, 'status');
      });
      return _.filter(issues, function (issue) {
        var hasTypes = types.length === 0 || _.some(types, function (filter) {
          if (filter.type) return issue.type === filter.type;
          return false;
        });
        var hasStauses = statuses.length === 0 || _.some(statuses, function (filter) {
          if (filter.status) return issue.status === filter.status;
          return false;
        });
        return hasTypes && hasStauses;
      });
    }

    $scope.isActive = function (filter) {
      return !!(_.find($scope.filters, filter));
    };

    $scope.toggleFilter = function (filter) {
      if ($scope.isActive(filter)) {
        $scope.filters = _.reject($scope.filters, filter);
      } else {
        $scope.filters.push(filter);
      }
      $scope.issues = filterIssues($scope.allIssues);
    };

  });
});
