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
package org.elasticsearch.xpack.ml.datafeed.extractor;

import org.elasticsearch.client.Client;
import org.elasticsearch.xpack.ml.datafeed.ChunkingConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.extractor.aggregation.AggregationDataExtractorFactory;
import org.elasticsearch.xpack.ml.datafeed.extractor.chunked.ChunkedDataExtractorFactory;
import org.elasticsearch.xpack.ml.datafeed.extractor.scroll.ScrollDataExtractorFactory;
import org.elasticsearch.xpack.ml.job.config.Job;

public interface DataExtractorFactory {
    DataExtractor newExtractor(long start, long end);

    /**
     * Creates a {@code DataExtractorFactory} for the given datafeed-job combination.
     */
    static DataExtractorFactory create(Client client, DatafeedConfig datafeedConfig, Job job) {
        boolean isScrollSearch = datafeedConfig.hasAggregations() == false;
        DataExtractorFactory dataExtractorFactory = isScrollSearch ? new ScrollDataExtractorFactory(client, datafeedConfig, job)
                : new AggregationDataExtractorFactory(client, datafeedConfig, job);
        ChunkingConfig chunkingConfig = datafeedConfig.getChunkingConfig();
        if (chunkingConfig == null) {
            chunkingConfig = isScrollSearch ? ChunkingConfig.newAuto() : ChunkingConfig.newOff();
        }

        return chunkingConfig.isEnabled() ? new ChunkedDataExtractorFactory(client, datafeedConfig, job, dataExtractorFactory)
                : dataExtractorFactory;
    }
}
