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

package org.elasticsearch.xpack.security.action.realm;

import org.elasticsearch.action.support.nodes.BaseNodeRequest;
import org.elasticsearch.action.support.nodes.BaseNodesRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 *
 */
public class ClearRealmCacheRequest extends BaseNodesRequest<ClearRealmCacheRequest> {

    String[] realms;
    String[] usernames;

    /**
     * @return  {@code true} if this request targets realms, {@code false} otherwise.
     */
    public boolean allRealms() {
        return realms == null || realms.length == 0;
    }

    /**
     * @return  The realms that should be evicted. Empty array indicates all realms.
     */
    public String[] realms() {
        return realms;
    }

    /**
     * Sets the realms for which caches will be evicted. When not set all the caches of all realms will be
     * evicted.
     *
     * @param realms    The realm names
     */
    public ClearRealmCacheRequest realms(String... realms) {
        this.realms = realms;
        return this;
    }

    /**
     * @return  {@code true} if this request targets users, {@code false} otherwise.
     */
    public boolean allUsernames() {
        return usernames == null || usernames.length == 0;
    }

    /**
     * @return  The usernames of the users that should be evicted. Empty array indicates all users.
     */
    public String[] usernames() {
        return usernames;
    }

    /**
     * Sets the usernames of the users that should be evicted from the caches. When not set, all users
     * will be evicted.
     *
     * @param usernames The usernames
     */
    public ClearRealmCacheRequest usernames(String... usernames) {
        this.usernames = usernames;
        return this;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        realms = in.readStringArray();
        usernames = in.readStringArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArrayNullable(realms);
        out.writeStringArrayNullable(usernames);
    }

    public static class Node extends BaseNodeRequest {

        String[] realms;
        String[] usernames;

        public Node() {
        }

        Node(ClearRealmCacheRequest request, String nodeId) {
            super(nodeId);
            this.realms = request.realms;
            this.usernames = request.usernames;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            realms = in.readStringArray();
            usernames = in.readStringArray();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeStringArrayNullable(realms);
            out.writeStringArrayNullable(usernames);
        }
    }
}
