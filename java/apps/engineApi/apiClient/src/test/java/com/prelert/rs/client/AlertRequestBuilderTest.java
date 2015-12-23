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

public class AlertRequestBuilderTest
{
    private static final String BASE_URL = "http://myServer:8080";

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
        new AlertRequestBuilder(m_Client, "foo").get();
        verify(m_Client).get(eq(BASE_URL + AlertRequestBuilder.ENDPOINT + "foo"), any(), eq(true));
    }

    @Test
    public void testGet_GivenProb() throws IOException
    {
        new AlertRequestBuilder(m_Client, "foo").probability(0.1).get();
        verify(m_Client).get(eq(BASE_URL + AlertRequestBuilder.ENDPOINT + "foo?probability=0.1"),
                            any(), eq(true));
    }

    @Test
    public void testGet_GivenScore() throws IOException
    {
        new AlertRequestBuilder(m_Client, "foo").score(0.1).get();
        verify(m_Client).get(eq(BASE_URL + AlertRequestBuilder.ENDPOINT + "foo?score=0.1"),
                any(), eq(true));
    }

    @Test
    public void testGet_GivenTimeout() throws IOException
    {
        new AlertRequestBuilder(m_Client, "foo").timeout(100l).get();
        verify(m_Client).get(eq(BASE_URL + AlertRequestBuilder.ENDPOINT + "foo?timeout=100"),
                any(), eq(true));
    }

    @Test
    public void testGet_GivenIncludeInterim() throws IOException
    {
        new AlertRequestBuilder(m_Client, "foo").includeInterim().get();
        verify(m_Client).get(eq(BASE_URL + AlertRequestBuilder.ENDPOINT + "foo?includeInterim=true"),
                any(), eq(true));
    }

    @Test
    public void testGet_GivenAlertOnInfluencers() throws IOException
    {
        new AlertRequestBuilder(m_Client, "foo").alertOnInfluencers().get();
        verify(m_Client).get(eq(BASE_URL + AlertRequestBuilder.ENDPOINT + "foo?alertOn=influencer"),
                any(), eq(true));
    }

    @Test
    public void testGet_GivenAlertOnAll() throws IOException
    {
        new AlertRequestBuilder(m_Client, "foo")
                        .alertOnInfluencers()
                        .alertOnBuckets()
                        .alertOnBucketInfluencers()
                        .get();

        verify(m_Client).get(eq(BASE_URL + AlertRequestBuilder.ENDPOINT +
                "foo?alertOn=influencer,bucket,bucketinfluencer"), any(), eq(true));
    }

    @Test
    public void testGet_GivenSomeCombination() throws IOException
    {
        new AlertRequestBuilder(m_Client, "foo")
                        .alertOnInfluencers()
                        .timeout(50l)
                        .score(20.0)
                        .get();

        verify(m_Client).get(eq(BASE_URL + AlertRequestBuilder.ENDPOINT +
                "foo?score=20.0&alertOn=influencer&timeout=50"), any(), eq(true));
    }
}
