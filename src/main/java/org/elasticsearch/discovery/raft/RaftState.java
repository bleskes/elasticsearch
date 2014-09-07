/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.discovery.raft;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;

public class RaftState {

    private long term;

    public static enum RaftRole {
        FOLLOWER,
        CANDIDATE,
        MASTER
    }

    private RaftRole role;

    @Nullable
    private DiscoveryNode votedFor;

    private long lastClusterStateTerm;
    private long lastClusterStateVersion;

    public RaftState() {
        term = 0;
        role = RaftRole.FOLLOWER;
        votedFor = null;
        lastClusterStateTerm = -1;
        lastClusterStateVersion = -1;
    }

    public synchronized long term() {
        return term;
    }

    public synchronized void term(long term) {
        if (this.term != term) {
            // vote doesn't hold for this term any more
            votedFor = null;
        }
        this.term = term;

    }

    public synchronized DiscoveryNode votedFor() {
        return votedFor;
    }

    public synchronized void votedFor(DiscoveryNode discoveryNode) {
        votedFor = discoveryNode;
    }

    public synchronized RaftRole role() {
        return role;
    }

    public synchronized void role(RaftRole role) {
        this.role = role;
    }

    public synchronized long lastClusterStateTerm() {
        return lastClusterStateTerm;
    }

    public synchronized void lastClusterStateTerm(long lastClusterStateTerm) {
        this.lastClusterStateTerm = lastClusterStateTerm;
    }

    public synchronized long lastClusterStateVersion() {
        return lastClusterStateVersion;
    }

    public synchronized void lastClusterStateVersion(long lastClusterStateVersion) {
        this.lastClusterStateVersion = lastClusterStateVersion;
    }


}
