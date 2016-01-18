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

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.shield.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response containing a User retrieved from the shield administrative index
 */
public class GetUsersResponse extends ActionResponse {
    private List<User> users;

    public GetUsersResponse() {
        this.users = Collections.emptyList();
    }

    public GetUsersResponse(User user) {
        this.users = Collections.singletonList(user);
    }

    public GetUsersResponse(List<User> users) {
        this.users = users;
    }

    public List<User> users() {
        return users;
    }

    public boolean isExists() {
        return users != null && users.size() > 0;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        users = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            users.add(User.readFrom(in));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(users == null ? 0 : users.size());
        for (User u : users) {
            User.writeTo(u, out);
        }
    }

}
