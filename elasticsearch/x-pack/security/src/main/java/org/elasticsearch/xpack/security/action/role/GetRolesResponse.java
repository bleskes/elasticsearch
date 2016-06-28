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

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;

import java.io.IOException;

/**
 * A response for the {@code Get Roles} API that holds the retrieved role descriptors.
 */
public class GetRolesResponse extends ActionResponse {

    private RoleDescriptor[] roles;

    public GetRolesResponse(RoleDescriptor... roles) {
        this.roles = roles;
    }

    public RoleDescriptor[] roles() {
        return roles;
    }

    public boolean hasRoles() {
        return roles.length > 0;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        roles = new RoleDescriptor[size];
        for (int i = 0; i < size; i++) {
            roles[i] = RoleDescriptor.readFrom(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(roles.length);
        for (RoleDescriptor role : roles) {
            RoleDescriptor.writeTo(role, out);
        }
    }
}
