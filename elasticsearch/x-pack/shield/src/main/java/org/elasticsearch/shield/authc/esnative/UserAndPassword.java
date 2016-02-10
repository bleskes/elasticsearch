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

package org.elasticsearch.shield.authc.esnative;

import org.elasticsearch.shield.user.User;

/**
 * Like User, but includes the hashed password
 *
 * NOT to be used for password verification
 *
 * NOTE that this purposefully does not serialize the {@code passwordHash}
 * field, because this is not meant to be used for security other than
 * retrieving the UserAndPassword from the index before local authentication.
 */
class UserAndPassword {

    private final User user;
    private final char[] passwordHash;

    UserAndPassword(User user, char[] passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
    }

    public User user() {
        return this.user;
    }

    public char[] passwordHash() {
        return this.passwordHash;
    }

    @Override
    public boolean equals(Object o) {
        return false; // Don't use this for user comparison
    }

    @Override
    public int hashCode() {
        int result = this.user.hashCode();
        result = 31 * result + passwordHash().hashCode();
        return result;
    }
}
