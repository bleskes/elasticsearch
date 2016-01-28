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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class implements an ES client that enables direct calls
 * to ES. To be used only to allow integration tests to verify
 * results that are not served vie the EngineApiClient.
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

    public double getBucketInitialScore(String bucketId) throws IOException
    {
        return getBucketField(bucketId, "initialAnomalyScore", Double.class);
    }

    private <T> T getBucketField(String bucketId, String field, Class<T> fieldType)
            throws IOException
    {
        String url = m_BaseUrl + "bucket/" + bucketId;
        return getField(url, field, fieldType);
    }

    private <T> T getField(String url, String field, Class<T> fieldType) throws IOException
    {
        ContentResponse response = null;
        try
        {
            response = m_HttpClient.GET(url);
        } catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            LOGGER.error("An error occurred while executing an HTTP GET to " + url, e);
            return null;
        }
        Map<String, Object> map = parseJsonAsMap(response.getContentAsString());
        return fieldType.cast(map.get(field));
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> parseJsonAsMap(String json) throws IOException
    {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        TypeReference<HashMap<String,Object>> typeRef
                = new TypeReference<HashMap<String,Object>>() {};

        Map<String,Object> map = mapper.readValue(json, typeRef);
        return (Map<String, Object>) map.get("_source");
    }
}
