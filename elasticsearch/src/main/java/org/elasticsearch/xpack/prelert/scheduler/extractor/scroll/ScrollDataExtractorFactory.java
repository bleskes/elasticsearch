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
package org.elasticsearch.xpack.prelert.scheduler.extractor.scroll;

import org.elasticsearch.client.Client;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfig;
import org.elasticsearch.xpack.prelert.scheduler.extractor.DataExtractor;
import org.elasticsearch.xpack.prelert.scheduler.extractor.DataExtractorFactory;

import java.util.Objects;

public class ScrollDataExtractorFactory implements DataExtractorFactory {

    private final Client client;
    private final SchedulerConfig schedulerConfig;
    private final Job job;

    public ScrollDataExtractorFactory(Client client, SchedulerConfig schedulerConfig, Job job) {
        this.client = Objects.requireNonNull(client);
        this.schedulerConfig = Objects.requireNonNull(schedulerConfig);
        this.job = Objects.requireNonNull(job);
    }

    @Override
    public DataExtractor newExtractor(long start, long end) {
        String timeField = job.getDataDescription().getTimeField();
        ScrollDataExtractorContext dataExtractorContext = new ScrollDataExtractorContext(
                job.getId(),
                job.allFields(),
                timeField,
                schedulerConfig.getIndexes(),
                schedulerConfig.getTypes(),
                schedulerConfig.getQuery(),
                schedulerConfig.getAggregations(),
                schedulerConfig.getScriptFields(),
                schedulerConfig.getScrollSize(),
                start,
                end);
        return new ScrollDataExtractor(client, dataExtractorContext);
    }
}
