define(function (require) {
  var React = require('react');
  var module = require('ui/modules').get('plugins/marvel/directives', []);
  var MarvelChart = require('plugins/marvel/directives/chart/chart_component');

  module.directive('marvelChart', function (marvelMetrics, $route, Private, courier, timefilter) {
    return {
      restrict: 'E',
      scope: {
        source: '='
      },
      link: function ($scope, $elem, attrs) {
        var $chart = React.createElement(MarvelChart, {
          source: $scope.source
        });

        var Sparkline = React.render($chart, $elem[0]);

        $scope.$watch('source.error', function (err) {
          $scope.err = err;
        });
        $scope.$watch('source.data', function (data) {
          Sparkline.setData(data);
        });
      }
    };

  });
});

