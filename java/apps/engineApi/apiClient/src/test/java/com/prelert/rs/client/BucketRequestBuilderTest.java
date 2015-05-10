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

public class BucketRequestBuilderTest
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
        new BucketRequestBuilder(m_Client, "foo", "bar").get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets/bar"), any());
    }

    @Test
    public void testGet_GivenExpand() throws IOException
    {
        new BucketRequestBuilder(m_Client, "foo", "bar").expand(true).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets/bar?expand=true"), any());
    }

    @Test
    public void testGet_GivenIncludeInterim() throws IOException
    {
        new BucketRequestBuilder(m_Client, "foo", "noot").includeInterim(true).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/buckets/noot?includeInterim=true"), any());
    }

    @Test
    public void testGet_GivenMultipleParameters() throws IOException
    {
        new BucketRequestBuilder(m_Client, "foo", "3600")
                .expand(true)
                .includeInterim(true)
                .get();
        verify(m_Client).get(
                eq(BASE_URL + "/results/foo/buckets/3600?expand=true&includeInterim=true"),
                any());
    }
}
