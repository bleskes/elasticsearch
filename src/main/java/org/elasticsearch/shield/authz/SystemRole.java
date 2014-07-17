package org.elasticsearch.shield.authz;

import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.transport.TransportRequest;

/**
 *
 */
public class SystemRole extends Permission  {

    public static final String NAME = "__es_system_role";

    @Override
    public boolean check(String action, TransportRequest request, MetaData metaData) {
        return true;
    }
}
