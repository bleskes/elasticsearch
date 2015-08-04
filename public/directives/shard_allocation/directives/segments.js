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



/* jshint newcap: false */
define(function (require) {
	var module = require('modules').get('marvel/directives', []);
  var React = require('react');
  var Segements = require('plugins/marvel/directives/shard_allocation/components/segments');
	module.directive('segments', function () {
		return {
			restrict: 'E',
			scope: {
				colors: '=colors',
				total: '=total'
			},
			link: function (scope, element) {
				var segments = Segements({ scope: scope });
				React.renderComponent(segments, element[0]);
			}
		};
	});
});


