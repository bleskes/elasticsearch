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

import org.elasticsearch.transport.TransportRequest;

/**
 *
 */
public interface ClientTransportFilter {

    static final ClientTransportFilter NOOP = new ClientTransportFilter() {
        @Override
        public void outbound(String action, TransportRequest request) {}
    };

    /**
     * Called just before the given request is sent by the transport. Any exception
     * thrown by this method will stop the request from being sent and the error will
     * be sent back to the sender.
     */
    void outbound(String action, TransportRequest request);

}
