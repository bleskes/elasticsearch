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

package org.elasticsearch.marvel.support.init.proxy;

import org.elasticsearch.client.Client;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.common.init.proxy.ClientProxy;

public class MonitoringClientProxy extends ClientProxy {

    /**
     * Creates a proxy to the given internal client (can be used for testing)
     */
    public static MonitoringClientProxy of(Client client) {
        MonitoringClientProxy proxy = new MonitoringClientProxy();
        proxy.client = client instanceof InternalClient ? (InternalClient) client : new InternalClient.Insecure(client);
        return proxy;
    }
}
