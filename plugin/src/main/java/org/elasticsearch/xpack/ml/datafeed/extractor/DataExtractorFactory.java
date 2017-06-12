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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
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
    static void create(Client client, DatafeedConfig datafeed, Job job, ActionListener<DataExtractorFactory> listener) {
        ActionListener<DataExtractorFactory> factoryHandler = ActionListener.wrap(
                factory -> listener.onResponse(datafeed.getChunkingConfig().isEnabled()
                        ? new ChunkedDataExtractorFactory(client, datafeed, job, factory) : factory)
                , listener::onFailure
        );

        boolean isScrollSearch = datafeed.hasAggregations() == false;
        if (isScrollSearch) {
            ScrollDataExtractorFactory.create(client, datafeed, job, factoryHandler);
        } else {
            factoryHandler.onResponse(new AggregationDataExtractorFactory(client, datafeed, job));
        }
    }
}
