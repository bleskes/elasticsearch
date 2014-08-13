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
    private static final Predicate<String> PREDICATE = Privilege.INTERNAL.predicate();

    private SystemRole() {
    }

    @Override
    public boolean check(String action, TransportRequest request, MetaData metaData) {
        return PREDICATE.apply(action);
    }
}
