define(function (require) {
  var _ = require('lodash');
  var numeral = require('numeral');
  var moment = require('moment');
  var template = require('text!marvel/directives/index_listing/index.html');
  var module = require('modules').get('marvel/directives', []);
  module.directive('marvelIndexListing', function () {
    return {
      restrict: 'E',
      template: template,
      scope: {},
      link: function ($scope) {

        function random(value, format) {
          format = format || '0,0';
          return numeral(Math.random()*value).format(format);
        }
        $scope.indices = [];
        _.times(10, function (n) {
          $scope.indices.push({
            name: moment.utc().subtract(n, 'days').format('[logstash-]YYYY.MM.DD'),
            documents: random(1000000000, '0.0a'),
            index_rate: random(5, '0.00'),
            search_rate: random(100, '0.00'),
            merge_rate: random(100000, '0.00a'),
            field_data: random(100000000, '0.00b')
          });
        });

      }
    };
  });
});

