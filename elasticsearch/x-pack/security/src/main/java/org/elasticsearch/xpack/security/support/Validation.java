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

package org.elasticsearch.xpack.security.support;

import java.util.regex.Pattern;

/**
 *
 */
public final class Validation {

    private static final Pattern COMMON_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_@\\-\\$\\.]{0,29}");

    public static final class Users {

        private static final int MIN_PASSWD_LENGTH = 6;

        public static Error validateUsername(String username) {
            return COMMON_NAME_PATTERN.matcher(username).matches() ?
                    null :
                    new Error("A valid username must be at least 1 character and no longer than 30 characters. " +
                            "It must begin with a letter (`a-z` or `A-Z`) or an underscore (`_`). Subsequent " +
                            "characters can be letters, underscores (`_`), digits (`0-9`) or any of the following " +
                            "symbols `@`, `-`, `.` or `$`");
        }

        public static Error validatePassword(char[] password) {
            return password.length >= MIN_PASSWD_LENGTH ?
                    null :
                    new Error("passwords must be at least [" + MIN_PASSWD_LENGTH + "] characters long");
        }

    }

    public static final class Roles {

        public static Error validateRoleName(String roleName) {
            return COMMON_NAME_PATTERN.matcher(roleName).matches() ?
                    null :
                    new Error("A valid role name must be at least 1 character and no longer than 30 characters. " +
                            "It must begin with a letter (`a-z` or `A-Z`) or an underscore (`_`). Subsequent " +
                            "characters can be letters, underscores (`_`), digits (`0-9`) or any of the following " +
                            "symbols `@`, `-`, `.` or `$`");
        }
    }

    public static class Error {

        private final String message;

        private Error(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
