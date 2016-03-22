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

package org.elasticsearch.shield.audit.index;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public enum IndexAuditLevel {

    ANONYMOUS_ACCESS_DENIED,
    AUTHENTICATION_FAILED,
    ACCESS_GRANTED,
    ACCESS_DENIED,
    TAMPERED_REQUEST,
    CONNECTION_GRANTED,
    CONNECTION_DENIED,
    SYSTEM_ACCESS_GRANTED,
    RUN_AS_GRANTED,
    RUN_AS_DENIED;

    static EnumSet<IndexAuditLevel> parse(List<String> levels) {
        EnumSet<IndexAuditLevel> enumSet = EnumSet.noneOf(IndexAuditLevel.class);
        for (String level : levels) {
            String lowerCaseLevel = level.trim().toLowerCase(Locale.ROOT);
            switch (lowerCaseLevel) {
                case "_all":
                    enumSet.addAll(Arrays.asList(IndexAuditLevel.values()));
                    break;
                case "anonymous_access_denied":
                    enumSet.add(ANONYMOUS_ACCESS_DENIED);
                    break;
                case "authentication_failed":
                    enumSet.add(AUTHENTICATION_FAILED);
                    break;
                case "access_granted":
                    enumSet.add(ACCESS_GRANTED);
                    break;
                case "access_denied":
                    enumSet.add(ACCESS_DENIED);
                    break;
                case "tampered_request":
                    enumSet.add(TAMPERED_REQUEST);
                    break;
                case "connection_granted":
                    enumSet.add(CONNECTION_GRANTED);
                    break;
                case "connection_denied":
                    enumSet.add(CONNECTION_DENIED);
                    break;
                case "system_access_granted":
                    enumSet.add(SYSTEM_ACCESS_GRANTED);
                    break;
                case "run_as_granted":
                    enumSet.add(RUN_AS_GRANTED);
                    break;
                case "run_as_denied":
                    enumSet.add(RUN_AS_DENIED);
                    break;
                default:
                    throw new IllegalArgumentException("invalid event name specified [" + level + "]");
            }
        }
        return enumSet;
    }

    public static EnumSet<IndexAuditLevel> parse(List<String> includeLevels, List<String> excludeLevels) {
        EnumSet<IndexAuditLevel> included = parse(includeLevels);
        EnumSet<IndexAuditLevel> excluded = parse(excludeLevels);
        included.removeAll(excluded);
        return included;
    }
}
