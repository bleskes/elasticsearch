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

package org.elasticsearch.xpack.security.action.role;

import org.elasticsearch.action.support.nodes.BaseNodeRequest;
import org.elasticsearch.action.support.nodes.BaseNodesRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * The request used to clear the cache for native roles stored in an index.
 */
public class ClearRolesCacheRequest extends BaseNodesRequest<ClearRolesCacheRequest> {

    String[] names;

    /**
     * Sets the roles for which caches will be evicted. When not set all the roles will be evicted from the cache.
     *
     * @param names    The role names
     */
    public ClearRolesCacheRequest names(String... names) {
        this.names = names;
        return this;
    }

    /**
     * @return an array of role names that will have the cache evicted or <code>null</code> if all
     */
    public String[] names() {
        return names;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        names = in.readOptionalStringArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalStringArray(names);
    }

    public static class Node extends BaseNodeRequest {
        String[] names;

        public Node() {
        }

        public Node(ClearRolesCacheRequest request, String nodeId) {
            super(nodeId);
            this.names = request.names();
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            names = in.readOptionalStringArray();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeOptionalStringArray(names);
        }
    }
}
