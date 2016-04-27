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

package org.elasticsearch.xpack.watcher.support.validation;

import org.elasticsearch.common.logging.LoggerMessageFormat;

import java.util.regex.Pattern;

/**
 *
 */
public class Validation {

    private static final Pattern NO_WS_PATTERN = Pattern.compile("\\S+");

    public static Error watchId(String id) {
        if (!NO_WS_PATTERN.matcher(id).matches()) {
            return new Error("Invalid watch id [{}]. Watch id cannot have white spaces", id);
        }
        return null;
    }

    public static Error actionId(String id) {
        if (!NO_WS_PATTERN.matcher(id).matches()) {
            return new Error("Invalid action id [{}]. Action id cannot have white spaces", id);
        }
        return null;
    }

    public static class Error {

        private final String message;

        public Error(String message, Object... args) {
            this.message = LoggerMessageFormat.format(message, args);
        }

        public String message() {
            return message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
