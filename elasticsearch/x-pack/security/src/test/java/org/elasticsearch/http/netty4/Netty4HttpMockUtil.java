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

package org.elasticsearch.http.netty4;

import org.elasticsearch.transport.netty4.Netty4OpenChannelsHandler;

import static org.mockito.Mockito.mock;

/** Allows setting a mock into Netty3HttpServerTransport */
public class Netty4HttpMockUtil {

    /**
     * We don't really need to start Netty for these tests, but we can't create a pipeline
     * with a null handler. So we set it to a mock for tests.
     */
    public static void setOpenChannelsHandlerToMock(Netty4HttpServerTransport transport) throws Exception {
        transport.serverOpenChannels = mock(Netty4OpenChannelsHandler.class);
    }

}