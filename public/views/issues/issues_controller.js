define(function (require) {
  var _ = require('lodash');
  var angular = require('angular');
  var compareIssues = require('plugins/marvel/lib/compare_issues');
  require('plugins/marvel/services/settings');
  require('plugins/marvel/services/metrics');
  require('plugins/marvel/services/clusters');

  var module = require('ui/modules').get('marvel', [
    'marvel/directives',
    'marvel/settings',
    'marvel/metrics'
  ]);

  // require('ui/routes').when('/issues', {
  //   template: require('plugins/marvel/views/issues/issues_template.html'),
  //   resolve: {
  //     marvel: function (Private) {
  //       var routeInit = Private(require('plugins/marvel/lib/route_init'));
  //       return routeInit();
  //     }
  //   }
  // });

  module.controller('issues', function (courier, $http, $route, $scope, Promise, Private, timefilter, globalState) {
    var clusters = $route.current.locals.marvel.clusters;
    var indexPattern = $route.current.locals.marvel.indexPattern;
    // var IssueDataSource = Private(require('plugins/marvel/directives/issues/data_source'));
    // var ClusterStatusDataSource = Private(require('plugins/marvel/directives/cluster_status/data_source'));

    timefilter.enabled = true;
    if (timefilter.refreshInterval.value === 0) {
      timefilter.refreshInterval.value = 10000;
      timefilter.refreshInterval.display = '10 Seconds';
    }


    // // Fetch the cluster status
    // var dataSource = new ClusterStatusDataSource(indexPattern, globalState.cluster, clusters);
    // $scope.cluster_status = dataSource;
    // dataSource.register(courier);
    // courier.fetch();

    // $scope.$on('$destroy', function () {
    //   dataSource.destroy();
    // });

    // Fetch the issues
    $scope.issues = [];
    $scope.allIssues = [];
    function fetch() {
      return $http.get('/api/marvel/v1/issues/' + globalState.cluster).then(function (resp) {
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
        $scope.summaries = {
          total: { status: '', red: 0, yellow: 0 },
          cluster: { status: '', red: 0, yellow: 0 },
          node: { status: '', red: 0, yellow: 0 },
          index: { status: '', red: 0, yellow: 0 },
        };
        _.each($scope.allIssues, function (issue) {
          $scope.summaries.total[issue.status] += 1;
          $scope.summaries[issue.type][issue.status] += 1;
        });
        _.each($scope.summaries, function (summaries, key) {
          if (summaries.yellow > 0) summaries.status = 'yellow';
          if (summaries.red > 0) summaries.status = 'red';
        });
        return data;
      });
    }
    fetch();

    var unsubscribe = $scope.$on('courier:searchRefresh', function () {
      fetch();
    });
    $scope.$on('$destroy', unsubscribe);

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

    $scope.showAll = function () {
      $scope.filters = [];
      $scope.issues = filterIssues($scope.allIssues);
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
