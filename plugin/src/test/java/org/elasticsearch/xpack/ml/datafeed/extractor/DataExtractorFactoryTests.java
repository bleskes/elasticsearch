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
package org.elasticsearch.xpack.ml.datafeed.extractor;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.datafeed.ChunkingConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedManagerTests;
import org.elasticsearch.xpack.ml.datafeed.extractor.aggregation.AggregationDataExtractorFactory;
import org.elasticsearch.xpack.ml.datafeed.extractor.chunked.ChunkedDataExtractorFactory;
import org.elasticsearch.xpack.ml.datafeed.extractor.scroll.ScrollDataExtractorFactory;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.junit.Before;

import java.util.Date;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;

public class DataExtractorFactoryTests extends ESTestCase {

    private Client client;

    @Before
    public void setUpTests() {
        client = mock(Client.class);
    }

    public void testCreateDataExtractorFactoryGivenDefaultScroll() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo").build();

        DataExtractorFactory dataExtractorFactory =
                DataExtractorFactory.create(client, datafeedConfig, jobBuilder.build(new Date()));

        assertThat(dataExtractorFactory, instanceOf(ChunkedDataExtractorFactory.class));
    }

    public void testCreateDataExtractorFactoryGivenScrollWithAutoChunk() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig.Builder datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo");
        datafeedConfig.setChunkingConfig(ChunkingConfig.newAuto());

        DataExtractorFactory dataExtractorFactory =
                DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()));

        assertThat(dataExtractorFactory, instanceOf(ChunkedDataExtractorFactory.class));
    }

    public void testCreateDataExtractorFactoryGivenScrollWithOffChunk() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig.Builder datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo");
        datafeedConfig.setChunkingConfig(ChunkingConfig.newOff());

        DataExtractorFactory dataExtractorFactory =
                DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()));

        assertThat(dataExtractorFactory, instanceOf(ScrollDataExtractorFactory.class));
    }

    public void testCreateDataExtractorFactoryGivenDefaultAggregation() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig.Builder datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo");
        datafeedConfig.setAggregations(AggregatorFactories.builder().addAggregator(
                AggregationBuilders.histogram("time").interval(300000)));

        DataExtractorFactory dataExtractorFactory =
                DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()));

        assertThat(dataExtractorFactory, instanceOf(ChunkedDataExtractorFactory.class));
    }

    public void testCreateDataExtractorFactoryGivenAggregationWithOffChunk() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig.Builder datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo");
        datafeedConfig.setChunkingConfig(ChunkingConfig.newOff());
        datafeedConfig.setAggregations(AggregatorFactories.builder().addAggregator(
                AggregationBuilders.histogram("time").interval(300000)));

        DataExtractorFactory dataExtractorFactory =
                DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()));

        assertThat(dataExtractorFactory, instanceOf(AggregationDataExtractorFactory.class));
    }

    public void testCreateDataExtractorFactoryGivenDefaultAggregationWithAutoChunk() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig.Builder datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo");
        datafeedConfig.setAggregations(AggregatorFactories.builder().addAggregator(
                AggregationBuilders.histogram("time").interval(300000)));
        datafeedConfig.setChunkingConfig(ChunkingConfig.newAuto());

        DataExtractorFactory dataExtractorFactory =
                DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()));

        assertThat(dataExtractorFactory, instanceOf(ChunkedDataExtractorFactory.class));
    }
}