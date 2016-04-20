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
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.job.ElasticsearchDataSourceCompatibility;

public class ElasticsearchQueryBuilderTest
{
    @Test
    public void testCreateSearchBody_GivenQueryOnlyAndCompatibility_1_7_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}", null, null, null,
                "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"@timestamp\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\":{}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"@timestamp\": {"
                + "                \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "                \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    @Test
    public void testCreateSearchBody_GivenQueryOnlyAndCompatibility_2_X_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_2_X_X, "\"match_all\":{}", null, null, null,
                "time");

        assertFalse(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"bool\": {"
                + "      \"filter\": ["
                + "        {\"match_all\":{}},"
                + "        {"
                + "          \"range\": {"
                + "            \"time\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\""
                + "            }"
                + "          }"
                + "        }"
                + "      ]"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    @Test
    public void testCreateSearchBody_GivenQueryAndFieldsAndCompatibility_1_7_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}", null, null,
                "[\"foo\",\"bar\"]", "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"@timestamp\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\":{}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"@timestamp\": {"
                + "                \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "                \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"fields\": [\"foo\",\"bar\"]"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    @Test
    public void testCreateSearchBody_GivenQueryAndFieldsAndCompatibility_2_X_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_2_X_X, "\"match_all\":{}", null, null,
                "[\"foo\",\"bar\"]", "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"@timestamp\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"bool\": {"
                + "      \"filter\": ["
                + "        {\"match_all\":{}},"
                + "        {"
                + "          \"range\": {"
                + "            \"@timestamp\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\""
                + "            }"
                + "          }"
                + "        }"
                + "      ]"
                + "    }"
                + "  },"
                + "  \"fields\": [\"foo\",\"bar\"]"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    @Test
    public void testCreateSearchBody_GivenQueryAndFieldsAndScriptFieldsAndCompatibility_1_7_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}",
                null,
                "{\"test1\":{\"script\": \"...\"}}",
                "[\"foo\",\"bar\"]", "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"@timestamp\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\":{}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"@timestamp\": {"
                + "                \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "                \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"script_fields\": {\"test1\":{\"script\":\"...\"}},"
                + "  \"fields\": [\"foo\",\"bar\"]"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    @Test
    public void testCreateSearchBody_GivenQueryAndFieldsAndScriptFieldsAndCompatibility_2_X_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_2_X_X, "\"match_all\":{}", null,
                "{\"test1\":{\"script\": \"...\"}}",
                "[\"foo\",\"bar\"]", "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"@timestamp\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"bool\": {"
                + "      \"filter\": ["
                + "        {\"match_all\":{}},"
                + "        {"
                + "          \"range\": {"
                + "            \"@timestamp\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\""
                + "            }"
                + "          }"
                + "        }"
                + "      ]"
                + "    }"
                + "  },"
                + "  \"script_fields\": {\"test1\":{\"script\":\"...\"}},"
                + "  \"fields\": [\"foo\",\"bar\"]"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    @Test
    public void testCreateSearchBody_GivenQueryAndAggsAndCompatibility_1_7_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}",
                "{\"my_aggs\":{}}",
                null, null,
                "@timestamp");

        assertTrue(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"@timestamp\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\": {"
                + "            \"match_all\":{}"
                + "          },"
                + "          \"must\": {"
                + "            \"range\": {"
                + "              \"@timestamp\": {"
                + "                \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "                \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"aggs\":{\"my_aggs\":{}}"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    @Test
    public void testCreateSearchBody_GivenQueryAndAggsAndCompatibility_2_X_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_2_X_X, "\"match_all\":{}",
                "{\"my_aggs\":{}}",
                null, null,
                "time");

        assertTrue(queryBuilder.isAggregated());

        String searchBody = queryBuilder.createSearchBody(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"time\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"bool\": {"
                + "      \"filter\": ["
                + "        {\"match_all\":{}},"
                + "        {"
                + "          \"range\": {"
                + "            \"time\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\""
                + "            }"
                + "          }"
                + "        }"
                + "      ]"
                + "    }"
                + "  },"
                + "  \"aggs\":{\"my_aggs\":{}}"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), searchBody.replaceAll(" ", ""));
    }

    @Test
    public void testCreateDataSummaryQuery_GivenQueryOnlyAndCompatibility_1_7_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}", null, null, null,
                "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"_doc\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\":{\"match_all\":{}},"
                + "          \"must\":{"
                + "            \"range\": {"
                + "              \"@timestamp\": {"
                + "                \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "                \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"aggs\":{"
                + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}"
                + "    },"
                + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    @Test
    public void testCreateDataSummaryQuery_GivenQueryOnlyAndCompatibility_2_X_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_2_X_X, "\"match_all\":{}", null, null, null,
                "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"_doc\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"bool\": {"
                + "      \"filter\": ["
                + "        {\"match_all\":{}},"
                + "        {"
                + "          \"range\": {"
                + "            \"@timestamp\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\""
                + "            }"
                + "          }"
                + "        }"
                + "      ]"
                + "    }"
                + "  },"
                + "  \"aggs\":{"
                + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}"
                + "    },"
                + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    @Test
    public void testCreateDataSummaryQuery_GivenQueryAndFieldsAndScriptFieldsAndCompatibility_1_7_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}",
                null,
                "{\"test1\":{\"script\": \"...\"}}",
                "[\"foo\",\"bar\"]", "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"_doc\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\":{\"match_all\":{}},"
                + "          \"must\":{"
                + "            \"range\": {"
                + "              \"@timestamp\": {"
                + "                \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "                \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"aggs\":{"
                + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}"
                + "    },"
                + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    @Test
    public void testCreateDataSummaryQuery_GivenQueryAndFieldsAndScriptFieldsAndCompatibility_2_X_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_2_X_X, "\"match_all\":{}",
                null,
                "{\"test1\":{\"script\": \"...\"}}",
                "[\"foo\",\"bar\"]", "@timestamp");

        assertFalse(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"_doc\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"bool\": {"
                + "      \"filter\": ["
                + "        {\"match_all\":{}},"
                + "        {"
                + "          \"range\": {"
                + "            \"@timestamp\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\""
                + "            }"
                + "          }"
                + "        }"
                + "      ]"
                + "    }"
                + "  },"
                + "  \"aggs\":{"
                + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}"
                + "    },"
                + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    @Test
    public void testCreateDataSummaryQuery_GivenQueryAndAggsAndCompatibility_1_7_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}",
                "{\"my_aggs\":{}}",
                null, null,
                "@timestamp");

        assertTrue(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"_doc\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"filtered\": {"
                + "      \"filter\": {"
                + "        \"bool\": {"
                + "          \"must\":{\"match_all\":{}},"
                + "          \"must\":{"
                + "            \"range\": {"
                + "              \"@timestamp\": {"
                + "                \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "                \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "                \"format\": \"date_time\""
                + "              }"
                + "            }"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  },"
                + "  \"aggs\":{"
                + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}"
                + "    },"
                + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    @Test
    public void testCreateDataSummaryQuery_GivenQueryAndAggsAndCompatibility_2_X_X()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_2_X_X, "\"match_all\":{}",
                "{\"my_aggs\":{}}",
                null, null,
                "@timestamp");

        assertTrue(queryBuilder.isAggregated());

        String dataSummaryQuery = queryBuilder.createDataSummaryQuery(1451606400000L, 1451610000000L);

        String expected = "{"
                + "  \"sort\": ["
                + "    {\"_doc\": {\"order\": \"asc\"}}"
                + "  ],"
                + "  \"query\": {"
                + "    \"bool\": {"
                + "      \"filter\": ["
                + "        {\"match_all\":{}},"
                + "        {"
                + "          \"range\": {"
                + "            \"@timestamp\": {"
                + "              \"gte\": \"2016-01-01T00:00:00.000Z\","
                + "              \"lt\": \"2016-01-01T01:00:00.000Z\","
                + "              \"format\": \"date_time\""
                + "            }"
                + "          }"
                + "        }"
                + "      ]"
                + "    }"
                + "  },"
                + "  \"aggs\":{"
                + "    \"earliestTime\":{"
                + "      \"min\":{\"field\":\"@timestamp\"}"
                + "    },"
                + "    \"latestTime\":{"
                + "      \"max\":{\"field\":\"@timestamp\"}"
                + "    }"
                + "  }"
                + "}";
        assertEquals(expected.replaceAll(" ", ""), dataSummaryQuery.replaceAll(" ", ""));
    }

    @Test
    public void testLogQueryInfo_GivenNoAggsNoFields()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}", null, null, null,
                "@timestamp");

        Logger logger = mock(Logger.class);
        queryBuilder.logQueryInfo(logger);

        verify(logger).debug("Will retrieve whole _source document from Elasticsearch");
    }

    @Test
    public void testLogQueryInfo_GivenFields()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}", null, null,
                "[\"foo\"]",
                "@timestamp");

        Logger logger = mock(Logger.class);
        queryBuilder.logQueryInfo(logger);

        verify(logger).debug("Will request only the following field(s) from Elasticsearch: [\"foo\"]");
    }

    @Test
    public void testLogQueryInfo_GivenAggs()
    {
        ElasticsearchQueryBuilder queryBuilder = new ElasticsearchQueryBuilder(
                ElasticsearchDataSourceCompatibility.V_1_7_X, "\"match_all\":{}",
                "{\"my_aggs\":{}}",
                null, null, "@timestamp");

        Logger logger = mock(Logger.class);
        queryBuilder.logQueryInfo(logger);

        verify(logger).debug("Will use the following Elasticsearch aggregations: {\"my_aggs\":{}}");
    }
}
