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

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.data.extraction.DataExtractor;

public class ElasticsearchDataExtractor implements DataExtractor
{
    /**
     * The search body contains sorting based on the time field
     * and a query. The query is composed by a bool query with
     * two must clauses, the recommended way to perform an AND query.
     * There are 6 placeholders:
     * <ol>
     *   <li> time field
     *   <li> user defined query
     *   <li> time field
     *   <li> start time (String in date_time format)
     *   <li> end time (String in date_time format)
     *   <li> aggregations (may be empty)
     * </ol
     */
    private static final String SEARCH_BODY_TEMPLATE = "{"
            + "  \"sort\": ["
            + "    {\"%s\": {\"order\": \"asc\"}}"
            + "  ],"
            + "  \"query\": {"
            + "    \"filtered\": {"
            + "      \"filter\": {"
            + "        \"bool\": {"
            + "          \"must\": {"
            + "            %s"
            + "          },"
            + "          \"must\": {"
            + "            \"range\": {"
            + "              \"%s\": {"
            + "                \"gte\": \"%s\","
            + "                \"lt\": \"%s\","
            + "                \"format\": \"date_time\""
            + "              }"
            + "            }"
            + "          }"
            + "        }"
            + "      }"
            + "    }"
            + "  }%s"
            + "}";

    /**
     * The data summary query returns the earliest and latest data times for a time range.
     * There are 3 placeholders:
     * <ol>
     *   <li> time field
     *   <li> start time (String in date_time format)
     *   <li> end time (String in date_time format)
     * </ol
     */
    private static final String DATA_SUMMARY_QUERY_TEMPLATE = "{"
            + "\"sort\":[\"_doc\"],"
            + "\"query\":{"
            +   "\"filtered\":{"
            +     "\"filter\":{"
            +       "\"range\":{"
            +         "\"%1$s\":{"
            +           "\"gte\":\"%2$s\","
            +           "\"lt\":\"%3$s\","
            +           "\"format\":\"date_time\""
            +         "}"
            +       "}"
            +     "}"
            +   "}"
            + "},"
            + "\"aggs\":{"
            +   "\"earliestTime\":{"
            +     "\"min\":{\"field\":\"%1$s\"}"
            +   "},"
            +   "\"latestTime\":{"
            +     "\"max\":{\"field\":\"%1$s\"}"
            +   "}"
            + "}"
            + "}";

    private static final String AGGREGATION_TEMPLATE = ",  %s";
    private static final String FIELDS_TEMPLATE = ",  \"fields\": %s";
    private static final String CLEAR_SCROLL_TEMPLATE = "{\"scroll_id\":[\"%s\"]}";

    private static final int OK_STATUS = 200;
    private static final String SLASH = "/";
    private static final String COMMA = ",";
    private static final int UNAGGREGATED_SCROLL_SIZE = 1000;
    private static final int SCROLL_CONTEXT_MINUTES = 60;
    private static final String INDEX_SETTINGS_END_POINT = "%s/_settings";
    private static final String SEARCH_SIZE_ONE_END_POINT = "_search?size=1";
    private static final String SEARCH_SCROLL_END_POINT = "_search?scroll=" + SCROLL_CONTEXT_MINUTES + "m&size=%d";
    private static final String CONTINUE_SCROLL_END_POINT = "_search/scroll?scroll=" + SCROLL_CONTEXT_MINUTES + "m";
    private static final String CLEAR_SCROLL_END_POINT = "_search/scroll";

    private static final Pattern TOTAL_HITS_PATTERN = Pattern.compile("\"hits\":\\{\"total\":(.*?),");
    private static final Pattern EARLIEST_TIME_PATTERN = Pattern.compile("\"earliestTime\":\\{\"value\":(.*?),");
    private static final Pattern LATEST_TIME_PATTERN = Pattern.compile("\"latestTime\":\\{\"value\":(.*?),");
    private static final Pattern INDEX_PATTERN = Pattern.compile("\"_index\":\"(.*?)\"");
    private static final Pattern NUMBER_OF_SHARDS_PATTERN = Pattern.compile("\"number_of_shards\":\"(.*?)\"");
    private static final long CHUNK_THRESHOLD_MS = 3600000;

    private final HttpRequester m_HttpRequester;
    private final String m_BaseUrl;
    private final String m_AuthHeader;
    private final List<String> m_Indices;
    private final List<String> m_Types;
    private final String m_Search;
    private final String m_Aggregations;
    private final List<String> m_Fields;
    private final String m_TimeField;
    private final ScrollState m_ScrollState;
    private volatile long m_CurrentStartTime;
    private volatile long m_CurrentEndTime;
    private volatile long m_EndTime;

    /**
     * The interval of each scroll search. Will be null when search is not chunked.
     */
    private volatile Long m_Chunk;

    private volatile Logger m_Logger;

    ElasticsearchDataExtractor(HttpRequester httpRequester, String baseUrl, String authHeader,
            List<String> indices, List<String> types, String search, String aggregations,
            List<String> fields, String timeField)
    {
        m_HttpRequester = Objects.requireNonNull(httpRequester);
        m_BaseUrl = Objects.requireNonNull(baseUrl);
        m_AuthHeader = authHeader;
        m_Indices = Objects.requireNonNull(indices);
        m_Types = Objects.requireNonNull(types);
        m_Search = Objects.requireNonNull(search);
        m_Aggregations = aggregations;
        m_Fields = fields;
        m_TimeField = Objects.requireNonNull(timeField);
        m_ScrollState =  m_Aggregations == null ? ScrollState.createDefault()
                : ScrollState.createAggregated();
    }

    public static ElasticsearchDataExtractor create(String baseUrl, String authHeader,
            List<String> indices, List<String> types, String search, String aggregations,
            List<String> fields, String timeField)
    {
        return new ElasticsearchDataExtractor(new HttpRequester(), baseUrl, authHeader, indices, types,
                search, aggregations, fields, timeField);
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

        m_Logger.info("Requesting data from '" + m_BaseUrl + "' within [" + startEpochMs + ", "
                + endEpochMs + ")");
    }

    private void setUpChunkedSearch() throws IOException
    {
        m_Chunk = null;
        String url = buildUrlWithIndicesAndTypes().append(SEARCH_SIZE_ONE_END_POINT).toString();
        String response = requestAndGetStringResponse(url,
                createDataSummaryQuery(m_CurrentStartTime, m_EndTime));
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
                m_Chunk = (shards * UNAGGREGATED_SCROLL_SIZE * dataTimeSpread) / totalHits;
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

    private String createDataSummaryQuery(long start, long end)
    {
        return String.format(DATA_SUMMARY_QUERY_TEMPLATE,
                m_TimeField, formatAsDateTime(start), formatAsDateTime(end));
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
        HttpResponse response = m_HttpRequester.get(url, m_AuthHeader, body);
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
            throw new IOException("Failed to parse long from pattern \"" + pattern
                    + "\". Response was:\n" + response, e);
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
            throw new IOException("Failed to parse double from pattern \"" + pattern
                    + "\". Response was:\n" + response, e);
        }
    }

    private static String matchString(String response, Pattern pattern) throws IOException
    {
        Matcher matcher = pattern.matcher(response);
        if (!matcher.find())
        {
            throw new IOException("Failed to parse string from pattern \"" + pattern
                    + "\". Response was:\n" + response);
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
            m_HttpRequester.delete(urlBuilder.toString(), m_AuthHeader,
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
        String searchBody = createSearchBody(m_CurrentStartTime, m_CurrentEndTime);
        m_Logger.trace("About to submit body " + searchBody + " to URL " + url);
        HttpResponse response = m_HttpRequester.get(url, m_AuthHeader, searchBody);
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
        int size = (m_Aggregations != null) ? 0 : UNAGGREGATED_SCROLL_SIZE;
        urlBuilder.append(String.format(SEARCH_SCROLL_END_POINT, size));
        return urlBuilder.toString();
    }

    private String createSearchBody(long start, long end)
    {
        return String.format(SEARCH_BODY_TEMPLATE, m_TimeField, m_Search, m_TimeField,
                formatAsDateTime(start), formatAsDateTime(end),
                createResultsFormatSpec());
    }

    private String createResultsFormatSpec()
    {
        return (m_Aggregations != null) ? createAggregations() :
                ((m_Fields != null) ? createFieldDataFields() : "");
    }

    private static String formatAsDateTime(long epochMs)
    {
        Instant instant = Instant.ofEpochMilli(epochMs);
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    }

    private String createAggregations()
    {
        return String.format(AGGREGATION_TEMPLATE, m_Aggregations);
    }

    private String createFieldDataFields()
    {
        try
        {
            return String.format(FIELDS_TEMPLATE, new ObjectMapper().writeValueAsString(m_Fields));
        }
        catch (JsonProcessingException e)
        {
            m_Logger.error("Could not convert field list to JSON: " + m_Fields, e);
        }
        return "";
    }

    private InputStream continueScroll() throws IOException
    {
        // Aggregations never need a continuation
        if (m_Aggregations == null)
        {
            StringBuilder urlBuilder = newUrlBuilder();
            urlBuilder.append(CONTINUE_SCROLL_END_POINT);
            HttpResponse response = m_HttpRequester.get(urlBuilder.toString(), m_AuthHeader,
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
