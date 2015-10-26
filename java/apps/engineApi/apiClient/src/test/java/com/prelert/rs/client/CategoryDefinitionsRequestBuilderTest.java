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

public class CategoryDefinitionsRequestBuilderTest
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
        new CategoryDefinitionsRequestBuilder(m_Client, "foo").get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/categorydefinitions"), any());
    }

    @Test
    public void testGet_GivenSkip() throws IOException
    {
        new CategoryDefinitionsRequestBuilder(m_Client, "foo").skip(200).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/categorydefinitions?skip=200"), any());
    }

    @Test
    public void testGet_GivenTake() throws IOException
    {
        new CategoryDefinitionsRequestBuilder(m_Client, "foo").take(1000).get();
        verify(m_Client).get(eq(BASE_URL + "/results/foo/categorydefinitions?take=1000"), any());
    }

    @Test
    public void testGet_GivenMultipleParameters() throws IOException
    {
        new CategoryDefinitionsRequestBuilder(m_Client, "foo").skip(0).take(1000).get();
        verify(m_Client).get(
                eq(BASE_URL + "/results/foo/categorydefinitions?skip=0&take=1000"),
                any());
    }
}
