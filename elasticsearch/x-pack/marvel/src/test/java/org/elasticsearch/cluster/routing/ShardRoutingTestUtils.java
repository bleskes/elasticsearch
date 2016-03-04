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

public class ShardRoutingTestUtils {

    /**
     * Gives access to package private {@link ShardRouting#initialize(String, String, long)} method for test purpose.
     **/
    public static void initialize(ShardRouting shardRouting, String nodeId) {
        shardRouting.initialize(nodeId, null, -1);
    }

    /**
     * Gives access to package private {@link ShardRouting#moveToStarted()} method for test purpose.
     **/
    public static void moveToStarted(ShardRouting shardRouting) {
        shardRouting.moveToStarted();
    }

    /**
     * Gives access to package private {@link ShardRouting#relocate(String, long)} method for test purpose.
     **/
    public static void relocate(ShardRouting shardRouting, String nodeId) {
        shardRouting.relocate(nodeId, -1);
    }
}
