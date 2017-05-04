/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.rolemapping;

import java.io.IOException;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

/**
 * Response for a role-mapping being deleted from the
 * {@link org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore}
 */
public class DeleteRoleMappingResponse extends ActionResponse implements ToXContentObject {

    private boolean found = false;

    /**
     * Package private for {@link DeleteRoleMappingAction#newResponse()}
     */
    DeleteRoleMappingResponse() {}

    DeleteRoleMappingResponse(boolean found) {
        this.found = found;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject().field("found", found).endObject();
        return builder;
    }

    /**
     * If <code>true</code>, indicates the {@link DeleteRoleMappingRequest#getName() named role-mapping} was found and deleted.
     * Otherwise, the role-mapping could not be found.
     */
    public boolean isFound() {
        return this.found;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        found = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(found);
    }

}
