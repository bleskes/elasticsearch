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
package org.elasticsearch.xpack.prelert.scheduler.http;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ElasticsearchQueryBuilderTests extends ESTestCase {

    private static final String MATCH_ALL_QUERY = "{\"match_all\":{}}";

    public void testCreateSearchBody_GivenQueryOnly() {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(MATCH_ALL_QUERY, null, null, "time");

        assertFalse(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{" + "  \"sort\": [" + "    {\"time\": {\"order\": \"asc\"}}" + "  ]," + "  \"query\": {" + "    \"bool\": {"
                + "      \"filter\": [" + "        {\"match_all\":{}}," + "        {" + "          \"range\": {" + "            \"time\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\"," + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\"" + "            }" + "          }" + "        }" + "      ]" + "    }" + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    public void testCreateSearchBody_GivenQueryAndScriptFields() {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(MATCH_ALL_QUERY, null,
                "{\"test1\":{\"script\": \"...\"}}", "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{" + "  \"sort\": [" + "    {\"@timestamp\": {\"order\": \"asc\"}}" + "  ]," + "  \"query\": {"
                + "    \"bool\": {" + "      \"filter\": [" + "        {\"match_all\":{}}," + "        {" + "          \"range\": {"
                + "            \"@timestamp\": {" + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\"," + "              \"format\": \"date_time\"" + "            }"
                + "          }" + "        }" + "      ]" + "    }" + "  }," + "  \"script_fields\": {\"test1\":{\"script\":\"...\"}}"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    public void testCreateSearchBody_GivenQueryAndAggs() {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(MATCH_ALL_QUERY, "{\"my_aggs\":{}}", null, "time");

        assertTrue(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{" + "  \"sort\": [" + "    {\"time\": {\"order\": \"asc\"}}" + "  ]," + "  \"query\": {" + "    \"bool\": {"
                + "      \"filter\": [" + "        {\"match_all\":{}}," + "        {" + "          \"range\": {" + "            \"time\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\"," + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\"" + "            }" + "          }" + "        }" + "      ]" + "    }" + "  },"
                + "  \"aggs\":{\"my_aggs\":{}}" + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    public void testCreateDataSummaryQuery_GivenQueryOnly() {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(MATCH_ALL_QUERY, null, null, "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{" + "  \"sort\": [" + "    {\"_doc\": {\"order\": \"asc\"}}" + "  ]," + "  \"query\": {" + "    \"bool\": {"
                + "      \"filter\": [" + "        {\"match_all\":{}}," + "        {" + "          \"range\": {"
                + "            \"@timestamp\": {" + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\"," + "              \"format\": \"date_time\"" + "            }"
                + "          }" + "        }" + "      ]" + "    }" + "  }," + "  \"aggs\":{" + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}" + "    }," + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}" + "    }" + "  }" + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    public void testCreateDataSummaryQuery_GivenQueryAndScriptFields() {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(MATCH_ALL_QUERY, null,
                "{\"test1\":{\"script\": \"...\"}}", "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{" + "  \"sort\": [" + "    {\"_doc\": {\"order\": \"asc\"}}" + "  ]," + "  \"query\": {" + "    \"bool\": {"
                + "      \"filter\": [" + "        {\"match_all\":{}}," + "        {" + "          \"range\": {"
                + "            \"@timestamp\": {" + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\"," + "              \"format\": \"date_time\"" + "            }"
                + "          }" + "        }" + "      ]" + "    }" + "  }," + "  \"aggs\":{" + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}" + "    }," + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}" + "    }" + "  }" + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    public void testCreateDataSummaryQuery_GivenQueryAndAggs() {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(MATCH_ALL_QUERY, "{\"my_aggs\":{}}", null, "@timestamp");

        assertTrue(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{" + "  \"sort\": [" + "    {\"_doc\": {\"order\": \"asc\"}}" + "  ]," + "  \"query\": {" + "    \"bool\": {"
                + "      \"filter\": [" + "        {\"match_all\":{}}," + "        {" + "          \"range\": {"
                + "            \"@timestamp\": {" + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\"," + "              \"format\": \"date_time\"" + "            }"
                + "          }" + "        }" + "      ]" + "    }" + "  }," + "  \"aggs\":{" + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}" + "    }," + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}" + "    }" + "  }" + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    public void testLogQueryInfo_GivenNoAggsNoFields() {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(MATCH_ALL_QUERY, null, null, "@timestamp");

        Logger logger = mock(Logger.class);
        queryBuilder.logQueryInfo(logger);

        verify(logger).debug("Will retrieve whole _source document from Elasticsearch");
    }

    public void testLogQueryInfo() {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(MATCH_ALL_QUERY, "{\"my_aggs\":{ \"foo\": \"bar\" }}",
                null, "@timestamp");

        Logger logger = mock(Logger.class);
        queryBuilder.logQueryInfo(logger);

        verify(logger).debug("Will use the following Elasticsearch aggregations: {\"my_aggs\":{ \"foo\": \"bar\" }}");
    }
}
