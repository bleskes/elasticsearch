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

package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum IgnoreDowntime {
    NEVER, ONCE, ALWAYS;

    /**
     * <p>
     * Parses a string and returns the corresponding enum value.
     * </p>
     * <p>
     * The method differs from {@link #valueOf(String)} by being
     * able to handle leading/trailing whitespace and being case
     * insensitive.
     * </p>
     * <p>
     * If there is no match {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param value A String that should match one of the enum values
     * @return the matching enum value
     */
    @JsonCreator
    public static IgnoreDowntime fromString(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
