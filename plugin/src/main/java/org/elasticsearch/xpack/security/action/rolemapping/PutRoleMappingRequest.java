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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xpack.security.authc.support.mapper.ExpressionRoleMapping;
import org.elasticsearch.xpack.security.authc.support.mapper.expressiondsl.RoleMapperExpression;
import org.elasticsearch.xpack.security.authc.support.mapper.expressiondsl.ExpressionParser;
import org.elasticsearch.xpack.security.support.MetadataUtils;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * Request object for adding/updating a role-mapping to the native store
 *
 * @see org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore
 */
public class PutRoleMappingRequest extends ActionRequest
        implements WriteRequest<PutRoleMappingRequest> {

    private String name = null;
    private boolean enabled = true;
    private List<String> roles = Collections.emptyList();
    private RoleMapperExpression rules = null;
    private Map<String, Object> metadata = Collections.emptyMap();
    private RefreshPolicy refreshPolicy = RefreshPolicy.IMMEDIATE;

    public PutRoleMappingRequest() {
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (name == null) {
            validationException = addValidationError("role-mapping name is missing",
                    validationException);
        }
            if (roles.isEmpty()) {
            validationException = addValidationError("role-mapping roles are missing",
                    validationException);
        }
        if (rules == null) {
            validationException = addValidationError("role-mapping rules are missing",
                    validationException);
        }
        if (MetadataUtils.containsReservedMetadata(metadata)) {
            validationException = addValidationError("metadata keys may not start with [" +
                    MetadataUtils.RESERVED_PREFIX + "]", validationException);
        }
        return validationException;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public void setRoles(List<String> roles) {
        this.roles = new ArrayList<>(roles);
    }

    public RoleMapperExpression getRules() {
        return rules;
    }

    public void setRules(RoleMapperExpression expression) {
        this.rules = expression;
    }

    @Override
    public PutRoleMappingRequest setRefreshPolicy(RefreshPolicy refreshPolicy) {
        this.refreshPolicy = refreshPolicy;
        return this;
    }

    /**
     * Should this request trigger a refresh ({@linkplain RefreshPolicy#IMMEDIATE}, the default),
     * wait for a refresh ({@linkplain RefreshPolicy#WAIT_UNTIL}), or proceed ignore refreshes
     * entirely ({@linkplain RefreshPolicy#NONE}).
     */
    @Override
    public RefreshPolicy getRefreshPolicy() {
        return refreshPolicy;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = Objects.requireNonNull(metadata);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        this.name = in.readString();
        this.enabled = in.readBoolean();
        this.roles = in.readList(StreamInput::readString);
        this.rules = ExpressionParser.readExpression(in);
        this.metadata = in.readMap();
        this.refreshPolicy = RefreshPolicy.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(name);
        out.writeBoolean(enabled);
        out.writeStringList(roles);
        ExpressionParser.writeExpression(rules, out);
        out.writeMap(metadata);
        refreshPolicy.writeTo(out);
    }

    public ExpressionRoleMapping getMapping() {
        return new ExpressionRoleMapping(
                name,
                rules,
                roles,
                metadata,
                enabled
        );
    }
}