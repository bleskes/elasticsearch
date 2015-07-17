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
  'use strict';
  var React = require('marvel/react');
  var D = React.DOM;
  var Shards = require('./shards');
  var calculateClass = require('../lib/calculateClass');
  var _ = require('lodash');
  var generateQueryAndLink = require('../lib/generateQueryAndLink');

  function sortByName(item) {
    if (item.type === 'node') {
      return [ !item.master, item.name];
    }
    return [ item.name ];
  }

  return React.createClass({
    createChild: function (data) {
      var key = data.id;
      var name = (
        React.createElement("a", {href:  generateQueryAndLink(data) }, 
          React.createElement("span", null,  data.name)
        )
      );
      var master;
      if (data.master) {
        master = (
          React.createElement("i", {className: "fa fa-star"})
        );
      }
      return (
        React.createElement("div", {className:  calculateClass(data, 'child'), key:  key }, 
          React.createElement("div", {className: "title"},  name,  master ), 
          React.createElement(Shards, {shards:  data.children})
        )
      );
    },
    render: function () {
      var data = _.sortBy(this.props.data, sortByName).map(this.createChild);
      return (
        React.createElement("td", {className: "children"},  data )
      );
    }
  });
});
