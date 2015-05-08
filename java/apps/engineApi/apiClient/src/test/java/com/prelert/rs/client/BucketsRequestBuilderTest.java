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

package com.prelert.rs.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BucketsRequestBuilderTest
{
    private static final String BASE_URL = "http://localhost:8080";

    @Mock private EngineApiClient m_Client;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(m_Client.getBaseUrl()).thenReturn(BASE_URL);
    }

    @Test
    public void testGet_GivenDefaultParameters() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets"), any());
    }

    @Test
    public void testGet_GivenExpand() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").expand(true).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets?expand=true"), any());
    }

    @Test
    public void testGet_GivenIncludeInterim() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").includeInterim(true).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets?includeInterim=true"), any());
    }

    @Test
    public void testGet_GivenAnomalyScoreThreshold() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").anomalyScoreThreshold(80.0).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets?anomalyScore=80.0"), any());
    }

    @Test
    public void testGet_GivenNormalizedProbabilityThreshold() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").normalizedProbabilityThreshold(65.0).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets?maxNormalizedProbability=65.0"), any());
    }

    @Test
    public void testGet_GivenSkip() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").skip(200).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets?skip=200"), any());
    }

    @Test
    public void testGet_GivenTake() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").take(1000).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets?take=1000"), any());
    }

    @Test
    public void testGet_GivenStartInEpoch() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").start(3600).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets?start=3600"), any());
    }

    @Test
    public void testGet_GivenStartInIso() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").start("2015-05-08T06:00:00+02:00").get();
        verify(m_Client).get(
                eq(BASE_URL + "/results/foo/buckets?start=2015-05-08T06%3A00%3A00%2B02%3A00"), any());
    }

    @Test
    public void testGet_GivenEndInEpoch() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").end(3600).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets?end=3600"), any());
    }

    @Test
    public void testGet_GivenEndInIso() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo").end("2015-05-08T06:00:00+02:00").get();
        verify(m_Client).get(
                eq(BASE_URL + "/results/foo/buckets?end=2015-05-08T06%3A00%3A00%2B02%3A00"), any());
    }

    @Test
    public void testGet_GivenMultipleParameters() throws IOException
    {
        new BucketsRequestBuilder(m_Client, "foo")
                .skip(0)
                .take(1000)
                .start(3600)
                .end(7200)
                .anomalyScoreThreshold(76.0)
                .normalizedProbabilityThreshold(40.0)
                .expand(true)
                .includeInterim(true)
                .get();
        verify(m_Client).get(eq(
                BASE_URL + "/results/foo/buckets?skip=0&take=1000&start=3600&end=7200&anomalyScore=76.0&maxNormalizedProbability=40.0&expand=true&includeInterim=true"),
                any());
    }
}
