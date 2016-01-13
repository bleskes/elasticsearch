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
    /**
     * The search body contains sorting based on the time field
     * and a query. The query is composed by a bool query with
     * two must clauses, the recommended way to perform an AND query.
     * There are 5 placeholders:
     * <ol>
     *   <li> time field
     *   <li> user defined query
     *   <li> time field
     *   <li> start time (epoch ms)
     *   <li> end time (epoch ms)
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
            + "                \"gte\": %s,"
            + "                \"lt\": %s"
            + "              }"
            + "            }"
            + "          }"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    private static final Pattern SCROLL_ID_PATTERN = Pattern.compile(".*\"_scroll_id\":\"(.*?)\".*");
    private static final Pattern EMPTY_HITS_PATTERN = Pattern.compile(".*\"hits\":\\[\\]");
    private static final int OK_STATUS = 200;
    private static final String SLASH = "/";
    private static final String COMMA = ",";
    private static final String SEARCH_SCROLL_END_POINT = "_search?scroll=60m&size=1000";
    private static final String CONTINUE_SCROLL_END_POINT = "_search/scroll?scroll=60m";

    /**
     * We want to read up until the "hits" array. 500 bytes (250 chars) should be enough
     */
    private static final int PUSHBACK_BUFFER_BYTES = 500;

    private final HttpGetRequester m_HttpGetRequester;
    private final String m_BaseUrl;
    private final List<String> m_Indices;
    private final List<String> m_Types;
    private final String m_Search;
    private final String m_TimeField;
    private String m_ScrollId;
    private boolean m_IsScrollComplete;
    private String m_StartEpochMs;
    private String m_EndEpochMs;
    private Logger m_Logger;

    ElasticsearchDataExtractor(HttpGetRequester httpGetRequester, String baseUrl,
            List<String> indices, List<String> types, String search, String timeField)
    {
        m_HttpGetRequester = Objects.requireNonNull(httpGetRequester);
        m_BaseUrl = Objects.requireNonNull(baseUrl);
        m_Indices = Objects.requireNonNull(indices);
        m_Types = Objects.requireNonNull(types);
        m_Search = Objects.requireNonNull(search);
        m_TimeField = Objects.requireNonNull(timeField);
    }

    public static ElasticsearchDataExtractor create(String baseUrl,
            List<String> indices, List<String> types, String search, String timeField)
    {
        return new ElasticsearchDataExtractor(new HttpGetRequester(), baseUrl, indices, types,
                search, timeField);
    }

    public void newSearch(String startEpochMs, String endEpochMs, Logger logger)
    {
        m_ScrollId = null;
        m_IsScrollComplete = false;
        m_StartEpochMs = startEpochMs;
        m_EndEpochMs = endEpochMs;
        m_Logger = logger;

        m_Logger.info("Requesting data from '" + m_BaseUrl + "' within [" + startEpochMs + ", "
                + endEpochMs + ")");
    }

    @Override
    public Optional<InputStream> next()
    {
        if (m_IsScrollComplete)
        {
            throw new NoSuchElementException();
        }
        try
        {
            PushbackInputStream stream = m_ScrollId == null ? initScroll() : continueScroll();
            m_IsScrollComplete = stream == null
                    || peekAndMatchInStream(stream, EMPTY_HITS_PATTERN).find();
            if (!m_IsScrollComplete)
            {
                return Optional.of(stream);
            }
        }
        catch (IOException e)
        {
            m_Logger.error("An error occurred during requesting data from: " + m_BaseUrl, e);
            m_IsScrollComplete = true;
        }
        return Optional.empty();
    }

    @Override
    public boolean hasNext()
    {
        return !m_IsScrollComplete;
    }

    private PushbackInputStream initScroll() throws IOException
    {
        String url = buildInitScrollUrl();
        HttpGetResponse response = m_HttpGetRequester.get(url, createSearchBody());
        if (response.getResponseCode() != OK_STATUS)
        {
            m_Logger.error("Request '" + url + "' failed with status code: "
                    + response.getResponseCode() + ". Response was:\n" + response.getResponseAsString());
            return null;
        }
        PushbackInputStream pushbackStream = new PushbackInputStream(response.getStream(),
                PUSHBACK_BUFFER_BYTES);
        Matcher matcher = peekAndMatchInStream(pushbackStream, SCROLL_ID_PATTERN);
        if (!matcher.find())
        {
            m_Logger.error("Field '_scroll_id' was expected but not found in response:\n"
                    + response.getResponseAsString());
            return null;
        }
        m_ScrollId = matcher.group(1);
        return pushbackStream;
    }

    private String buildInitScrollUrl()
    {
        StringBuilder urlBuilder = newUrlBuilder();
        urlBuilder.append(m_Indices.stream().collect(Collectors.joining(COMMA)));
        urlBuilder.append(SLASH);
        urlBuilder.append(m_Types.stream().collect(Collectors.joining(COMMA)));
        urlBuilder.append(SLASH);
        urlBuilder.append(SEARCH_SCROLL_END_POINT);
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
                m_TimeField, m_Search, m_TimeField, m_StartEpochMs, m_EndEpochMs);
    }

    private Matcher peekAndMatchInStream(PushbackInputStream stream, Pattern pattern)
            throws IOException
    {
        byte[] peek = new byte[PUSHBACK_BUFFER_BYTES];
        stream.read(peek);
        String peekString = new String(peek, StandardCharsets.UTF_8);
        Matcher matcher = pattern.matcher(peekString);
        stream.unread(peek);
        return matcher;
    }

    private PushbackInputStream continueScroll() throws IOException
    {
        StringBuilder urlBuilder = newUrlBuilder();
        urlBuilder.append(CONTINUE_SCROLL_END_POINT);
        HttpGetResponse response = m_HttpGetRequester.get(urlBuilder.toString(), m_ScrollId);
        if (response.getResponseCode() == OK_STATUS)
        {
            return new PushbackInputStream(response.getStream(), PUSHBACK_BUFFER_BYTES);
        }
        m_Logger.error("Request '"  + urlBuilder.toString() + "' with scroll id '" + m_ScrollId
                + "' failed with status code: " + response.getResponseCode() + ". Response was:\n"
                + response.getResponseAsString());
        return null;
    }
}
