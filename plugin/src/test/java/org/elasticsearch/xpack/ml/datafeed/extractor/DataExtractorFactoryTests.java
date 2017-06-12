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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.fieldcaps.FieldCapabilities;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesAction;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesResponse;
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
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataExtractorFactoryTests extends ESTestCase {

    private FieldCapabilitiesResponse fieldsCapabilities;

    private Client client;

    @Before
    public void setUpTests() {
        client = mock(Client.class);
        fieldsCapabilities = mock(FieldCapabilitiesResponse.class);
        givenAggregatableField("time", "date");
        givenAggregatableField("field", "keyword");

        doAnswer(invocationMock -> {
            @SuppressWarnings("raw_types")
            ActionListener listener = (ActionListener) invocationMock.getArguments()[2];
            listener.onResponse(fieldsCapabilities);
            return null;
        }).when(client).execute(same(FieldCapabilitiesAction.INSTANCE), any(), any());
    }

    public void testCreateDataExtractorFactoryGivenDefaultScroll() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo").build();

        ActionListener<DataExtractorFactory> listener = ActionListener.wrap(
                dataExtractorFactory -> assertThat(dataExtractorFactory, instanceOf(ChunkedDataExtractorFactory.class)),
                e -> fail()
        );

        DataExtractorFactory.create(client, datafeedConfig, jobBuilder.build(new Date()), listener);
    }

    public void testCreateDataExtractorFactoryGivenScrollWithAutoChunk() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig.Builder datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo");
        datafeedConfig.setChunkingConfig(ChunkingConfig.newAuto());

        ActionListener<DataExtractorFactory> listener = ActionListener.wrap(
                dataExtractorFactory -> assertThat(dataExtractorFactory, instanceOf(ChunkedDataExtractorFactory.class)),
                e -> fail()
        );

        DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()), listener);
    }

    public void testCreateDataExtractorFactoryGivenScrollWithOffChunk() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig.Builder datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo");
        datafeedConfig.setChunkingConfig(ChunkingConfig.newOff());

        ActionListener<DataExtractorFactory> listener = ActionListener.wrap(
                dataExtractorFactory -> assertThat(dataExtractorFactory, instanceOf(ScrollDataExtractorFactory.class)),
                e -> fail()
        );

        DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()), listener);
    }

    public void testCreateDataExtractorFactoryGivenDefaultAggregation() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig.Builder datafeedConfig = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo");
        datafeedConfig.setAggregations(AggregatorFactories.builder().addAggregator(
                AggregationBuilders.histogram("time").interval(300000)));

        ActionListener<DataExtractorFactory> listener = ActionListener.wrap(
                dataExtractorFactory -> assertThat(dataExtractorFactory, instanceOf(ChunkedDataExtractorFactory.class)),
                e -> fail()
        );

        DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()), listener);
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

        ActionListener<DataExtractorFactory> listener = ActionListener.wrap(
                dataExtractorFactory -> assertThat(dataExtractorFactory, instanceOf(AggregationDataExtractorFactory.class)),
                e -> fail()
        );

        DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()), listener);
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

        ActionListener<DataExtractorFactory> listener = ActionListener.wrap(
                dataExtractorFactory -> assertThat(dataExtractorFactory, instanceOf(ChunkedDataExtractorFactory.class)),
                e -> fail()
        );

        DataExtractorFactory.create(client, datafeedConfig.build(), jobBuilder.build(new Date()), listener);
    }

    private void givenAggregatableField(String field, String type) {
        FieldCapabilities fieldCaps = mock(FieldCapabilities.class);
        when(fieldCaps.isSearchable()).thenReturn(true);
        when(fieldCaps.isAggregatable()).thenReturn(true);
        Map<String, FieldCapabilities> fieldCapsMap = new HashMap<>();
        fieldCapsMap.put(type, fieldCaps);
        when(fieldsCapabilities.getField(field)).thenReturn(fieldCapsMap);
    }
}