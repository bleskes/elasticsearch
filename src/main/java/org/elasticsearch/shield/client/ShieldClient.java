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

package org.elasticsearch.shield.client;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * A wrapper to elasticsearch clients that exposes all Shield related APIs
 */
public class ShieldClient {

    private final ShieldAuthcClient authcClient;

    public ShieldClient(ElasticsearchClient client) {
        this.authcClient = new ShieldAuthcClient(client);
    }

    /**
     * @return  The Shield authentication client.
     */
    public ShieldAuthcClient authc() {
        return authcClient;
    }

}
