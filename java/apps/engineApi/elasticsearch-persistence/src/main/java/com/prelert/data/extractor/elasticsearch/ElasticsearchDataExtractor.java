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
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
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

import com.google.common.annotations.VisibleForTesting;
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

    static final Pattern SCROLL_ID_PATTERN = Pattern.compile("\"_scroll_id\":\"(.*?)\"");
    private static final Pattern EMPTY_HITS_PATTERN = Pattern.compile("\"hits\":\\[\\]");
    private static final Pattern EMPTY_AGGREGATIONS_PATTERN = Pattern.compile("\"aggregations\":.*?\"buckets\":\\[\\]");
    private static final int OK_STATUS = 200;
    private static final String SLASH = "/";
    private static final String COMMA = ",";
    private static final String SEARCH_SCROLL_TEMPLATE = "_search?scroll=60m&size=%d";
    private static final int UNAGGREGATED_SCROLL_SIZE = 1000;
    private static final String CONTINUE_SCROLL_END_POINT = "_search/scroll?scroll=60m";

    /**
     * We want to read up until the "hits" array.  32KB (~= 32000 UTF-8
     * chars given that field names and scroll ID are ASCII) should be enough.
     * The longest reported scroll ID is 20708 characters - see
     * http://elasticsearch-users.115913.n3.nabble.com/Ridiculously-long-Scroll-id-td4038567.html
     */
    private static final int PUSHBACK_BUFFER_BYTES = 32768;

    private final HttpGetRequester m_HttpGetRequester;
    private final String m_BaseUrl;
    private final String m_AuthHeader;
    private final List<String> m_Indices;
    private final List<String> m_Types;
    private final String m_Search;
    private final String m_Aggregations;
    private final String m_TimeField;
    private volatile String m_ScrollId;
    private volatile boolean m_IsScrollComplete;
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
        m_ScrollId = null;
        m_IsScrollComplete = false;
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
        if (m_IsScrollComplete)
        {
            throw new NoSuchElementException();
        }
        try
        {
            PushbackInputStream stream = (m_ScrollId == null) ? initScroll() : continueScroll();
            updateScrollId(stream);
            updateIsScrollComplete(stream);
            return m_IsScrollComplete ? Optional.empty() : Optional.of(stream);
        }
        catch (IOException e)
        {
            m_Logger.error("An error occurred during requesting data from: " + m_BaseUrl, e);
            m_IsScrollComplete = true;
            throw e;
        }
    }

    @Override
    public boolean hasNext()
    {
        return !m_IsScrollComplete;
    }

    private PushbackInputStream initScroll() throws IOException
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
        return new PushbackInputStream(response.getStream(), PUSHBACK_BUFFER_BYTES);
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

    private void updateScrollId(PushbackInputStream stream) throws IOException
    {
        if (stream == null)
        {
            return;
        }

        Matcher matcher = peekAndMatchInStream(stream, SCROLL_ID_PATTERN);
        if (!matcher.find())
        {
            throw new IOException("Field '_scroll_id' was expected but not found in response:\n"
                    + HttpGetResponse.getStreamAsString(stream));
        }
        m_ScrollId = matcher.group(1);
    }

    private void updateIsScrollComplete(PushbackInputStream stream) throws IOException
    {
        Pattern emptyPattern = (m_Aggregations == null) ? EMPTY_HITS_PATTERN : EMPTY_AGGREGATIONS_PATTERN;
        m_IsScrollComplete = (stream == null) || peekAndMatchInStream(stream, emptyPattern).find();
    }

    @VisibleForTesting
    static Matcher peekAndMatchInStream(PushbackInputStream stream, Pattern pattern)
            throws IOException
    {
        byte[] peek = new byte[PUSHBACK_BUFFER_BYTES];
        int bytesRead = readUntilEndOrLimit(stream, peek);

        // We make the assumption here that invalid byte sequences will be read as invalid char
        // rather than throwing an exception
        String peekString = new String(peek, 0, bytesRead, StandardCharsets.UTF_8);

        Matcher matcher = pattern.matcher(peekString);
        stream.unread(peek, 0, bytesRead);
        return matcher;
    }

    /**
     * Reads from a stream until it has read at least {@link #PUSHBACK_BUFFER_BYTES}
     * or the stream ends.
     *
     * @param stream the stream
     * @param buffer the buffer where the stream is read into
     * @return the number of total bytes read
     * @throws IOException
     */
    private static int readUntilEndOrLimit(PushbackInputStream stream, byte[] buffer)
            throws IOException
    {
        int totalBytesRead = 0;
        int bytesRead = 0;

        while (bytesRead >= 0 && totalBytesRead < PUSHBACK_BUFFER_BYTES)
        {
            bytesRead = stream.read(buffer, totalBytesRead, PUSHBACK_BUFFER_BYTES - totalBytesRead);
            if (bytesRead > 0)
            {
                totalBytesRead += bytesRead;
            }
        }

        return totalBytesRead;
    }

    private PushbackInputStream continueScroll() throws IOException
    {
        // Aggregations never need a continuation
        if (m_Aggregations == null)
        {
            StringBuilder urlBuilder = newUrlBuilder();
            urlBuilder.append(CONTINUE_SCROLL_END_POINT);
            HttpGetResponse response = m_HttpGetRequester.get(urlBuilder.toString(), m_AuthHeader, m_ScrollId);
            if (response.getResponseCode() == OK_STATUS)
            {
                return new PushbackInputStream(response.getStream(), PUSHBACK_BUFFER_BYTES);
            }
            throw new IOException("Request '"  + urlBuilder.toString() + "' with scroll id '" + m_ScrollId
                    + "' failed with status code: " + response.getResponseCode() + ". Response was:\n"
                    + response.getResponseAsString());
        }
        return null;
    }
}
