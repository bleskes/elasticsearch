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
  var _ = require('lodash');

  return function (shards, nodes) {
    function setNodeName(shard) {
      var node = nodes[shard.node];
      shard.nodeName = (node && node.name) || null;
      shard.type = 'shard';
      if (shard.state === 'INITIALIZING' && shard.relocating_node) {
        shard.relocating_message = 'Relocating from ' + nodes[shard.relocating_node].name;
      }
      if (shard.state === 'RELOCATING') {
        shard.relocating_message = 'Relocating to ' + nodes[shard.relocating_node].name;
      }
      return shard;
    }

    return shards.map(setNodeName);
  };

});
