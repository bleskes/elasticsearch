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

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Range;

/**
 * <p> Selects the indices that contain data for a given time range
 * by using the field stats API of Elasticsearch.
 *
 * <p> When {@link #selectByTime(long, long)} is called an {@code IntervalTree}
 * is created and populated from the field stats response. The tree allows
 * for efficiently retrieving the indices that correspond to a given time range.
 *
 * <p> The tree is cached so that it can be reused for subsequent
 * selections that are for a time range that is enclosed by the
 * time range of the previously cached call. This is particularly
 * important for reducing the overhead of the way lookback searches
 * are performed. It allows for the client to cache the field stats
 * for the entire lookback time range and removes the need to
 * re-execute field stats for each of the searches corresponding
 * to the chunked time ranges.
 *
 * <p>Field stats behaves differently between various versions
 * of elasticsearch. In particular, before 2.0 there is no index_constraints.
 * Before 2.2, date fields are returned as strings and formatted
 * in a way that does not necessarily match the time format of the source data.
 * (e.g. fill in milliseconds in standard ISO timestamps without millis).
 * The decision here is to support field stats as it is in version 2.2 onwards.
 * Therefore, once it fails, we will fall back to returning all indices as we
 * do not want to fail the job just because field stats fails.
 */
public class FieldStatsCachedIndexSelector implements IndexSelector
{
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Request body for field_stats requesting the range of the time field
     * for those indices that have time field in a given range.
     * There are 3 expected arguments:
     * <ol>
     *   <li> time field
     *   <li> start epoch ms
     *   <li> end epoch ms
     * </ol
     */
    private static final String FIELD_STATS_BODY = "{"
            + "\"fields\": [\"%1$s\"],"
            + "\"index_constraints\": {"
            +   "\"%1$s\": {"
            +     "\"max_value\": {"
            +       "\"gte\": \"%2$s\","
            +       "\"format\": \"epoch_millis\""
            +     "},"
            +     "\"min_value\": {"
            +       "\"lt\": \"%3$s\","
            +       "\"format\": \"epoch_millis\""
            +     "}"
            +   "}"
            + "}"
            + "}";

    private static final String INDICES_NAME = "indices";
    private static final String MIN_VALUE_NAME = "min_value";
    private static final String MAX_VALUE_NAME = "max_value";

    private final HttpRequester m_HttpRequester;
    private final ElasticsearchUrlBuilder m_UrlBuilder;
    private final String m_TimeField;
    private final List<String> m_AllIndices;
    private final IntervalTree<Long, String> m_IndicesIntervalTree;
    private volatile Range<Long> m_PreviousCachedRange;

    /**
     * This is {@code true} if the field stats request has ever failed
     * since the last time the object was created.
     */
    private volatile boolean m_HasFieldStatsFailed;

    public FieldStatsCachedIndexSelector(HttpRequester httpRequester,
            ElasticsearchUrlBuilder urlBuilder, String timeField, List<String> allIndices)
    {
        m_HttpRequester = Objects.requireNonNull(httpRequester);
        m_UrlBuilder = Objects.requireNonNull(urlBuilder);
        m_TimeField = Objects.requireNonNull(timeField);
        m_AllIndices = Objects.requireNonNull(allIndices);
        m_IndicesIntervalTree = new IntervalTree<>();
        m_HasFieldStatsFailed = false;
    }

    @Override
    public List<String> selectByTime(long startMs, long endMs, Logger logger)
    {
        if (endMs <= startMs)
        {
            logger.error("selectByTime expects the end time to be strictly greater than the start "
                    + "time; actual call was: startMs = " + startMs + ", endMs = " + endMs);
            return Collections.emptyList();
        }

        if (m_HasFieldStatsFailed)
        {
            return m_AllIndices;
        }

        try
        {
            return selectByTimeUsingFieldStats(startMs, endMs);
        }
        catch (IOException e)
        {
            m_HasFieldStatsFailed = true;
            logger.warn("Failed to select indices using the field stats API; "
                    + "falling back to using configured indices. Reason was: " + e.getMessage());
            return m_AllIndices;
        }
    }

    private List<String> selectByTimeUsingFieldStats(long startMs, long endMs) throws IOException
    {
        Range<Long> timeRange = Range.closedOpen(startMs, endMs);
        synchronized (this)
        {
            if (!isCached(timeRange))
            {
                executeFieldStats(timeRange);
            }
            return m_IndicesIntervalTree.getIntersectingValues(timeRange);
        }
    }

    private boolean isCached(Range<Long> timeRange)
    {
        return m_PreviousCachedRange != null && m_PreviousCachedRange.encloses(timeRange);
    }

    private void executeFieldStats(Range<Long> timeRange) throws IOException
    {
        String url = m_UrlBuilder.buildFieldStatsUrl(m_AllIndices);
        String body = String.format(FIELD_STATS_BODY, m_TimeField, timeRange.lowerEndpoint(),
                timeRange.upperEndpoint());
        HttpResponse response = requestFieldStats(url, body);
        parseFieldStatsResponse(response);
        m_PreviousCachedRange = timeRange;
    }

    private HttpResponse requestFieldStats(String url, String body) throws IOException
    {
        HttpResponse response = m_HttpRequester.get(url, body);
        if (response.getResponseCode() != HttpResponse.OK_STATUS)
        {
            throw new IOException("Request to '"  + url + "' failed with status code "
                    + response.getResponseCode() + ". Response was:\n"
                    + response.getResponseAsString());
        }
        return response;
    }

    private void parseFieldStatsResponse(HttpResponse response) throws IOException
    {
        m_IndicesIntervalTree.clear();
        JsonNode rootNode = JSON_MAPPER.readTree(response.getStream());
        JsonNode indicesNode = findValue(rootNode, INDICES_NAME);
        Iterator<Entry<String, JsonNode>> indicesIterator = indicesNode.fields();
        while (indicesIterator.hasNext())
        {
            Entry<String, JsonNode> next = indicesIterator.next();
            String indexName = next.getKey();
            JsonNode indexNode = next.getValue();
            long minValue = findLong(indexNode, MIN_VALUE_NAME);
            long maxValue = findLong(indexNode, MAX_VALUE_NAME);
            Range<Long> indexRange = Range.closed(minValue, maxValue);
            m_IndicesIntervalTree.put(indexRange, indexName);
        }
    }

    private static JsonNode findValue(JsonNode node, String fieldName) throws IOException
    {
        JsonNode valueNode = node.findValue(fieldName);
        if (valueNode == null)
        {
            throw new IOException("Expected field '" + fieldName
                    + "' was missing from field stats response");
        }
        return valueNode;
    }

    private static long findLong(JsonNode node, String fieldName) throws IOException
    {
        JsonNode valueNode = findValue(node, fieldName);
        if (valueNode.canConvertToLong())
        {
            return valueNode.asLong();
        }
        throw new IOException("Field '" + fieldName
                + "' was expected to be a long; actual type was: " + valueNode.getNodeType());
    }

    @Override
    public void clearCache()
    {
        synchronized (this)
        {
            m_IndicesIntervalTree.clear();
            m_PreviousCachedRange = null;
        }
    }
}
