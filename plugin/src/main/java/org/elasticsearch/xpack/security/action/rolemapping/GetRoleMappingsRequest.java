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

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * Request to retrieve role-mappings from X-Pack security
 *
 * @see org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore
 */
public class GetRoleMappingsRequest extends ActionRequest {

    private String[] names = Strings.EMPTY_ARRAY;

    public GetRoleMappingsRequest() {
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (names == null) {
            validationException = addValidationError("role-mapping names are missing",
                    validationException);
        }
        return validationException;
    }

    /**
     * Specify (by name) which role-mappings to delete.
     * @see org.elasticsearch.xpack.security.authc.support.mapper.ExpressionRoleMapping#getName()
     */
    public void setNames(String... names) {
        this.names = names;
    }

    /**
     * @see #setNames(String...)
     */
    public String[] getNames() {
        return names;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        names = in.readStringArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArray(names);
    }
}
