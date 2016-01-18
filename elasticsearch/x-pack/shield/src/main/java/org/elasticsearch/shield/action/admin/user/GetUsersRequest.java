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

package org.elasticsearch.shield.action.admin.user;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Collections;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * Request to retrieve a user from the shield administrative index from a username
 */
public class GetUsersRequest extends ActionRequest<GetUsersRequest> {

    private String[] users;

    public GetUsersRequest() {
        users = Strings.EMPTY_ARRAY;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (users == null) {
            validationException = addValidationError("users cannot be null", validationException);
        }
        return validationException;
    }

    public void users(String... usernames) {
        this.users = usernames;
    }

    public String[] users() {
        return users;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        users = in.readStringArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArray(users);
    }

}
