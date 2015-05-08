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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public class HttpGetRequesterTest
{
    private static final TypeReference<Pagination<String>> PAGINATION_REFERENCE =
            new TypeReference<Pagination<String>>() {};
    private static final TypeReference<SingleDocument<String>> SINGLE_DOC_REFERENCE =
            new TypeReference<SingleDocument<String>>() {};

    @Mock private EngineApiClient m_Client;
    private HttpGetRequester<String> m_Requester;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_Requester = new HttpGetRequester<>(m_Client);
    }

    @Test
    public void testGetPage_GivenClientReturnsNull() throws IOException
    {
        when(m_Client.get("foo", PAGINATION_REFERENCE)).thenReturn(null);

        Pagination<String> page = m_Requester.getPage("foo", PAGINATION_REFERENCE);

        assertTrue(page.getDocuments().isEmpty());
    }

    @Test
    public void testGetPage_GivenClientReturnsPage() throws IOException
    {
        Pagination<String> page = new Pagination<String>();
        when(m_Client.get("foo", PAGINATION_REFERENCE)).thenReturn(page);

        assertEquals(page, m_Requester.getPage("foo", PAGINATION_REFERENCE));
    }

    @Test
    public void testGetSingleDocument_GivenClientReturnsNull() throws IOException
    {
        when(m_Client.get("foo", SINGLE_DOC_REFERENCE)).thenReturn(null);

        SingleDocument<String> doc = m_Requester.getSingleDocument("foo", SINGLE_DOC_REFERENCE);

        assertFalse(doc.isExists());
    }

    @Test
    public void testGetSingleDocument_GivenClientReturnsDocument() throws IOException
    {
        SingleDocument<String> doc = new SingleDocument<String>();
        when(m_Client.get("foo", SINGLE_DOC_REFERENCE)).thenReturn(doc);

        assertEquals(doc, m_Requester.getSingleDocument("foo", SINGLE_DOC_REFERENCE));
    }
}
