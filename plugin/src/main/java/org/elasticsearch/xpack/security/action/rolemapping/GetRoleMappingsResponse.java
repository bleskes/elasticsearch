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
import org.elasticsearch.xpack.security.authc.support.mapper.ExpressionRoleMapping;

/**
 * Response to {@link GetRoleMappingsAction get role-mappings API}.
 *
 * @see org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore
 */
public class GetRoleMappingsResponse extends ActionResponse {

    private ExpressionRoleMapping[] mappings;

    public GetRoleMappingsResponse(ExpressionRoleMapping... mappings) {
        this.mappings = mappings;
    }

    public ExpressionRoleMapping[] mappings() {
        return mappings;
    }

    public boolean hasMappings() {
        return mappings.length > 0;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        mappings = new ExpressionRoleMapping[size];
        for (int i = 0; i < size; i++) {
            mappings[i] = new ExpressionRoleMapping(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(mappings.length);
        for (ExpressionRoleMapping mapping : mappings) {
            mapping.writeTo(out);
        }
    }
}
