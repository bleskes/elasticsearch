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

package org.elasticsearch.shield.action.user;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.shield.user.User;

import java.io.IOException;
import java.util.Collection;

/**
 * Response containing a User retrieved from the shield administrative index
 */
public class GetUsersResponse extends ActionResponse {

    private User[] users;

    public GetUsersResponse(User... users) {
        this.users = users;
    }

    public GetUsersResponse(Collection<User> users) {
        this(users.toArray(new User[users.size()]));
    }

    public User[] users() {
        return users;
    }

    public boolean hasUsers() {
        return users != null && users.length > 0;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        if (size < 0) {
            users = null;
        } else {
            users = new User[size];
            for (int i = 0; i < size; i++) {
                users[i] = User.readFrom(in);
            }
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(users == null ? -1 : users.length);
        if (users != null) {
            for (User user : users) {
                User.writeTo(user, out);
            }
        }
    }

}
