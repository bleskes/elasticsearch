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

package org.elasticsearch.shield.transport;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.*;

/**
 *
 */
public class SecuredTransportService extends TransportService {

    private final ServerTransportFilter filter;

    @Inject
    public SecuredTransportService(Settings settings, Transport transport, ThreadPool threadPool, ServerTransportFilter filter) {
        super(settings, transport, threadPool);
        this.filter = filter;
    }

    @Override
    public void registerHandler(String action, TransportRequestHandler handler) {
        super.registerHandler(action, new SecuredRequestHandler(action, handler, filter));
    }

    static class SecuredRequestHandler implements TransportRequestHandler {

        private final String action;
        private final TransportRequestHandler handler;
        private final ServerTransportFilter filter;

        SecuredRequestHandler(String action, TransportRequestHandler handler, ServerTransportFilter filter) {
            this.action = action;
            this.handler = handler;
            this.filter = filter;
        }

        @Override
        public TransportRequest newInstance() {
            return handler.newInstance();
        }

        @Override @SuppressWarnings("unchecked")
        public void messageReceived(TransportRequest request, TransportChannel channel) throws Exception {
            try {
                filter.inbound(action, request);
            } catch (Throwable t) {
                channel.sendResponse(t);
                return;
            }
            handler.messageReceived(request, channel);
        }

        @Override
        public String executor() {
            return handler.executor();
        }

        @Override
        public boolean isForceExecution() {
            return handler.isForceExecution();
        }
    }
}
