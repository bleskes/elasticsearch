/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class implements an ES client that enables direct calls
 * to ES. To be used only to allow integration tests to verify
 * results that are not served vie the EngineApiClient.
 */
class ElasticsearchDirectClient
{
    private final String m_BaseUrl;
    private final CloseableHttpClient m_HttpClient;

    public ElasticsearchDirectClient(String baseUrl)
    {
        m_BaseUrl = baseUrl;
        m_HttpClient = HttpClients.createDefault();
    }

    public double getBucketInitialScore(String bucketId) throws ClientProtocolException, IOException
    {
        return getBucketField(bucketId, "initialAnomalyScore", Double.class);
    }

    private <T> T getBucketField(String bucketId, String field, Class<T> fieldType)
            throws ClientProtocolException, IOException
    {
        String url = m_BaseUrl + "bucket/" + bucketId;
        return getField(url, field, fieldType);
    }

    private <T> T getField(String url, String field, Class<T> fieldType)
            throws ClientProtocolException, IOException
    {
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response = m_HttpClient.execute(httpGet))
        {
            HttpEntity entity = response.getEntity();
            String jsonContent = convertOneLineInputStreamToString(entity.getContent());
            Map<String, Object> map = parseJsonAsMap(jsonContent);
            return fieldType.cast(map.get(field));
        }
    }

    private String convertOneLineInputStreamToString(InputStream stream) throws IOException
    {
        try (InputStreamReader i = new InputStreamReader(stream))
        {
            BufferedReader str = new BufferedReader(i);
            return str.readLine();
        }
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
