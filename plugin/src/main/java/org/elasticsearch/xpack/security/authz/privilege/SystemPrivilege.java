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

package org.elasticsearch.xpack.security.authz.privilege;

import org.elasticsearch.xpack.security.support.Automatons;

import java.util.Collections;
import java.util.function.Predicate;

public final class SystemPrivilege extends Privilege {

    public static SystemPrivilege INSTANCE = new SystemPrivilege();

    private static final Predicate<String> PREDICATE = Automatons.predicate(Automatons.
            minusAndMinimize(Automatons.patterns(
            "internal:*",
            "indices:monitor/*", // added for monitoring
            "cluster:monitor/*",  // added for monitoring
            "cluster:admin/reroute", // added for DiskThresholdDecider.DiskListener
            "indices:admin/mapping/put" // needed for recovery and shrink api
    ), Automatons.patterns("internal:transport/proxy/*"))); // no proxy actions for system user!

    private SystemPrivilege() {
        super(Collections.singleton("internal"));
    }

    @Override
    public Predicate<String> predicate() {
        return PREDICATE;
    }
}
