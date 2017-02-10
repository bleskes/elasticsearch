/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.datafeed.extractor.chunked;

import org.elasticsearch.client.Client;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.extractor.DataExtractor;
import org.elasticsearch.xpack.ml.datafeed.extractor.DataExtractorFactory;
import org.elasticsearch.xpack.ml.job.config.Job;

import java.util.Objects;

public class ChunkedDataExtractorFactory implements DataExtractorFactory {

    private final Client client;
    private final DatafeedConfig datafeedConfig;
    private final Job job;
    private final DataExtractorFactory dataExtractorFactory;

    public ChunkedDataExtractorFactory(Client client, DatafeedConfig datafeedConfig, Job job, DataExtractorFactory dataExtractorFactory) {
        this.client = Objects.requireNonNull(client);
        this.datafeedConfig = Objects.requireNonNull(datafeedConfig);
        this.job = Objects.requireNonNull(job);
        this.dataExtractorFactory = Objects.requireNonNull(dataExtractorFactory);
    }

    @Override
    public DataExtractor newExtractor(long start, long end) {
        ChunkedDataExtractorContext dataExtractorContext = new ChunkedDataExtractorContext(
                job.getId(),
                job.getDataDescription().getTimeField(),
                datafeedConfig.getIndexes(),
                datafeedConfig.getTypes(),
                datafeedConfig.getQuery(),
                datafeedConfig.getScrollSize(),
                start,
                end,
                datafeedConfig.getChunkingConfig() == null ? null : datafeedConfig.getChunkingConfig().getTimeSpan());
        return new ChunkedDataExtractor(client, dataExtractorFactory, dataExtractorContext);
    }
}
