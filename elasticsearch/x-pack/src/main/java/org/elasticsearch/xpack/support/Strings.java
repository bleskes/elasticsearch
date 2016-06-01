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

package org.elasticsearch.xpack.support;

import java.util.Objects;

public class Strings {
    private Strings() {
    }

    public static String join(String delimiter, int... values) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(values);
        if (values.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(4 * values.length);
        sb.append(values[0]);

        for (int i = 1; i < values.length; i++) {
            sb.append(delimiter).append(values[i]);
        }

        return sb.toString();
    }
}
