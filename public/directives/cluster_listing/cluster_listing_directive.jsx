var React = require('react');
var module = require('ui/modules').get('marvel/directives', []);
var Table = require('plugins/marvel/directives/paginated_table/components/table');
var ClusterRow = require('./components/cluster_row.jsx');

module.directive('marvelClusterListing', function (globalState, kbnUrl, $location) {
  return {
    restrict: 'E',
    scope: { clusters: '=' },
    link: function ($scope, $el) {

      var options = {
        title: 'Your Clusters',
        searchPlaceholder: 'Filter Clusters',
        // "key" properties are scalars used for sorting
        columns: [
          {
            key: 'cluster_name',
            sort: 1,
            title: 'Name'
          },
          {
            key: 'stats.nodes.count.total',
            sort: 0,
            title: 'Nodes'
          },
          {
            key: 'stats.indices.count',
            sort: 0,
            title: 'Indices'
          },
          {
            key: 'stats.nodes.jvm.max_uptime_in_millis',
            sort: 0,
            title: 'Uptime'
          },
          {
            key: 'stats.indices.store.size_in_bytes',
            sort: 0,
            title: 'Data'
          },
          {
            key: 'license.type',
            sort: 0,
            title: 'License'
          }
        ]
      };

      var table = React.render(<Table
        scope={ $scope }
        template={ ClusterRow }
        options={ options }/>, $el[0]);

      function changeCluster(name) {
        $scope.$evalAsync(function () {
          globalState.cluster = name;
          globalState.save();
          kbnUrl.changePath('/overview');
        });
      }

      $scope.$watch('clusters', (data) => {
        if (data) {
          data.forEach((cluster) => {
            cluster.changeCluster = changeCluster;
          });
          table.setData(data);
        }
      });
    }
  };
});
