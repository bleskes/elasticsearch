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

package org.elasticsearch.xpack.security.transport;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.transport.TransportException;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.transport.TransportRequestOptions;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.transport.TransportResponseHandler;
import org.elasticsearch.transport.TransportService;

public class SecurityClientTransportService extends TransportService {

    private final ClientTransportFilter clientFilter;

    @Inject
    public SecurityClientTransportService(Settings settings, Transport transport, ThreadPool threadPool,
                                          ClientTransportFilter clientFilter) {
        super(settings, transport, threadPool);
        this.clientFilter = clientFilter;
    }

    @Override
    public <T extends TransportResponse> void sendRequest(DiscoveryNode node, String action, TransportRequest request,
                                                          TransportRequestOptions options, TransportResponseHandler<T> handler) {
        try {
            clientFilter.outbound(action, request);
            super.sendRequest(node, action, request, options, handler);
        } catch (Exception e) {
            handler.handleException(new TransportException("failed sending request", e));
        }
    }

}
