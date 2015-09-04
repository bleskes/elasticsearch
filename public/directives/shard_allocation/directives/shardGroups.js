/**
 * ELASTICSEARCH CONFIDENTIAL
 * _____________________________
 *
 *  [2014] Elasticsearch Incorporated All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */



define(function (require) {
  var module = require('ui/modules').get('marvel/directives', []);
  var calculateClass = require('../lib/calculateClass');
  module.directive('shardGroups', function () {
    return {
      restrict: 'E',
      scope: {
        groups: '=groups',
        unassigned: '=unassigned'
      },
      templateUrl: '/kibana/app/panels/marvel/shard_allocation/directives/shardGroups.html',
      link: function (scope) {
        scope.calculateClass = calculateClass;
      }
    };
  });
});
