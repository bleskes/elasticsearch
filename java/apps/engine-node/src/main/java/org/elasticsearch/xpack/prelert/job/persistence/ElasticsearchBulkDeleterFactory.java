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
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.client.Client;

import java.util.function.Function;

/**
 * TODO This is all just silly static typing shenanigans because Guice can't inject
 * anonymous lambdas.  This can all be removed once Guice goes away.
 */
public class ElasticsearchBulkDeleterFactory implements Function<String, ElasticsearchBulkDeleter> {

    private final Client client;

    public ElasticsearchBulkDeleterFactory(Client client) {
        this.client = client;
    }

    @Override
    public ElasticsearchBulkDeleter apply(String jobId) {
        return new ElasticsearchBulkDeleter(client, jobId);
    }
}
