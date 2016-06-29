/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.data.extractors.elasticsearch;

import static org.junit.Assert.assertEquals;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class FieldStatsCachedIndexSelectorTest
{
    private static final ElasticsearchUrlBuilder URL_BUILDER = ElasticsearchUrlBuilder
            .create("http://myes:9200", Collections.emptyList());

    @Mock private HttpRequester m_HttpRequester;
    @Mock private Logger m_Logger;
    private ArgumentCaptor<String> m_UrlCaptor = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<String> m_RequestBodyCaptor  = ArgumentCaptor.forClass(String.class);

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown()
    {
        Mockito.verifyNoMoreInteractions(m_Logger);
    }

    @Test
    public void testSelectByTime_GivenEndTimeEarlierThanStartTime()
    {
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("*"));

        assertEquals(Collections.emptyList(), indexSelector.selectByTime(100L, 50L, m_Logger));

        verify(m_Logger).error("selectByTime expects the end time to be strictly greater than the "
                + "start time; actual call was: startMs = 100, endMs = 50");
    }

    @Test
    public void testSelectByTime_GivenEndTimeSameAsStartTime()
    {
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("*"));

        assertEquals(Collections.emptyList(), indexSelector.selectByTime(100L, 100L, m_Logger));

        verify(m_Logger).error("selectByTime expects the end time to be strictly greater than the "
                + "start time; actual call was: startMs = 100, endMs = 100");
    }

    @Test
    public void testSelectByTime_GivenHttpResponseHasStatus400() throws IOException
    {
        HttpResponse httpResponse = new HttpResponse(toStream("Oups..."), 400);
        when(m_HttpRequester.get(anyString(), anyString())).thenReturn(httpResponse);

        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("foo", "bar-*"));
        assertEquals(Arrays.asList("foo", "bar-*"), indexSelector.selectByTime(0L, 50L, m_Logger));

        // Verify second call does not log twice
        assertEquals(Arrays.asList("foo", "bar-*"), indexSelector.selectByTime(0L, 50L, m_Logger));
        verify(m_Logger).warn("Failed to select indices using the field stats API; "
                + "falling back to using configured indices. Reason was: "
                + "Request to 'http://myes:9200/foo,bar-*/_field_stats?level=indices' "
                + "failed with status code 400. Response was:\nOups...");
    }

    @Test
    public void testSelectByTime_GivenRangesWithoutOverlap() throws IOException
    {
        String json = "{"
                + "\"_shards\":{},"
                + "\"indices\":{"
                +   "\"index_1\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 1,"
                +         "\"max_value\": 2"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_2\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 3,"
                +         "\"max_value\": 4"
                +       "}"
                +     "}"
                +   "}"
                + "}"
                +"}";

        String expectedRequestBody = "{"
                + "\"fields\": [\"@timestamp\"],"
                + "\"index_constraints\": {"
                +   "\"@timestamp\": {"
                +     "\"max_value\": {"
                +       "\"gte\": \"0\","
                +       "\"format\": \"epoch_millis\""
                +     "},"
                +     "\"min_value\": {"
                +       "\"lt\": \"5\","
                +       "\"format\": \"epoch_millis\""
                +     "}"
                +   "}"
                + "}"
                + "}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenReturn(createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("index_*"));

        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(0L, 5L, m_Logger));

        verify(m_HttpRequester, times(1)).get(m_UrlCaptor.capture(), m_RequestBodyCaptor.capture());
        assertEquals("http://myes:9200/index_*/_field_stats?level=indices", m_UrlCaptor.getValue());
        assertEquals(expectedRequestBody, m_RequestBodyCaptor.getValue());

        assertEquals(Collections.emptyList(), indexSelector.selectByTime(0L, 1L, m_Logger));
        assertEquals(Arrays.asList("index_1"), indexSelector.selectByTime(0L, 2L, m_Logger));
        assertEquals(Arrays.asList("index_1"), indexSelector.selectByTime(0L, 3L, m_Logger));
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(0L, 4L, m_Logger));
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(0L, 5L, m_Logger));
        assertEquals(Arrays.asList("index_1"), indexSelector.selectByTime(1L, 2L, m_Logger));
        assertEquals(Arrays.asList("index_1"), indexSelector.selectByTime(1L, 3L, m_Logger));
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(1L, 4L, m_Logger));
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(1L, 5L, m_Logger));
        assertEquals(Arrays.asList("index_1"), indexSelector.selectByTime(2L, 3L, m_Logger));
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(2L, 4L, m_Logger));
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(2L, 5L, m_Logger));
        assertEquals(Arrays.asList("index_2"), indexSelector.selectByTime(3L, 4L, m_Logger));
        assertEquals(Arrays.asList("index_2"), indexSelector.selectByTime(3L, 5L, m_Logger));
        assertEquals(Arrays.asList("index_2"), indexSelector.selectByTime(4L, 5L, m_Logger));
    }

    @Test
    public void testSelectByTime_GivenRangesWithOverlap() throws IOException
    {
        String json = "{"
                + "\"_shards\":{},"
                + "\"indices\":{"
                +   "\"index_1\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 0,"
                +         "\"max_value\": 4"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_2\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 1,"
                +         "\"max_value\": 1"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_3\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 1,"
                +         "\"max_value\": 5"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_4\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 1,"
                +         "\"max_value\": 3"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_5\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 2,"
                +         "\"max_value\": 4"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_6\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 8,"
                +         "\"max_value\": 15"
                +       "}"
                +     "}"
                +   "}"
                + "}"
                +"}";

        String expectedRequestBody = "{"
                + "\"fields\": [\"@timestamp\"],"
                + "\"index_constraints\": {"
                +   "\"@timestamp\": {"
                +     "\"max_value\": {"
                +       "\"gte\": \"2\","
                +       "\"format\": \"epoch_millis\""
                +     "},"
                +     "\"min_value\": {"
                +       "\"lt\": \"5\","
                +       "\"format\": \"epoch_millis\""
                +     "}"
                +   "}"
                + "}"
                + "}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenReturn(createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("index_*"));

        assertEqualsIgnoreOrder(Arrays.asList("index_1", "index_3", "index_4", "index_5"),
                indexSelector.selectByTime(2L, 5L, m_Logger));

        verify(m_HttpRequester, times(1)).get(m_UrlCaptor.capture(), m_RequestBodyCaptor.capture());
        assertEquals("http://myes:9200/index_*/_field_stats?level=indices", m_UrlCaptor.getValue());
        assertEquals(expectedRequestBody, m_RequestBodyCaptor.getValue());
    }

    @Test
    public void testSelectByTime_GivenRangesWithSameMinMaxValues() throws IOException
    {
        String json = "{"
                + "\"_shards\":{},"
                + "\"indices\":{"
                +   "\"index_1\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 3,"
                +         "\"max_value\": 3"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_2\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 3,"
                +         "\"max_value\": 3"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_3\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 3,"
                +         "\"max_value\": 3"
                +       "}"
                +     "}"
                +   "}"
                + "}"
                +"}";

        String expectedRequestBody = "{"
                + "\"fields\": [\"@timestamp\"],"
                + "\"index_constraints\": {"
                +   "\"@timestamp\": {"
                +     "\"max_value\": {"
                +       "\"gte\": \"3\","
                +       "\"format\": \"epoch_millis\""
                +     "},"
                +     "\"min_value\": {"
                +       "\"lt\": \"4\","
                +       "\"format\": \"epoch_millis\""
                +     "}"
                +   "}"
                + "}"
                + "}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenReturn(createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("index_*"));

        assertEqualsIgnoreOrder(Arrays.asList("index_1", "index_2", "index_3"),
                indexSelector.selectByTime(3L, 4L, m_Logger));

        verify(m_HttpRequester, times(1)).get(m_UrlCaptor.capture(), m_RequestBodyCaptor.capture());
        assertEquals("http://myes:9200/index_*/_field_stats?level=indices", m_UrlCaptor.getValue());
        assertEquals(expectedRequestBody, m_RequestBodyCaptor.getValue());
    }

    @Test
    public void testSelectByTime_GivenRangeIsNotCached() throws IOException
    {
        String json = "{"
                + "\"_shards\":{},"
                + "\"indices\":{"
                +   "\"index_1\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 1,"
                +         "\"max_value\": 2"
                +       "}"
                +     "}"
                +   "}"
                + "}"
                +"}";

        String expectedRequestBody1 = "{"
                + "\"fields\": [\"@timestamp\"],"
                + "\"index_constraints\": {"
                +   "\"@timestamp\": {"
                +     "\"max_value\": {"
                +       "\"gte\": \"1\","
                +       "\"format\": \"epoch_millis\""
                +     "},"
                +     "\"min_value\": {"
                +       "\"lt\": \"2\","
                +       "\"format\": \"epoch_millis\""
                +     "}"
                +   "}"
                + "}"
                + "}";

        String expectedRequestBody2 = "{"
                + "\"fields\": [\"@timestamp\"],"
                + "\"index_constraints\": {"
                +   "\"@timestamp\": {"
                +     "\"max_value\": {"
                +       "\"gte\": \"2\","
                +       "\"format\": \"epoch_millis\""
                +     "},"
                +     "\"min_value\": {"
                +       "\"lt\": \"3\","
                +       "\"format\": \"epoch_millis\""
                +     "}"
                +   "}"
                + "}"
                + "}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenReturn(createOkHttpResponse(json), createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("index_*"));

        assertEquals(Arrays.asList("index_1"), indexSelector.selectByTime(1L, 2L, m_Logger));
        assertEquals(Arrays.asList("index_1"), indexSelector.selectByTime(2L, 3L, m_Logger));

        verify(m_HttpRequester, times(2)).get(m_UrlCaptor.capture(), m_RequestBodyCaptor.capture());
        assertEquals("http://myes:9200/index_*/_field_stats?level=indices", m_UrlCaptor.getValue());
        List<String> bodies = m_RequestBodyCaptor.getAllValues();
        assertEquals(expectedRequestBody1, bodies.get(0));
        assertEquals(expectedRequestBody2, bodies.get(1));
    }

    @Test
    public void testSelectByTime_GivenNoIndexMatchesRange() throws IOException
    {
        String json = "{"
                + "\"_shards\":{},"
                + "\"indices\":{}"
                +"}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenReturn(createOkHttpResponse(json), createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("index_*"));

        assertEquals(Collections.emptyList(), indexSelector.selectByTime(1000L, 2000L, m_Logger));

        verify(m_HttpRequester, times(1)).get(m_UrlCaptor.capture(), m_RequestBodyCaptor.capture());
        assertEquals("http://myes:9200/index_*/_field_stats?level=indices", m_UrlCaptor.getValue());
    }

    @Test
    public void testSelectByTime_GivenResponseIsEmptyJson() throws IOException
    {
        String json = "{}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenReturn(createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("index-*"));

        assertEquals(Arrays.asList("index-*"), indexSelector.selectByTime(0L, 5L, m_Logger));

        // Verify second call does not log twice
        assertEquals(Arrays.asList("index-*"), indexSelector.selectByTime(0L, 5L, m_Logger));
        verify(m_Logger).warn("Failed to select indices using the field stats API; "
                + "falling back to using configured indices. Reason was: "
                + "Expected field 'indices' was missing from field stats response");
    }

    @Test
    public void testSelectByTime_GivenResponseDoesNotContainMinValueAndMaxValue() throws IOException
    {
        String json = "{"
                + "\"_shards\":{},"
                + "\"indices\":{"
                +   "\"myIndex-1\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 1,"
                +         "\"max_value\": 2"
                +       "}"
                +     "}"
                +   "},"
                +   "\"myIndex-2\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +       "}"
                +     "}"
                +   "}"
                + "}"
                +"}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenReturn(createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("myIndex-*"));

        assertEquals(Arrays.asList("myIndex-*"), indexSelector.selectByTime(0L, 5L, m_Logger));
        assertEquals(Arrays.asList("myIndex-*"), indexSelector.selectByTime(0L, 5L, m_Logger));

        verify(m_HttpRequester, times(1)).get(m_UrlCaptor.capture(), m_RequestBodyCaptor.capture());
        assertEquals("http://myes:9200/myIndex-*/_field_stats?level=indices", m_UrlCaptor.getValue());

        verify(m_Logger).warn("Failed to select indices using the field stats API; "
                + "falling back to using configured indices. Reason was: "
                + "Expected field 'min_value' was missing from field stats response");
    }

    @Test
    public void testSelectByTime_GivenMinValueIsString() throws IOException
    {
        String json = "{"
                + "\"_shards\":{},"
                + "\"indices\":{"
                +   "\"myIndex-1\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": \"2014-01-01T00:00:00Z\","
                +         "\"max_value\": 2"
                +       "}"
                +     "}"
                +   "}"
                + "}"
                +"}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenReturn(createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("myIndex-*"));

        assertEquals(Arrays.asList("myIndex-*"), indexSelector.selectByTime(0L, 5L, m_Logger));
        assertEquals(Arrays.asList("myIndex-*"), indexSelector.selectByTime(0L, 5L, m_Logger));

        verify(m_HttpRequester, times(1)).get(m_UrlCaptor.capture(), m_RequestBodyCaptor.capture());
        assertEquals("http://myes:9200/myIndex-*/_field_stats?level=indices", m_UrlCaptor.getValue());

        verify(m_Logger).warn("Failed to select indices using the field stats API; "
                + "falling back to using configured indices. Reason was: "
                + "Field 'min_value' was expected to be a long; actual type was: STRING");
    }

    @Test
    public void testClearCache() throws IOException
    {
        String json = "{"
                + "\"_shards\":{},"
                + "\"indices\":{"
                +   "\"index_1\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 1,"
                +         "\"max_value\": 2"
                +       "}"
                +     "}"
                +   "},"
                +   "\"index_2\":{"
                +     "\"fields\": {"
                +       "\"@timestamp\":{"
                +         "\"min_value\": 3,"
                +         "\"max_value\": 4"
                +       "}"
                +     "}"
                +   "}"
                + "}"
                +"}";

        String expectedRequestBody = "{"
                + "\"fields\": [\"@timestamp\"],"
                + "\"index_constraints\": {"
                +   "\"@timestamp\": {"
                +     "\"max_value\": {"
                +       "\"gte\": \"0\","
                +       "\"format\": \"epoch_millis\""
                +     "},"
                +     "\"min_value\": {"
                +       "\"lt\": \"5\","
                +       "\"format\": \"epoch_millis\""
                +     "}"
                +   "}"
                + "}"
                + "}";

        when(m_HttpRequester.get(anyString(), anyString()))
                .thenAnswer(inv -> createOkHttpResponse(json));
        IndexSelector indexSelector = new FieldStatsCachedIndexSelector(m_HttpRequester,
                URL_BUILDER, "@timestamp", Arrays.asList("*"));

        // This will be cached
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(0L, 5L, m_Logger));

        // This should be retrieved from the cache without causing a new HTTP request
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(0L, 5L, m_Logger));

        indexSelector.clearCache();

        // Now cache should be empty thus causing a new HTTP request
        assertEquals(Arrays.asList("index_1", "index_2"), indexSelector.selectByTime(0L, 5L, m_Logger));

        verify(m_HttpRequester, times(2)).get(m_UrlCaptor.capture(), m_RequestBodyCaptor.capture());
        List<String> urls = m_UrlCaptor.getAllValues();
        assertEquals("http://myes:9200/*/_field_stats?level=indices", urls.get(0));
        assertEquals("http://myes:9200/*/_field_stats?level=indices", urls.get(1));
        List<String> requestBodies = m_RequestBodyCaptor.getAllValues();
        assertEquals(expectedRequestBody, requestBodies.get(0));
        assertEquals(expectedRequestBody, requestBodies.get(1));
    }

    private static HttpResponse createOkHttpResponse(String response)
    {
        return new HttpResponse(toStream(response), 200);
    }

    private static InputStream toStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertEqualsIgnoreOrder(List<String> expected, List<String> actual)
    {
        Set<String> expectedAsSet = new HashSet<>();
        expectedAsSet.addAll(expected);
        Set<String> actualAsSet = new HashSet<>();
        actualAsSet.addAll(actual);
        assertEquals(expectedAsSet, actualAsSet);
    }
}
