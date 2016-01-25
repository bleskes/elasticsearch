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

package org.elasticsearch.shield.authz.privilege;

/**
 *
 */
public class HealthAndStatsPrivilege extends GeneralPrivilege {

    public static final HealthAndStatsPrivilege INSTANCE = new HealthAndStatsPrivilege();

    public static final String NAME = "health_and_stats";

    private HealthAndStatsPrivilege() {
        super(NAME, "cluster:monitor/health*",
                    "cluster:monitor/stats*",
                    "indices:monitor/stats*",
                    "cluster:monitor/nodes/stats*");
    }
}
