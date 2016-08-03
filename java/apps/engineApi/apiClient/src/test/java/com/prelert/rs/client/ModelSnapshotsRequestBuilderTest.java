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

public class ModelSnapshotsRequestBuilderTest
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
    public void testGet_GivenNoOptionalParameters() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo"), any());
    }

    @Test
    public void testGet_GivenSkipAndTake() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").skip(42L).take(420L).get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo?skip=42&take=420"), any());
    }

    @Test
    public void testGet_GivenEpochStartAndEnd() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").start(4200L).end(8400L).get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo?start=4200&end=8400"), any());
    }

    @Test
    public void testGet_GivenStringStartAndEnd() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo")
                .start("2016-01-01T00:00:00Z").end("2016-02-01T00:00:00Z").get();
        verify(m_Client).get(
                eq(BASE_URL + "/modelsnapshots/foo?start=2016-01-01T00%3A00%3A00Z&end=2016-02-01T00%3A00%3A00Z"),
                any());
    }

    @Test
    public void testGet_GivenSortAndOrder() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").sortField("timestamp").descending(false).get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo?sort=timestamp&desc=false"), any());
    }

    @Test
    public void testGet_GivenDescription() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").description("foo").get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo?description=foo"), any());
    }
}
