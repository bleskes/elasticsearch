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

package org.elasticsearch.shield.action.role;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.shield.authz.RoleDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response when retrieving a role from the shield index. Does not contain a
 * real {@code Role} object, only a {@code RoleDescriptor}.
 */
public class GetRolesResponse extends ActionResponse {
    private List<RoleDescriptor> roles;

    public GetRolesResponse() {
        roles = Collections.emptyList();
    }

    public GetRolesResponse(RoleDescriptor role) {
        this.roles = Collections.singletonList(role);
    }

    public GetRolesResponse(List<RoleDescriptor> roles) {
        this.roles = roles;
    }

    public List<RoleDescriptor> roles() {
        return roles;
    }

    public boolean isExists() {
        return roles != null && roles.size() > 0;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        roles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            roles.add(RoleDescriptor.readFrom(in));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(roles.size());
        for (RoleDescriptor role : roles) {
            RoleDescriptor.writeTo(role, out);
        }
    }
}
