/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.persistence;

import java.util.Objects;

/**
 * The job identification needed from the Elasticsearch classes.
 * It contains the jobId and the index name.
 */
class ElasticsearchJobId
{
    /**
     * If this is changed, ProcessCtrl.ES_INDEX_PREFIX should also be changed
     */
    public static final String INDEX_PREFIX = "prelertresults-";

    private final String jobId;
    private final String indexName;

    public ElasticsearchJobId(String jobId)
    {
        this.jobId = Objects.requireNonNull(jobId);
        indexName = INDEX_PREFIX + jobId;
    }

    String getId()
    {
        return jobId;
    }

    String getIndex()
    {
        return indexName;
    }

    @Override
    public String toString()
    {
        return jobId;
    }
}
