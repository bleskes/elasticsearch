define(['marvel/lib/format_number', 'text!marvel/directives/cluster_status/index.html'],
function (formatNumber, template) {
  var module = require('modules').get('marvel/directives', []);
  module.directive('marvelClusterStatus', function ($route, courier, timefilter) {
    var marvelIndex = $route.current.locals.indexPattern;
    return {
      restrict: 'E',
      template: template,
      scope: {
        issues: '='
      },
      link: function($scope) {
        // TODO Make a Marvel Searchsource wrapper
        var searchSource = new courier.SearchSource()
          .set('index', marvelIndex)
          .set('size', 1)
          .set('sort', {'@timestamp': { order: 'desc' }})
          .set('query', '_type:cluster_stats');

        // Set the proper onResults resp
        searchSource.onResults(function(resp) {
          // FIXME don't get the object this way
          $scope.clusterInfo = convertResp(resp);
        });

        // Handle errors
        searchSource.onError(function(err) {
          console.log(err);
        });

        // listen for changes to the time for updates for now.
        $scope.$listen(timefilter, 'fetch', function() {
          searchSource.fetch
        });

        // TODO NOOOO!!!!!
        searchSource.fetch();
      }
    };
  });
  function convertResp(resp) {
    var rawClusterData = _(resp.hits.hits[0]._source);
    var clusterStatus = rawClusterData
      .pick(['status', 'cluster_name'])
      .value();

    function get(arg) {
      // TODO should be able to also get like everything, the second form of get
      return rawClusterData.get(arg);
    }

    clusterStatus.nodes_count = get('nodes.count.total');
    clusterStatus.shards_count = get('indices.shards.total');
    clusterStatus.replication_factor = get('indices.shards.index.replication.min');
    clusterStatus.document_count = formatNumber(get('indices.docs.count'), 'int_commas');
    // TODO check this one, probably wrong
    clusterStatus.data = formatNumber(get('nodes.fs.available_in_bytes'), 'byte');
    clusterStatus.upTime = formatNumber(get('nodes.jvm.max_uptime_in_millis'), 'time_since');
    clusterStatus.version = get('nodes.versions[0]');
    return clusterStatus;
  }
});
