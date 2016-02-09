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

import org.elasticsearch.shield.support.AutomatonPredicate;

import java.util.function.Predicate;

import static org.elasticsearch.shield.support.Automatons.patterns;

/**
 *
 */
public class SystemPrivilege extends Privilege<SystemPrivilege> {

    public static SystemPrivilege INSTANCE = new SystemPrivilege();

    protected static final Predicate<String> PREDICATE = new AutomatonPredicate(patterns(
            "internal:*",
            "indices:monitor/*", // added for marvel
            "cluster:monitor/*",  // added for marvel
            "cluster:admin/reroute" // added for DiskThresholdDecider.DiskListener
    ));

    SystemPrivilege() {
        super(new Name("internal"));
    }

    @Override
    public Predicate<String> predicate() {
        return PREDICATE;
    }

    @Override
    public boolean implies(SystemPrivilege other) {
        return true;
    }
}
