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
        columns: [
          {
            key: 'cluster_uuid',
            sort: 1,
            title: 'Name'
          },
          {
            key: 'stats.node_count',
            sort: 0,
            title: 'Nodes'
          },
          {
            key: 'stats.indice_count',
            sort: 0,
            title: 'Indices'
          },
          {
            key: 'stats.uptime',
            sort: 0,
            title: 'Uptime'
          },
          {
            key: 'stats.data',
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
