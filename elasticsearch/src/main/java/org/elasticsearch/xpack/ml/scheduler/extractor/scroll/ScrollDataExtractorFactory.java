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
package org.elasticsearch.xpack.ml.scheduler.extractor.scroll;

import org.elasticsearch.client.Client;
import org.elasticsearch.xpack.ml.job.Job;
import org.elasticsearch.xpack.ml.scheduler.SchedulerConfig;
import org.elasticsearch.xpack.ml.scheduler.extractor.DataExtractor;
import org.elasticsearch.xpack.ml.scheduler.extractor.DataExtractorFactory;

import java.util.Objects;

public class ScrollDataExtractorFactory implements DataExtractorFactory {

    private final Client client;
    private final SchedulerConfig schedulerConfig;
    private final Job job;
    private final ExtractedFields extractedFields;

    public ScrollDataExtractorFactory(Client client, SchedulerConfig schedulerConfig, Job job) {
        this.client = Objects.requireNonNull(client);
        this.schedulerConfig = Objects.requireNonNull(schedulerConfig);
        this.job = Objects.requireNonNull(job);
        this.extractedFields = ExtractedFields.build(job, schedulerConfig);
    }

    @Override
    public DataExtractor newExtractor(long start, long end) {
        ScrollDataExtractorContext dataExtractorContext = new ScrollDataExtractorContext(
                job.getId(),
                extractedFields,
                schedulerConfig.getIndexes(),
                schedulerConfig.getTypes(),
                schedulerConfig.getQuery(),
                schedulerConfig.getScriptFields(),
                schedulerConfig.getScrollSize(),
                start,
                end);
        return new ScrollDataExtractor(client, dataExtractorContext);
    }
}
