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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ElasticsearchDataExtractorTest
{
    private static final String BASE_URL = "http://localhost:9200";
    private static final List<String> INDICES = Arrays.asList("dataIndex");
    private static final List<String> TYPES = Arrays.asList("dataType");
    private static final String SEARCH = "\"match_all\": {}";
    private static final String TIME_FIELD = "time";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private Logger m_Logger;

    private String m_Aggregations;
    private String m_ScriptFields;
    private List<String> m_Fields;

    private ElasticsearchDataExtractor m_Extractor;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDataExtraction() throws IOException
    {
        String initialResponse = "{"
                + "\"_scroll_id\":\"c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        String scrollResponse = "{"
                + "\"_scroll_id\":\"secondScrollId\","
                + "\"took\":8,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403782200\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403782200\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        String scrollEndResponse = "{"
                + "\"_scroll_id\":\"thirdScrollId\","
                + "\"took\":8,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":[]"
                + "}"
                + "}";

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(initialResponse), 200),
                new HttpResponse(toStream(scrollResponse), 200),
                new HttpResponse(toStream(scrollEndResponse), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertEquals(scrollResponse, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());

        requester.assertEqualRequestsToResponses();

        RequestParams firstRequestParams = requester.getGetRequestParams(0);
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=1000", firstRequestParams.url);
        String expectedSearchBody = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\": {}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"time\": {"
                + "                \"gte\": \"1970-01-17T04:53:20.000Z\","
                + "                \"lt\": \"1970-01-17T05:53:20.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expectedSearchBody, firstRequestParams.requestBody);

        RequestParams secondRequestParams = requester.getGetRequestParams(1);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", secondRequestParams.url);
        assertEquals("c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1", secondRequestParams.requestBody);

        RequestParams thirdRequestParams = requester.getGetRequestParams(2);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", thirdRequestParams.url);
        assertEquals("secondScrollId", thirdRequestParams.requestBody);

        assertEquals("http://localhost:9200/_search/scroll", requester.getDeleteRequestParams(0).url);
        assertEquals("{\"scroll_id\":[\"thirdScrollId\"]}",
                requester.getDeleteRequestParams(0).requestBody);
        assertEquals(1, requester.m_DeleteRequestParams.size());
    }

    @Test
    public void testDataExtraction_GivenInitialResponseContainsLongScrollId() throws IOException
    {
        StringBuilder scrollId = new StringBuilder();
        for (int i = 0; i < 300 * 1024; i++)
        {
            scrollId.append("a");
        }

        String initialResponse = "{"
                + "\"_scroll_id\":\""+ scrollId + "\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        String scrollEndResponse = "{"
                + "\"_scroll_id\":\""+ scrollId + "\","
                + "\"took\":8,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":[]"
                + "}"
                + "}";

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(initialResponse), 200),
                new HttpResponse(toStream(scrollEndResponse), 200));
        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
        assertFalse(m_Extractor.hasNext());

        assertEquals(scrollId.toString(), requester.getGetRequestParams(1).requestBody);
    }

    @Test
    public void testDataExtraction_GivenInitialResponseContainsNoHitsAndNoScrollId() throws IOException
    {
        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage("Field '_scroll_id' was expected but not found in first 2 bytes of response:\n{}");

        String initialResponse = "{}";
        HttpResponse httpGetResponse = new HttpResponse(
                toStream(initialResponse), 200);
        List<HttpResponse> responses = Arrays.asList(httpGetResponse);
        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
    }

    @Test
    public void testDataExtraction_GivenInitialResponseContainsHitsButNoScrollId() throws IOException
    {
        String initialResponse = "{"
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "}"
                + "}";
        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage("Field '_scroll_id' was expected but not found in first 272 bytes of response:\n" + initialResponse);

        HttpResponse httpGetResponse = new HttpResponse(
                toStream(initialResponse), 200);
        List<HttpResponse> responses = Arrays.asList(httpGetResponse);
        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
    }

    @Test
    public void testDataExtraction_GivenInitialResponseContainsTooLongScrollId() throws IOException
    {
        StringBuilder scrollId = new StringBuilder();
        for (int i = 0; i < 1024 * 1024; i++)
        {
            scrollId.append("a");
        }

        String initialResponse = "{"
                + "\"_scroll_id\":\""+ scrollId + "\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "}"
                + "}";
        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage("Field '_scroll_id' was expected but not found in first 1048576 bytes of response:\n" + initialResponse);

        HttpResponse httpGetResponse = new HttpResponse(
                toStream(initialResponse), 200);
        List<HttpResponse> responses = Arrays.asList(httpGetResponse);
        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
    }

    @Test
    public void testDataExtraction_GivenInitialResponseDoesNotReturnOk() throws IOException
    {
        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage(
                "Request 'http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=1000' failed with status code: 500. Response was:\n{}");

        String initialResponse = "{}";
        List<HttpResponse> responses = Arrays.asList(new HttpResponse(
                toStream(initialResponse), 500));
        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
    }

    @Test
    public void testDataExtraction_GivenScrollResponseDoesNotReturnOk() throws IOException
    {
        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage(
                "Request 'http://localhost:9200/_search/scroll?scroll=60m' with scroll id 'c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1' "
                + "failed with status code: 500. Response was:\n{}");

        String initialResponse = "{"
                + "\"_scroll_id\":\"c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"hits\":[..."
                + "}";
        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(initialResponse), 200),
                new HttpResponse(toStream("{}"), 500));
        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
    }

    @Test
    public void testNext_ThrowsGivenHasNotNext() throws IOException
    {
        m_ExpectedException.expect(NoSuchElementException.class);

        String initialResponse = "{"
                + "\"_scroll_id\":\"c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"hits\":[]"
                + "}";
        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(initialResponse), 200));
        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());
        m_Extractor.next();
    }

    @Test
    public void testDataExtractionWithFields() throws IOException
    {
        m_Fields = Arrays.asList("id");

        String initialResponse = "{"
                + "\"_scroll_id\":\"c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"fields\":{"
                + "      \"id\":[\"1403481600\"]"
                + "    }"
                + "  ]"
                + "}"
                + "}";

        String scrollResponse = "{"
                + "\"_scroll_id\":\"secondScrollId\","
                + "\"took\":8,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403782200\","
                + "    \"_score\":null,"
                + "    \"fields\":{"
                + "      \"id\":[\"1403782200\"]"
                + "    }"
                + "  ]"
                + "}"
                + "}";

        String scrollEndResponse = "{"
                + "\"_scroll_id\":\"thirdScrollId\","
                + "\"took\":8,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":[]"
                + "}"
                + "}";

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(initialResponse), 200),
                new HttpResponse(toStream(scrollResponse), 200),
                new HttpResponse(toStream(scrollEndResponse), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertEquals(scrollResponse, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());

        requester.assertEqualRequestsToResponses();

        RequestParams firstRequestParams = requester.getGetRequestParams(0);
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=1000", firstRequestParams.url);
        String expectedSearchBody = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\": {}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"time\": {"
                + "                \"gte\": \"1970-01-17T04:53:20.000Z\","
                + "                \"lt\": \"1970-01-17T05:53:20.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"fields\": [\"id\"]"
                + "}";
        assertEquals(expectedSearchBody, firstRequestParams.requestBody);

        RequestParams secondRequestParams = requester.getGetRequestParams(1);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", secondRequestParams.url);
        assertEquals("c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1", secondRequestParams.requestBody);

        RequestParams thirdRequestParams = requester.getGetRequestParams(2);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", thirdRequestParams.url);
        assertEquals("secondScrollId", thirdRequestParams.requestBody);

        assertEquals("http://localhost:9200/_search/scroll", requester.getDeleteRequestParams(0).url);
        assertEquals("{\"scroll_id\":[\"thirdScrollId\"]}",
                requester.getDeleteRequestParams(0).requestBody);
        assertEquals(1, requester.m_DeleteRequestParams.size());
    }

    @Test
    public void testDataExtractionWithAggregations() throws IOException
    {
        m_Aggregations = "{\"aggs\":{\"my-aggs\": {\"terms\":{\"field\":\"foo\"}}}}";

        String initialResponse = "{"
                + "\"_scroll_id\":\"r2d2bjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"aggregations\":{"
                + "  \"my-aggs\":{"
                + "    \"buckets\":["
                + "      {"
                + "        \"key\":\"foo\""
                + "      }"
                + "    ]"
                + "  }"
                + "}"
                + "}";

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(initialResponse), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse, streamToString(m_Extractor.next().get()));
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());

        requester.assertEqualRequestsToResponses();

        assertEquals(1, requester.m_GetRequestParams.size());
        RequestParams requestParams = requester.getGetRequestParams(0);
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=0", requestParams.url);
        String expectedSearchBody = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\": {}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"time\": {"
                + "                \"gte\": \"1970-01-17T04:53:20.000Z\","
                + "                \"lt\": \"1970-01-17T05:53:20.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  {\"aggs\":{\"my-aggs\": {\"terms\":{\"field\":\"foo\"}}}}"
                + "}";
        assertEquals(expectedSearchBody, requestParams.requestBody);

        assertEquals("http://localhost:9200/_search/scroll", requester.getDeleteRequestParams(0).url);
        assertEquals("{\"scroll_id\":[\"r2d2bjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\"]}",
                requester.getDeleteRequestParams(0).requestBody);
        assertEquals(1, requester.m_DeleteRequestParams.size());
    }

    @Test
    public void testDataExtractionWithAggregations_GivenResponseHasEmptyBuckets() throws IOException
    {
        m_Aggregations = "{\"aggs\":{\"my-aggs\": {\"terms\":{\"field\":\"foo\"}}}}";

        String initialResponse = "{"
                + "\"_scroll_id\":\"r2d2bjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"aggregations\":{"
                + "  \"my-aggs\":{"
                + "    \"buckets\":[]"
                + "  }"
                + "}"
                + "}";

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(initialResponse), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());

        requester.assertEqualRequestsToResponses();

        assertEquals(1, requester.m_GetRequestParams.size());
        RequestParams requestParams = requester.getGetRequestParams(0);
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=0", requestParams.url);
    }

    @Test
    public void testChunkedDataExtraction() throws IOException
    {
        String dataSummaryResponse = "{"
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":10000,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "},"
                + "\"aggregations\":{"
                +   "\"earliestTime\":{"
                +     "\"value\":1400000001000,"
                +     "\"value_as_string\":\"2014-05-13T16:53:21Z\""
                +   "},"
                +   "\"latestTime\":{"
                +     "\"value\":1400007201000,"
                +     "\"value_as_string\":\"2014-05-13T17:16:01Z\""
                +   "}"
                + "}"
                + "}";

        String indexResponse = "{"
                + "\"dataIndex\":{"
                + "  \"settings\":{"
                + "    \"index\":{"
                + "      \"creation_date\":0,"
                + "      \"number_of_shards\":\"5\","
                + "      \"number_of_replicas\":\"1\""
                + "    }"
                + "  }"
                + "}";

        String initialResponse1 = "{"
                + "\"_scroll_id\":\"scrollId_1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":10000,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        String continueResponse1 = "{"
                + "\"_scroll_id\":\"scrollId_2\","
                + "\"took\":8,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":10000,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403782200\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403782200\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        String endResponse1 = "{"
                + "\"_scroll_id\":\"scrollId_3\","
                + "\"took\":8,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":10000,"
                + "  \"max_score\":null,"
                + "  \"hits\":[]"
                + "}"
                + "}";

        String initialResponse2 = "{"
                + "\"_scroll_id\":\"scrollId_4\","
                + "\"hits\":{"
                + "  \"total\":10000,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\""
                + "  ]"
                + "}"
                + "}";

        String endResponse2 = "{"
                + "\"_scroll_id\":\"scrollId_5\","
                + "\"hits\":[]"
                + "}";

        String initialResponse3 = "{"
                + "\"_scroll_id\":\"scrollId_6\","
                + "\"hits\":[]"
                + "}";

        String dataSummaryResponse2 = "{"
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "},"
                + "\"aggregations\":{"
                +   "\"earliestTime\":{"
                +     "\"value\":1400007201000,"
                +     "\"value_as_string\":\"2014-05-13T17:16:01Z\""
                +   "},"
                +   "\"latestTime\":{"
                +     "\"value\":1400007201000,"
                +     "\"value_as_string\":\"2014-05-13T17:16:01Z\""
                +   "}"
                + "}"
                + "}";

        String initialResponse4 = "{"
                + "\"_scroll_id\":\"scrollId_7\","
                + "\"hits\":{"
                + "  \"total\":1,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\""
                + "  ]"
                + "}"
                + "}";

        String endResponse4 = "{"
                + "\"_scroll_id\":\"scrollId_8\","
                + "\"hits\":[]"
                + "}";

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(dataSummaryResponse), 200),
                new HttpResponse(toStream(indexResponse), 200),
                new HttpResponse(toStream(initialResponse1), 200),
                new HttpResponse(toStream(continueResponse1), 200),
                new HttpResponse(toStream(endResponse1), 200),
                new HttpResponse(toStream(initialResponse2), 200),
                new HttpResponse(toStream(endResponse2), 200),
                new HttpResponse(toStream(initialResponse3), 200),
                new HttpResponse(toStream(dataSummaryResponse2), 200),
                new HttpResponse(toStream(initialResponse4), 200),
                new HttpResponse(toStream(endResponse4), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000000L, 1407200000000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse1, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertEquals(continueResponse1, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse2, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse4, streamToString(m_Extractor.next().get()));
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());

        requester.assertEqualRequestsToResponses();

        int requestCount = 0;
        RequestParams requestParams = requester.getGetRequestParams(requestCount++);
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?size=1", requestParams.url);
        String expectedDataSummaryBody = "{"
                + "  \"sort\": [\"_doc\"],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"range\": {"
                + "          \"time\": {"
                + "            \"gte\": \"2014-05-13T16:53:20.000Z\","
                + "            \"lt\": \"2014-08-05T00:53:20.000Z\","
                + "            \"format\": \"date_time\""
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"aggs\":{"
                + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"time\"}"
                + "    },"
                + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"time\"}"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expectedDataSummaryBody.replace(" ", ""), requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        assertEquals("http://localhost:9200/dataIndex/_settings", requestParams.url);
        assertNull(requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=1000", requestParams.url);
        String expectedSearchBody = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\": {}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"time\": {"
                + "                \"gte\": \"2014-05-13T16:53:21.000Z\","
                + "                \"lt\": \"2014-05-13T17:53:21.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expectedSearchBody, requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", requestParams.url);
        assertEquals("scrollId_1", requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", requestParams.url);
        assertEquals("scrollId_2", requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        expectedSearchBody = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\": {}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"time\": {"
                + "                \"gte\": \"2014-05-13T17:53:21.000Z\","
                + "                \"lt\": \"2014-05-13T18:53:21.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}";
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=1000", requestParams.url);
        assertEquals(expectedSearchBody, requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", requestParams.url);
        assertEquals("scrollId_4", requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        expectedSearchBody = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\": {}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"time\": {"
                + "                \"gte\": \"2014-05-13T18:53:21.000Z\","
                + "                \"lt\": \"2014-05-13T19:53:21.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}";
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=1000", requestParams.url);
        assertEquals(expectedSearchBody, requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?size=1", requestParams.url);
        expectedDataSummaryBody = "{"
                + "  \"sort\": [\"_doc\"],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"range\": {"
                + "          \"time\": {"
                + "            \"gte\": \"2014-05-13T18:53:21.000Z\","
                + "            \"lt\": \"2014-08-05T00:53:20.000Z\","
                + "            \"format\": \"date_time\""
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"aggs\":{"
                + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"time\"}"
                + "    },"
                + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"time\"}"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expectedDataSummaryBody.replace(" ", ""), requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        expectedSearchBody = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\": {}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"time\": {"
                + "                \"gte\": \"2014-05-13T18:53:21.000Z\","
                + "                \"lt\": \"2014-08-05T00:53:20.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}";
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=1000", requestParams.url);
        assertEquals(expectedSearchBody, requestParams.requestBody);

        requestParams = requester.getGetRequestParams(requestCount++);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", requestParams.url);
        assertEquals("scrollId_7", requestParams.requestBody);

        assertEquals(requestCount, requester.m_RequestCount);

        String[] deletedScrollIds = {"scrollId_3", "scrollId_5", "scrollId_6", "scrollId_8"};
        assertEquals(4, requester.m_DeleteRequestParams.size());
        for (int i = 0; i < deletedScrollIds.length; i++)
        {
            assertEquals("http://localhost:9200/_search/scroll", requester.getDeleteRequestParams(i).url);
            assertEquals(String.format("{\"scroll_id\":[\"%s\"]}", deletedScrollIds[i]),
                    requester.getDeleteRequestParams(i).requestBody);
        }
    }

    @Test
    public void testChunkedDataExtraction_GivenZeroHits() throws IOException
    {
        String dataSummaryResponse = "{"
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":0,"
                + "  \"max_score\":null,"
                + "  \"hits\":[]"
                + "},"
                + "\"aggregations\":{"
                +   "\"earliestTime\":null,"
                +   "\"latestTime\":null"
                + "}"
                + "}";

        String searchResponse = "{"
                + "\"_scroll_id\":\"scrollId_1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":0,"
                + "  \"max_score\":null,"
                + "  \"hits\":[]"
                + "}"
                + "}";


        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(dataSummaryResponse), 200),
                new HttpResponse(toStream(searchResponse), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000000L, 1407200000000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());
    }

    @Test
    public void testChunkedDataExtraction_GivenDataSummaryRequestIsNotOk() throws IOException
    {
        String dataSummaryResponse = "{}";

        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage("Request 'http://localhost:9200/dataIndex/dataType/_search?size=1' "
                + "failed with status code: 400. Response was:\n" + dataSummaryResponse);

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(dataSummaryResponse), 400));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000000L, 1407200000000L, m_Logger);
    }

    @Test
    public void testChunkedDataExtraction_GivenEmptyDataSummaryResponse() throws IOException
    {
        String dataSummaryResponse = "{}";

        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage("Failed to parse string from pattern"
                + " '\"hits\":\\{.*?\"total\":(.*?),'. Response was:\n" + dataSummaryResponse);

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(dataSummaryResponse), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000000L, 1407200000000L, m_Logger);
    }

    @Test
    public void testChunkedDataExtraction_GivenTotalHitsCannotBeParsed() throws IOException
    {
        String dataSummaryResponse = "{"
                + "\"hits\":{"
                + "  \"total\":\"NaN\","
                + "  \"max_score\":null,"
                + "  \"hits\":[]"
                + "},"
                + "}";

        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage("Failed to parse long from pattern"
                + " '\"hits\":\\{.*?\"total\":(.*?),'. Response was:\n" + dataSummaryResponse);

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(dataSummaryResponse), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000000L, 1407200000000L, m_Logger);
    }

    @Test
    public void testCancel() throws IOException
    {
        String initialResponse = "{"
                + "\"_scroll_id\":\"c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        String scrollEndResponse = "{"
                + "\"_scroll_id\":\"thirdScrollId\","
                + "\"took\":8,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":[]"
                + "}"
                + "}";

        List<HttpResponse> responses = Arrays.asList(
                new HttpResponse(toStream(initialResponse), 200),
                new HttpResponse(toStream(scrollEndResponse), 200));

        MockHttpRequester requester = new MockHttpRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1403600000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse, streamToString(m_Extractor.next().get()));

        m_Extractor.cancel();

        assertFalse(m_Extractor.hasNext());
        assertEquals("http://localhost:9200/_search/scroll", requester.getDeleteRequestParams(0).url);
        assertEquals("{\"scroll_id\":[\"c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\"]}",
                requester.getDeleteRequestParams(0).requestBody);
        assertEquals(1, requester.m_DeleteRequestParams.size());
    }

    private static InputStream toStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String streamToString(InputStream stream) throws IOException
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return buffer.lines().collect(Collectors.joining("\n")).trim();
        }
    }

    private void createExtractor(MockHttpRequester httpRequester)
    {
        m_Extractor = new ElasticsearchDataExtractor(httpRequester, BASE_URL, null, INDICES, TYPES,
                SEARCH, m_Aggregations, m_ScriptFields, m_Fields, TIME_FIELD, 1000);
    }

    private static class MockHttpRequester extends HttpRequester
    {
        private List<HttpResponse> m_Responses;
        private int m_RequestCount = 0;
        private int m_AuthRequestCount = 0;
        private List<RequestParams> m_GetRequestParams;
        private List<RequestParams> m_DeleteRequestParams;

        public MockHttpRequester(List<HttpResponse> responses)
        {
            m_Responses = responses;
            m_GetRequestParams = new ArrayList<>(responses.size());
            m_DeleteRequestParams = new ArrayList<>();
        }

        @Override
        public HttpResponse get(String url, String authHeader, String requestBody)
        {
            m_GetRequestParams.add(new RequestParams(url, requestBody));
            if (authHeader != null)
            {
                ++m_AuthRequestCount;
            }
            return m_Responses.get(m_RequestCount++);
        }

        @Override
        public HttpResponse delete(String url, String authHeader, String requestBody)
        {
            m_DeleteRequestParams.add(new RequestParams(url, requestBody));
            return null;
        }

        public RequestParams getGetRequestParams(int callCount)
        {
            return m_GetRequestParams.get(callCount);
        }

        public RequestParams getDeleteRequestParams(int callCount)
        {
            return m_DeleteRequestParams.get(callCount);
        }

        public void assertEqualRequestsToResponses()
        {
            assertEquals(m_Responses.size(), m_GetRequestParams.size());
        }
    }

    private static class RequestParams
    {
        public final String url;
        public final String requestBody;

        public RequestParams(String url, String requestBody)
        {
            this.url = url;
            this.requestBody = requestBody;
        }
    }
}
