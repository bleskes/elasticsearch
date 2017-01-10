/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job.persistence;

import org.elasticsearch.client.Client;

import java.util.function.Function;

/**
 * TODO This is all just silly static typing shenanigans because Guice can't inject
 * anonymous lambdas.  This can all be removed once Guice goes away.
 */
public class JobDataDeleterFactory implements Function<String, JobDataDeleter> {

    private final Client client;

    public JobDataDeleterFactory(Client client) {
        this.client = client;
    }

    @Override
    public JobDataDeleter apply(String jobId) {
        return new JobDataDeleter(client, jobId);
    }
}
