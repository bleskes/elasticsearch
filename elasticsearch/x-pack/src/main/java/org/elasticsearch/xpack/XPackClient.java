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

package org.elasticsearch.xpack;

import org.elasticsearch.client.Client;
import org.elasticsearch.marvel.client.MonitoringClient;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.client.SecurityClient;
import org.elasticsearch.watcher.client.WatcherClient;

import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;

/**
 *
 */
public class XPackClient {

    private final Client client;

    private final MonitoringClient monitoringClient;
    private final SecurityClient securityClient;
    private final WatcherClient watcherClient;

    public XPackClient(Client client) {
        this.client = client;
        this.monitoringClient = new MonitoringClient(client);
        this.securityClient = new SecurityClient(client);
        this.watcherClient = new WatcherClient(client);
    }

    public MonitoringClient monitoring() {
        return monitoringClient;
    }

    public SecurityClient security() {
        return securityClient;
    }

    public WatcherClient watcher() {
        return watcherClient;
    }

    public XPackClient withHeaders(Map<String, String> headers) {
        return new XPackClient(client.filterWithHeader(headers));
    }

    /**
     * Returns a client that will call xpack APIs on behalf of the given user.
     *
     * @param username The username of the user
     * @param passwd    The password of the user. This char array can be cleared after calling this method.
     */
    public XPackClient withAuth(String username, char[] passwd) {
       return withHeaders(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue(username, new SecuredString(passwd))));
    }
}
