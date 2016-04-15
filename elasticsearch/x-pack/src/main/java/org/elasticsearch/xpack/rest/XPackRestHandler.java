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

package org.elasticsearch.xpack.rest;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.xpack.XPackClient;

/**
 *
 */
public abstract class XPackRestHandler extends BaseRestHandler {

    protected static String URI_BASE = "_xpack";

    public XPackRestHandler(Settings settings, Client client) {
        super(settings, client);
    }

    @Override
    protected final void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        handleRequest(request, channel, new XPackClient(client));
    }

    protected abstract void handleRequest(RestRequest request, RestChannel channel, XPackClient client) throws Exception;
}
