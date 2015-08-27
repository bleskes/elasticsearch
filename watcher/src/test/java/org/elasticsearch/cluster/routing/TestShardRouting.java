/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.cluster.routing;

/**
 * A helper that allows to create shard routing instances within tests, while not requiring to expose
 * different simplified constructors on the ShardRouting itself.
 */
public class TestShardRouting {

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId, boolean primary, ShardRoutingState state, long version) {
        return new ShardRouting(index, shardId, currentNodeId, null, null, primary, state, version, null, AllocationId.newInitializing(), true, ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE);
    }

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId, String relocatingNodeId, boolean primary, ShardRoutingState state, long version) {
        return new ShardRouting(index, shardId, currentNodeId, relocatingNodeId, null, primary, state, version, null, AllocationId.newInitializing(), true, ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE);
    }

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId, String relocatingNodeId, RestoreSource restoreSource, boolean primary, ShardRoutingState state, long version) {
        return new ShardRouting(index, shardId, currentNodeId, relocatingNodeId, restoreSource, primary, state, version, null, AllocationId.newInitializing(), true, ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE);
    }

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId,
                                               String relocatingNodeId, RestoreSource restoreSource, boolean primary, ShardRoutingState state, long version,
                                               UnassignedInfo unassignedInfo) {
        return new ShardRouting(index, shardId, currentNodeId, relocatingNodeId, restoreSource, primary, state, version, unassignedInfo, AllocationId.newInitializing(), true, ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE);
    }
}
