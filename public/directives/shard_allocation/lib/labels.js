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



define(function () {
  // The ui had different columns in different order depending on the
  // $scope.pane.view variable. This provides a lookup for the column headers
  // labels are linked to view from public/directives/shard_allocation/lib/changeData.js
  return {
    index: ['Nodes'], // "index detail" page shows nodes on which index shards are allocated
    node: ['Indices'], // "node detail" page shows the indexes that have shards on this node
    indexWithUnassigned: ['Unassigned', 'Nodes'] // NOTE: is this unused or is there even an indexWithUnassigned view?
  };
});
