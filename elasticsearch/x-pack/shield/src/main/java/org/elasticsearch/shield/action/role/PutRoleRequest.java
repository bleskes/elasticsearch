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

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.shield.authz.RoleDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * Request object for adding a role to the shield index
 */
public class PutRoleRequest extends ActionRequest<PutRoleRequest> implements ToXContent {

    private String name;
    private String[] clusterPrivileges;
    private List<RoleDescriptor.IndicesPrivileges> indicesPrivileges = new ArrayList<>();
    private String[] runAs;
    
    public PutRoleRequest() {
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (name == null) {
            validationException = addValidationError("role name is missing", validationException);
        }
        return validationException;
    }

    public void source(String name, BytesReference source) throws Exception {
        RoleDescriptor descriptor = RoleDescriptor.source(name, source);
        this.name = descriptor.getName();
        this.clusterPrivileges = descriptor.getClusterPrivileges();
        this.indicesPrivileges = Arrays.asList(descriptor.getIndicesPrivileges());
        this.runAs = descriptor.getRunAs();
    }

    public void name(String name) {
        this.name = name;
    }

    public void cluster(String... clusterPrivileges) {
        this.clusterPrivileges = clusterPrivileges;
    }

    public void addIndex(String[] indices, String[] privileges, @Nullable String[] fields, @Nullable BytesReference query) {
        this.indicesPrivileges.add(RoleDescriptor.IndicesPrivileges.builder()
                .indices(indices)
                .privileges(privileges)
                .fields(fields)
                .query(query)
                .build());
    }

    public void runAs(String... usernames) {
        this.runAs = usernames;
    }

    public String name() {
        return name;
    }

    public String[] cluster() {
        return clusterPrivileges;
    }

    public RoleDescriptor.IndicesPrivileges[] indices() {
        return indicesPrivileges.toArray(new RoleDescriptor.IndicesPrivileges[indicesPrivileges.size()]);
    }

    public String[] runAs() {
        return runAs;
    }

    private RoleDescriptor roleDescriptor() {
        return new RoleDescriptor(name, clusterPrivileges,
                indicesPrivileges.toArray(new RoleDescriptor.IndicesPrivileges[indicesPrivileges.size()]), runAs);
    }
    
    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        name = in.readString();
        clusterPrivileges = in.readOptionalStringArray();
        int indicesSize = in.readVInt();
        indicesPrivileges = new ArrayList<>(indicesSize);
        for (int i = 0; i < indicesSize; i++) {
            indicesPrivileges.add(RoleDescriptor.IndicesPrivileges.readIndicesPrivileges(in));
        }
        runAs = in.readOptionalStringArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(name);
        out.writeOptionalStringArray(clusterPrivileges);
        out.writeVInt(indicesPrivileges.size());
        for (RoleDescriptor.IndicesPrivileges index : indicesPrivileges) {
            index.writeTo(out);
        }
        out.writeOptionalStringArray(runAs);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return this.roleDescriptor().toXContent(builder, params);
    }
}
