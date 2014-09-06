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

package org.elasticsearch.shield.authz;

import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.transport.TransportRequest;

/**
 *
 */
public class SystemRole extends Permission.Global {

    public static final SystemRole INSTANCE = new SystemRole();

    public static final String NAME = "__es_system_role";
    private static final Predicate<String> PREDICATE = Privilege.SYSTEM.predicate();

    private SystemRole() {
    }

    public boolean check(String action) {
        return PREDICATE.apply(action);
    }

    @Override
    public boolean check(String action, TransportRequest request, MetaData metaData) {
        return PREDICATE.apply(action);
    }
}
