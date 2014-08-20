package org.elasticsearch.shield.authz.indicesresolver;

import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.transport.TransportRequest;

import java.util.Set;

/**
 *
 */
public interface IndicesResolver<Request extends TransportRequest> {

    Class<Request> requestType();

    Set<String> resolve(Request request, MetaData metaData);

}
