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
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.prelert.job.data.extraction.DataExtractor;

public class ElasticsearchDataExtractor implements DataExtractor
{
    private static final String CLEAR_SCROLL_TEMPLATE = "{\"scroll_id\":[\"%s\"]}";

    private static final int OK_STATUS = 200;
    private static final String SLASH = "/";
    private static final String COMMA = ",";
    private static final int SCROLL_CONTEXT_MINUTES = 60;
    private static final String INDEX_SETTINGS_END_POINT = "%s/_settings";
    private static final String SEARCH_SIZE_ONE_END_POINT = "_search?size=1";
    private static final String SEARCH_SCROLL_END_POINT = "_search?scroll=" + SCROLL_CONTEXT_MINUTES + "m&size=%d";
    private static final String CONTINUE_SCROLL_END_POINT = "_search/scroll?scroll=" + SCROLL_CONTEXT_MINUTES + "m";
    private static final String CLEAR_SCROLL_END_POINT = "_search/scroll";

    private static final Pattern TOTAL_HITS_PATTERN = Pattern.compile("\"hits\":\\{.*?\"total\":(.*?),");
    private static final Pattern EARLIEST_TIME_PATTERN = Pattern.compile("\"earliestTime\":\\{.*?\"value\":(.*?),");
    private static final Pattern LATEST_TIME_PATTERN = Pattern.compile("\"latestTime\":\\{.*?\"value\":(.*?),");
    private static final Pattern INDEX_PATTERN = Pattern.compile("\"_index\":\"(.*?)\"");
    private static final Pattern NUMBER_OF_SHARDS_PATTERN = Pattern.compile("\"number_of_shards\":\"(.*?)\"");
    private static final long CHUNK_THRESHOLD_MS = 3600000;
    private static final long MIN_CHUNK_SIZE_MS = 10000L;

    private final HttpRequester m_HttpRequester;
    private final String m_BaseUrl;
    private final List<String> m_Indices;
    private final List<String> m_Types;
    private final int m_ScrollSize;
    private final ElasticsearchQueryBuilder m_QueryBuilder;
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

    public ElasticsearchDataExtractor(HttpRequester httpRequester, String baseUrl,
            List<String> indices, List<String> types, ElasticsearchQueryBuilder queryBuilder,
            int scrollSize)
    {
        m_HttpRequester = Objects.requireNonNull(httpRequester);
        m_BaseUrl = Objects.requireNonNull(baseUrl);
        m_Indices = Objects.requireNonNull(indices);
        m_Types = Objects.requireNonNull(types);
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

        m_Logger.info("Requesting data from '" + m_BaseUrl + "' within [" + startEpochMs + ", "
                + endEpochMs + ")");
    }

    private void setUpChunkedSearch() throws IOException
    {
        m_Chunk = null;
        String url = buildUrlWithIndicesAndTypes().append(SEARCH_SIZE_ONE_END_POINT).toString();
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
        else
        {
            m_CurrentEndTime = m_EndTime;
        }
    }

    private StringBuilder buildUrlWithIndicesAndTypes()
    {
        return newUrlBuilder()
                .append(m_Indices.stream().collect(Collectors.joining(COMMA))).append(SLASH)
                .append(m_Types.stream().collect(Collectors.joining(COMMA))).append(SLASH);
    }

    private StringBuilder newUrlBuilder()
    {
        StringBuilder urlBuilder = new StringBuilder(m_BaseUrl);
        if (!m_BaseUrl.endsWith(SLASH))
        {
            urlBuilder.append(SLASH);
        }
        return urlBuilder;
    }

    private long readNumberOfShards(String index) throws IOException
    {
        String response = requestAndGetStringResponse(
                newUrlBuilder().append(String.format(INDEX_SETTINGS_END_POINT, index)).toString(),
                null);
        return matchLong(response, NUMBER_OF_SHARDS_PATTERN);
    }

    private String requestAndGetStringResponse(String url, String body) throws IOException
    {
        HttpResponse response = m_HttpRequester.get(url, body);
        if (response.getResponseCode() != OK_STATUS)
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
            m_Logger.error("An error occurred during requesting data from: " + m_BaseUrl, e);
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

        StringBuilder urlBuilder = newUrlBuilder();
        urlBuilder.append(CLEAR_SCROLL_END_POINT);
        try
        {
            m_HttpRequester.delete(urlBuilder.toString(),
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
        String url = buildInitScrollUrl();
        String searchBody = m_QueryBuilder.createSearchBody(m_CurrentStartTime, m_CurrentEndTime);
        m_Logger.trace("About to submit body " + searchBody + " to URL " + url);
        HttpResponse response = m_HttpRequester.get(url, searchBody);
        if (response.getResponseCode() != OK_STATUS)
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

    private String buildInitScrollUrl()
    {
        StringBuilder urlBuilder = buildUrlWithIndicesAndTypes();
        // With aggregations we don't want any hits returned for the raw data,
        // just the aggregations
        int size = m_QueryBuilder.isAggregated() ? 0 : m_ScrollSize;
        urlBuilder.append(String.format(SEARCH_SCROLL_END_POINT, size));
        return urlBuilder.toString();
    }

    private InputStream continueScroll() throws IOException
    {
        // Aggregations never need a continuation
        if (!m_QueryBuilder.isAggregated())
        {
            StringBuilder urlBuilder = newUrlBuilder();
            urlBuilder.append(CONTINUE_SCROLL_END_POINT);
            HttpResponse response = m_HttpRequester.get(urlBuilder.toString(),
                    m_ScrollState.getScrollId());
            if (response.getResponseCode() == OK_STATUS)
            {
                return response.getStream();
            }
            throw new IOException("Request '"  + urlBuilder.toString() + "' with scroll id '"
                    + m_ScrollState.getScrollId() + "' failed with status code: "
                    + response.getResponseCode() + ". Response was:\n"
                    + response.getResponseAsString());
        }
        return null;
    }
}
