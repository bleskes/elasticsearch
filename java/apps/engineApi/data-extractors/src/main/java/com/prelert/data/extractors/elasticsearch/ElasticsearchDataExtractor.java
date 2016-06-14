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
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;
import com.prelert.job.data.extraction.DataExtractor;

public class ElasticsearchDataExtractor implements DataExtractor
{
    private static final String CLEAR_SCROLL_TEMPLATE = "{\"scroll_id\":[\"%s\"]}";
    private static final Pattern TOTAL_HITS_PATTERN = Pattern.compile("\"hits\":\\{.*?\"total\":(.*?),");
    private static final Pattern EARLIEST_TIME_PATTERN = Pattern.compile("\"earliestTime\":\\{.*?\"value\":(.*?),");
    private static final Pattern LATEST_TIME_PATTERN = Pattern.compile("\"latestTime\":\\{.*?\"value\":(.*?),");
    private static final Pattern INDEX_PATTERN = Pattern.compile("\"_index\":\"(.*?)\"");
    private static final Pattern NUMBER_OF_SHARDS_PATTERN = Pattern.compile("\"number_of_shards\":\"(.*?)\"");
    private static final long CHUNK_THRESHOLD_MS = 3600000;
    private static final long MIN_CHUNK_SIZE_MS = 10000L;

    private final HttpRequester m_HttpRequester;
    private final ElasticsearchUrlBuilder m_UrlBuilder;
    private final ElasticsearchQueryBuilder m_QueryBuilder;
    private final IndexSelector m_IndexSelector;
    private final int m_ScrollSize;
    private final ScrollState m_ScrollState;
    private volatile long m_CurrentStartTime;
    private volatile long m_CurrentEndTime;
    private volatile long m_EndTime;
    private volatile boolean m_IsFirstSearch;

    /**
     * The interval of each scroll search. Will be null when search is not chunked.
     */
    private volatile Long m_Chunk;

    private volatile Logger m_Logger;

    public ElasticsearchDataExtractor(HttpRequester httpRequester, ElasticsearchUrlBuilder urlBuilder,
            ElasticsearchQueryBuilder queryBuilder, IndexSelector indexSelector, int scrollSize)
    {
        m_HttpRequester = Objects.requireNonNull(httpRequester);
        m_UrlBuilder = Objects.requireNonNull(urlBuilder);
        m_IndexSelector = Objects.requireNonNull(indexSelector);
        m_ScrollSize = scrollSize;
        m_QueryBuilder = Objects.requireNonNull(queryBuilder);
        m_ScrollState =  queryBuilder.isAggregated() ? ScrollState.createAggregated()
                : ScrollState.createDefault();
        m_IsFirstSearch = true;
    }

    @Override
    public void newSearch(long startEpochMs, long endEpochMs, Logger logger) throws IOException
    {
        m_ScrollState.reset();
        m_IndexSelector.clearCache();
        m_CurrentStartTime = startEpochMs;
        m_CurrentEndTime = startEpochMs;
        m_EndTime = endEpochMs;
        m_Logger = logger;
        if (endEpochMs - startEpochMs > CHUNK_THRESHOLD_MS)
        {
            setUpChunkedSearch();
        }

        if (m_IsFirstSearch)
        {
            m_QueryBuilder.logQueryInfo(m_Logger);
            m_IsFirstSearch = false;
        }

        m_Logger.info("Requesting data from '" + m_UrlBuilder.getBaseUrl()
                + "' within [" + startEpochMs + ", " + endEpochMs + ")");
    }

    private void setUpChunkedSearch() throws IOException
    {
        m_CurrentEndTime = m_EndTime;
        m_Chunk = null;
        List<String> indices = m_IndexSelector.selectByTime(m_CurrentStartTime, m_EndTime, m_Logger);
        if (indices.isEmpty())
        {
            return;
        }

        String url = m_UrlBuilder.buildSearchSizeOneUrl(indices);
        String response = requestAndGetStringResponse(url,
                m_QueryBuilder.createDataSummaryQuery(m_CurrentStartTime, m_EndTime));
        long totalHits = matchLong(response, TOTAL_HITS_PATTERN);
        if (totalHits > 0)
        {
            // Aggregation value may be a double
            m_CurrentStartTime = (long) matchDouble(response, EARLIEST_TIME_PATTERN);
            m_CurrentEndTime = m_CurrentStartTime;
            long latestTime = (long) matchDouble(response, LATEST_TIME_PATTERN);
            long dataTimeSpread = latestTime - m_CurrentStartTime;
            if (dataTimeSpread > 0)
            {
                String index = matchString(response, INDEX_PATTERN);
                long shards = readNumberOfShards(index);
                m_Chunk = Math.max(MIN_CHUNK_SIZE_MS,
                        (shards * m_ScrollSize * dataTimeSpread) / totalHits);
            }
        }
    }

    private String requestAndGetStringResponse(String url, String body) throws IOException
    {
        HttpResponse response = m_HttpRequester.get(url, body);
        if (response.getResponseCode() != HttpResponse.OK_STATUS)
        {
            throw new IOException("Request '" + url + "' failed with status code: "
                    + response.getResponseCode() + ". Response was:\n" + response.getResponseAsString());
        }
        return response.getResponseAsString();
    }

    private static long matchLong(String response, Pattern pattern) throws IOException
    {
        String match = matchString(response, pattern);
        try
        {
            return Long.parseLong(match);
        }
        catch (NumberFormatException e)
        {
            throw new IOException("Failed to parse long from pattern '" + pattern
                    + "'. Response was:\n" + response, e);
        }
    }

    private static double matchDouble(String response, Pattern pattern) throws IOException
    {
        String match = matchString(response, pattern);
        try
        {
            return Double.parseDouble(match);
        }
        catch (NumberFormatException e)
        {
            throw new IOException("Failed to parse double from pattern '" + pattern
                    + "'. Response was:\n" + response, e);
        }
    }

    private static String matchString(String response, Pattern pattern) throws IOException
    {
        Matcher matcher = pattern.matcher(response);
        if (!matcher.find())
        {
            throw new IOException("Failed to parse string from pattern '" + pattern
                    + "'. Response was:\n" + response);
        }
        return matcher.group(1);
    }

    private long readNumberOfShards(String index) throws IOException
    {
        String url = m_UrlBuilder.buildIndexSettingsUrl(index);
        String response = requestAndGetStringResponse(url, null);
        return matchLong(response, NUMBER_OF_SHARDS_PATTERN);
    }

    public void clear()
    {
        m_ScrollState.reset();
        m_IndexSelector.clearCache();
    }

    @Override
    public Optional<InputStream> next() throws IOException
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }
        try
        {
            return getNextStream();
        }
        catch (IOException e)
        {
            m_Logger.error("An error occurred during requesting data from: "
                    + m_UrlBuilder.getBaseUrl(), e);
            m_ScrollState.forceComplete();
            throw e;
        }
    }

    private Optional<InputStream> getNextStream() throws IOException
    {
        while (hasNext())
        {
            boolean isNewScroll = m_ScrollState.getScrollId() == null || m_ScrollState.isComplete();
            InputStream stream = isNewScroll ? initScroll() : continueScroll();
            stream = m_ScrollState.updateFromStream(stream);
            if (m_ScrollState.isComplete())
            {
                clearScroll();

                // If it was a new scroll it means it returned 0 hits. If we are doing
                // a chunked search, we reconfigure the search in order to jump to the next
                // time interval where there are data.
                if (isNewScroll && m_Chunk != null)
                {
                    setUpChunkedSearch();
                }
            }
            else
            {
                return Optional.of(stream);
            }
        }
        return Optional.empty();
    }

    private void clearScroll()
    {
        if (m_ScrollState.getScrollId() == null)
        {
            return;
        }

        String url = m_UrlBuilder.buildClearScrollUrl();
        try
        {
            m_HttpRequester.delete(url,
                    String.format(CLEAR_SCROLL_TEMPLATE, m_ScrollState.getScrollId()));
        }
        catch (IOException e)
        {
            m_Logger.error("An error ocurred during clearing scroll context", e);
        }
    }

    @Override
    public boolean hasNext()
    {
        return !m_ScrollState.isComplete() || m_CurrentEndTime < m_EndTime;
    }

    private InputStream initScroll() throws IOException
    {
        advanceTime();
        List<String> indices = m_IndexSelector.selectByTime(m_CurrentStartTime, m_CurrentEndTime,
                m_Logger);
        if (indices.isEmpty())
        {
            // No indices contain data for the current time range, thus abort the search
            return null;
        }
        String url = buildInitScrollUrl(indices);
        String searchBody = m_QueryBuilder.createSearchBody(m_CurrentStartTime, m_CurrentEndTime);
        m_Logger.trace("About to submit body " + searchBody + " to URL " + url);
        HttpResponse response = m_HttpRequester.get(url, searchBody);
        if (response.getResponseCode() != HttpResponse.OK_STATUS)
        {
            throw new IOException("Request '" + url + "' failed with status code: "
                    + response.getResponseCode() + ". Response was:\n" + response.getResponseAsString());
        }
        return response.getStream();
    }

    private void advanceTime()
    {
        m_CurrentStartTime = m_CurrentEndTime;
        m_CurrentEndTime = m_Chunk == null ? m_EndTime
                : Math.min(m_CurrentStartTime + m_Chunk, m_EndTime);
    }

    private String buildInitScrollUrl(List<String> indices) throws IOException
    {
        Preconditions.checkArgument(!indices.isEmpty());
        // With aggregations we don't want any hits returned for the raw data,
        // just the aggregations
        int size = m_QueryBuilder.isAggregated() ? 0 : m_ScrollSize;
        return m_UrlBuilder.buildInitScrollUrl(indices, size);
    }

    private InputStream continueScroll() throws IOException
    {
        // Aggregations never need a continuation
        if (!m_QueryBuilder.isAggregated())
        {
            String url = m_UrlBuilder.buildContinueScrollUrl();
            HttpResponse response = m_HttpRequester.get(url, m_ScrollState.getScrollId());
            if (response.getResponseCode() == HttpResponse.OK_STATUS)
            {
                return response.getStream();
            }
            throw new IOException("Request '"  + url + "' with scroll id '"
                    + m_ScrollState.getScrollId() + "' failed with status code: "
                    + response.getResponseCode() + ". Response was:\n"
                    + response.getResponseAsString());
        }
        return null;
    }
}
