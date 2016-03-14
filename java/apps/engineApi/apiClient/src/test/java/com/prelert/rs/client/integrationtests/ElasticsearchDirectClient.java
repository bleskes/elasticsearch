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

package com.prelert.rs.client.integrationtests;

import java.io.Closeable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class implements an ES client that enables direct calls
 * to ES. To be used only to allow integration tests to verify
 * results that are not served via the EngineApiClient.
 */
class ElasticsearchDirectClient implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchDirectClient.class);

    private final String m_BaseUrl;
    private final HttpClient m_HttpClient;

    public ElasticsearchDirectClient(String baseUrl)
    {
        m_BaseUrl = baseUrl;
        m_HttpClient = new HttpClient();
        try
        {
            m_HttpClient.start();
        } catch (Exception e)
        {
            LOGGER.error("Failed to start HTTP client", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close()
    {
        try
        {
            m_HttpClient.stop();
        } catch (Exception e)
        {
            LOGGER.error("Failed to stop the HTTP client", e);
        }
    }

    public double getBucketInitialScore(Date timestamp) throws IOException
    {
        return getBucketField(timestamp, "initialAnomalyScore", Double.class);
    }

    private <T> T getBucketField(Date timestamp, String field, Class<T> fieldType)
            throws IOException
    {
        String url = m_BaseUrl + "bucket/_search";
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String isoTime = df.format(timestamp);
        String query = "{\"query\": {\"match\": {\"@timestamp\":\"" + isoTime + "\"}}}";
        return getField(url, query, field, fieldType);
    }

    private <T> T getField(String url, String query, String field, Class<T> fieldType) throws IOException
    {
        ContentResponse response = null;
        try
        {
            response = m_HttpClient.newRequest(url)
                    .method(HttpMethod.GET)
                    .content(new StringContentProvider(query), "text/json")
                    .send();
        } catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            LOGGER.error("An error occurred while executing an HTTP GET to " + url, e);
            return null;
        }
        Map<String, Object> map = parseJsonHitAsMap(response.getContentAsString());
        return fieldType.cast(map.get(field));
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> parseJsonHitAsMap(String json) throws IOException
    {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        TypeReference<HashMap<String, Object>> typeRefHash =
                new TypeReference<HashMap<String, Object>>() {};

        Map<String, Object> map = mapper.readValue(json, typeRefHash);
        map = (Map<String, Object>)map.get("hits");
        ArrayList<Object> list = (ArrayList<Object>)map.get("hits");
        map = (Map<String, Object>)list.get(0);
        return (Map<String, Object>) map.get("_source");
    }
}
