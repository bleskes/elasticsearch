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

package com.prelert.data.extractor.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

        List<HttpGetResponse> responses = Arrays.asList(
                new HttpGetResponse(toStream(initialResponse), 200),
                new HttpGetResponse(toStream(scrollResponse), 200),
                new HttpGetResponse(toStream(scrollEndResponse), 200));

        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertEquals(scrollResponse, streamToString(m_Extractor.next().get()));
        assertTrue(m_Extractor.hasNext());
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());

        requester.assertEqualRequestsToResponses();

        RequestParams firstRequestParams = requester.getRequestParams(0);
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
                + "                \"lt\": \"1970-01-18T08:40:00.000Z\","
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

        RequestParams secondRequestParams = requester.getRequestParams(1);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", secondRequestParams.url);
        assertEquals("c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1", secondRequestParams.requestBody);

        RequestParams thirdRequestParams = requester.getRequestParams(2);
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", thirdRequestParams.url);
        assertEquals("secondScrollId", thirdRequestParams.requestBody);
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

        List<HttpGetResponse> responses = Arrays.asList(
                new HttpGetResponse(toStream(initialResponse), 200),
                new HttpGetResponse(toStream(scrollEndResponse), 200));
        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
        assertTrue(m_Extractor.hasNext());
        m_Extractor.next();
        assertFalse(m_Extractor.hasNext());

        assertEquals(scrollId.toString(), requester.getRequestParams(1).requestBody);
    }

    @Test
    public void testDataExtraction_GivenInitialResponseContainsNoHitsAndNoScrollId() throws IOException
    {
        m_ExpectedException.expect(IOException.class);
        m_ExpectedException.expectMessage("Field '_scroll_id' was expected but not found in response:\n{}");

        String initialResponse = "{}";
        HttpGetResponse httpGetResponse = new HttpGetResponse(
                toStream(initialResponse), 200);
        List<HttpGetResponse> responses = Arrays.asList(httpGetResponse);
        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

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
        m_ExpectedException.expectMessage("Field '_scroll_id' was expected but not found in response:\n" + initialResponse);

        HttpGetResponse httpGetResponse = new HttpGetResponse(
                toStream(initialResponse), 200);
        List<HttpGetResponse> responses = Arrays.asList(httpGetResponse);
        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

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
        m_ExpectedException.expectMessage("Field '_scroll_id' was expected but not found in response:\n" + initialResponse);

        HttpGetResponse httpGetResponse = new HttpGetResponse(
                toStream(initialResponse), 200);
        List<HttpGetResponse> responses = Arrays.asList(httpGetResponse);
        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

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
        List<HttpGetResponse> responses = Arrays.asList(new HttpGetResponse(
                toStream(initialResponse), 500));
        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

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
        List<HttpGetResponse> responses = Arrays.asList(
                new HttpGetResponse(toStream(initialResponse), 200),
                new HttpGetResponse(toStream("{}"), 500));
        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

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
        List<HttpGetResponse> responses = Arrays.asList(
                new HttpGetResponse(toStream(initialResponse), 200));
        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());
        m_Extractor.next();
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

        List<HttpGetResponse> responses = Arrays.asList(
                new HttpGetResponse(toStream(initialResponse), 200));

        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertEquals(initialResponse, streamToString(m_Extractor.next().get()));
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());

        requester.assertEqualRequestsToResponses();

        assertEquals(1, requester.m_RequestParams.size());
        RequestParams requestParams = requester.getRequestParams(0);
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
                + "                \"lt\": \"1970-01-18T08:40:00.000Z\","
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

        List<HttpGetResponse> responses = Arrays.asList(
                new HttpGetResponse(toStream(initialResponse), 200));

        MockHttpGetRequester requester = new MockHttpGetRequester(responses);
        createExtractor(requester);

        m_Extractor.newSearch(1400000000L, 1500000000L, m_Logger);

        assertTrue(m_Extractor.hasNext());
        assertFalse(m_Extractor.next().isPresent());
        assertFalse(m_Extractor.hasNext());

        requester.assertEqualRequestsToResponses();

        assertEquals(1, requester.m_RequestParams.size());
        RequestParams requestParams = requester.getRequestParams(0);
        assertEquals("http://localhost:9200/dataIndex/dataType/_search?scroll=60m&size=0", requestParams.url);
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

    private void createExtractor(MockHttpGetRequester httpGetRequester)
    {
        m_Extractor = new ElasticsearchDataExtractor(httpGetRequester, BASE_URL, null, INDICES, TYPES,
                SEARCH, m_Aggregations, TIME_FIELD);
    }

    private static class MockHttpGetRequester extends HttpGetRequester
    {
        private List<HttpGetResponse> m_Responses;
        private int m_RequestCount = 0;
        private int m_AuthRequestCount = 0;
        private List<RequestParams> m_RequestParams;

        public MockHttpGetRequester(List<HttpGetResponse> responses)
        {
            m_Responses = responses;
            m_RequestParams = new ArrayList<>(responses.size());
        }

        @Override
        public HttpGetResponse get(String url, String authHeader, String requestBody)
        {
            m_RequestParams.add(new RequestParams(url, requestBody));
            if (authHeader != null)
            {
                ++m_AuthRequestCount;
            }
            return m_Responses.get(m_RequestCount++);
        }

        public RequestParams getRequestParams(int callCount)
        {
            return m_RequestParams.get(callCount);
        }

        public void assertEqualRequestsToResponses()
        {
            assertEquals(m_Responses.size(), m_RequestParams.size());
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
