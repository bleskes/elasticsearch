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
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

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
     *   <li> start time (epoch ms)
     *   <li> end time (epoch ms)
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

    private static final String AGGREGATION_TEMPLATE = ","
            + "  %s";

    private static final int OK_STATUS = 200;
    private static final String SLASH = "/";
    private static final String COMMA = ",";
    private static final String SEARCH_SCROLL_TEMPLATE = "_search?scroll=60m&size=%d";
    private static final int UNAGGREGATED_SCROLL_SIZE = 1000;
    private static final String CONTINUE_SCROLL_END_POINT = "_search/scroll?scroll=60m";

    private final HttpGetRequester m_HttpGetRequester;
    private final String m_BaseUrl;
    private final String m_AuthHeader;
    private final List<String> m_Indices;
    private final List<String> m_Types;
    private final String m_Search;
    private final String m_Aggregations;
    private final String m_TimeField;
    private final ScrollState m_ScrollState;
    private volatile String m_StartTime;
    private volatile String m_EndTime;
    private volatile Logger m_Logger;

    ElasticsearchDataExtractor(HttpGetRequester httpGetRequester, String baseUrl, String authHeader,
            List<String> indices, List<String> types, String search, String aggregations, String timeField)
    {
        m_HttpGetRequester = Objects.requireNonNull(httpGetRequester);
        m_BaseUrl = Objects.requireNonNull(baseUrl);
        m_AuthHeader = authHeader;
        m_Indices = Objects.requireNonNull(indices);
        m_Types = Objects.requireNonNull(types);
        m_Search = Objects.requireNonNull(search);
        m_Aggregations = aggregations;
        m_TimeField = Objects.requireNonNull(timeField);
        m_ScrollState =  m_Aggregations == null ? ScrollState.createDefault()
                : ScrollState.createAggregated();
    }

    public static ElasticsearchDataExtractor create(String baseUrl, String authHeader,
            List<String> indices, List<String> types, String search, String aggregations, String timeField)
    {
        return new ElasticsearchDataExtractor(new HttpGetRequester(), baseUrl, authHeader, indices, types,
                search, aggregations, timeField);
    }

    @Override
    public void newSearch(long startEpochMs, long endEpochMs, Logger logger)
    {
        m_ScrollState.reset();
        m_StartTime = formatAsDateTime(startEpochMs);
        m_EndTime = formatAsDateTime(endEpochMs);
        m_Logger = logger;

        m_Logger.info("Requesting data from '" + m_BaseUrl + "' within [" + startEpochMs + ", "
                + endEpochMs + ")");
    }

    private static String formatAsDateTime(long epochMs)
    {
        Instant instant = Instant.ofEpochMilli(epochMs);
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    }

    @Override
    public Optional<InputStream> next() throws IOException
    {
        if (m_ScrollState.isComplete())
        {
            throw new NoSuchElementException();
        }
        try
        {
            InputStream stream = (m_ScrollState.getScrollId() == null) ? initScroll() : continueScroll();
            stream = m_ScrollState.updateFromStream(stream);
            return m_ScrollState.isComplete() ? Optional.empty() : Optional.of(stream);
        }
        catch (IOException e)
        {
            m_Logger.error("An error occurred during requesting data from: " + m_BaseUrl, e);
            m_ScrollState.forceComplete();
            throw e;
        }
    }

    @Override
    public boolean hasNext()
    {
        return !m_ScrollState.isComplete();
    }

    private InputStream initScroll() throws IOException
    {
        String url = buildInitScrollUrl();
        String searchBody = createSearchBody();
        m_Logger.trace("About to submit body " + searchBody + " to URL " + url);
        HttpGetResponse response = m_HttpGetRequester.get(url, m_AuthHeader, searchBody);
        if (response.getResponseCode() != OK_STATUS)
        {
            throw new IOException("Request '" + url + "' failed with status code: "
                    + response.getResponseCode() + ". Response was:\n" + response.getResponseAsString());
        }
        return response.getStream();
    }

    private String buildInitScrollUrl()
    {
        StringBuilder urlBuilder = newUrlBuilder();
        urlBuilder.append(m_Indices.stream().collect(Collectors.joining(COMMA)));
        urlBuilder.append(SLASH);
        urlBuilder.append(m_Types.stream().collect(Collectors.joining(COMMA)));
        urlBuilder.append(SLASH);
        // With aggregations we don't want any hits returned for the raw data,
        // just the aggregations
        int size = (m_Aggregations != null) ? 0 : UNAGGREGATED_SCROLL_SIZE;
        urlBuilder.append(String.format(SEARCH_SCROLL_TEMPLATE, size));
        return urlBuilder.toString();
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

    private String createSearchBody()
    {
        return String.format(SEARCH_BODY_TEMPLATE,
                m_TimeField, m_Search, m_TimeField, m_StartTime, m_EndTime,
                createAggregations());
    }

    private String createAggregations()
    {
        return (m_Aggregations != null) ? String.format(AGGREGATION_TEMPLATE, m_Aggregations) : "";
    }

    private InputStream continueScroll() throws IOException
    {
        // Aggregations never need a continuation
        if (m_Aggregations == null)
        {
            StringBuilder urlBuilder = newUrlBuilder();
            urlBuilder.append(CONTINUE_SCROLL_END_POINT);
            HttpGetResponse response = m_HttpGetRequester.get(urlBuilder.toString(), m_AuthHeader,
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
