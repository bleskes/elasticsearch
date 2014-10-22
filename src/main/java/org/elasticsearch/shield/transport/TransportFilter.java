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
import org.elasticsearch.transport.TransportResponse;

/**
 *
 */
public interface TransportFilter {

    static final TransportFilter NOOP = new Base();

    /**
     * Called just before the given request is about to be sent. Any exception thrown
     * by this method will stop the request from being sent.
     */
    void outboundRequest(String action, TransportRequest request);

    /**
     * Called just after the given request was received by the transport. Any exception
     * thrown by this method will stop the request from being handled and the error will
     * be sent back to the sender.
     */
    void inboundRequest(String action, TransportRequest request);

    /**
     * Called just before the given response is about to be sent. Any exception thrown
     * by this method will stop the response from being sent and an error will be sent
     * instead.
     */
    void outboundResponse(String action, TransportResponse response);

    /**
     * Called just after the given response was received by the transport. Any exception
     * thrown by this method will stop the response from being handled normally and instead
     * the error will be used as the response.
     */
    void inboundResponse(TransportResponse response);

    static class Base implements TransportFilter {

        @Override
        public void outboundRequest(String action, TransportRequest request) {
        }

        @Override
        public void inboundRequest(String action, TransportRequest request) {
        }

        @Override
        public void outboundResponse(String action, TransportResponse response) {
        }

        @Override
        public void inboundResponse(TransportResponse response) {
        }
    }

}
