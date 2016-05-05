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

package com.prelert.job.persistence.elasticsearch;

import static org.junit.Assert.assertEquals;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.audit.AuditActivity;
import com.prelert.job.audit.AuditMessage;
import com.prelert.job.audit.Level;

public class ElasticsearchAuditorTest
{
    @Mock private Client m_Client;
    @Mock private ListenableActionFuture<IndexResponse> m_IndexResponse;
    @Captor private ArgumentCaptor<String> m_IndexCaptor;
    @Captor private ArgumentCaptor<XContentBuilder> m_JsonCaptor;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInfo()
    {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(m_Client, "prelert-int", "foo");

        auditor.info("Here is my info");

        assertEquals("prelert-int", m_IndexCaptor.getValue());
        AuditMessage auditMessage = parseAuditMessage();
        assertEquals("foo", auditMessage.getJobId());
        assertEquals("Here is my info", auditMessage.getMessage());
        assertEquals(Level.INFO, auditMessage.getLevel());
    }

    @Test
    public void testWarning()
    {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(m_Client, "someIndex", "bar");

        auditor.warning("Here is my warning");

        assertEquals("someIndex", m_IndexCaptor.getValue());
        AuditMessage auditMessage = parseAuditMessage();
        assertEquals("bar", auditMessage.getJobId());
        assertEquals("Here is my warning", auditMessage.getMessage());
        assertEquals(Level.WARNING, auditMessage.getLevel());
    }

    @Test
    public void testError()
    {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(m_Client, "someIndex", "foobar");

        auditor.error("Here is my error");

        assertEquals("someIndex", m_IndexCaptor.getValue());
        AuditMessage auditMessage = parseAuditMessage();
        assertEquals("foobar", auditMessage.getJobId());
        assertEquals("Here is my error", auditMessage.getMessage());
        assertEquals(Level.ERROR, auditMessage.getLevel());
    }

    @Test
    public void testActivity_GivenString()
    {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(m_Client, "someIndex", "");

        auditor.activity("Here is my activity");

        assertEquals("someIndex", m_IndexCaptor.getValue());
        AuditMessage auditMessage = parseAuditMessage();
        assertEquals("", auditMessage.getJobId());
        assertEquals("Here is my activity", auditMessage.getMessage());
        assertEquals(Level.ACTIVITY, auditMessage.getLevel());
    }

    @Test
    public void testActivity_GivenNumbers()
    {
        givenClientPersistsSuccessfully();
        ElasticsearchAuditor auditor = new ElasticsearchAuditor(m_Client, "someIndex", "");

        auditor.activity(10, 100, 5, 50);

        assertEquals("someIndex", m_IndexCaptor.getValue());
        AuditActivity auditActivity = parseAuditActivity();
        assertEquals(10, auditActivity.getTotalJobs());
        assertEquals(100, auditActivity.getTotalDetectors());
        assertEquals(5, auditActivity.getRunningJobs());
        assertEquals(50, auditActivity.getRunningDetectors());
    }

    @Test
    public void testError_GivenNoSuchIndex()
    {
        when(m_Client.prepareIndex("someIndex", "auditMessage"))
                .thenThrow(new IndexNotFoundException("someIndex"));

        ElasticsearchAuditor auditor = new ElasticsearchAuditor(m_Client, "someIndex", "foobar");

        auditor.error("Here is my error");
    }

    private void givenClientPersistsSuccessfully()
    {
        IndexRequestBuilder indexRequestBuilder = mock(IndexRequestBuilder.class);
        when(indexRequestBuilder.setSource(m_JsonCaptor.capture())).thenReturn(indexRequestBuilder);
        when(indexRequestBuilder.execute()).thenReturn(m_IndexResponse);
        when(m_Client.prepareIndex(m_IndexCaptor.capture(), eq("auditMessage")))
                .thenReturn(indexRequestBuilder);
        when(m_Client.prepareIndex(m_IndexCaptor.capture(), eq("auditActivity")))
                .thenReturn(indexRequestBuilder);
    }

    private AuditMessage parseAuditMessage()
    {
        try
        {
            String json = m_JsonCaptor.getValue().string();
            json = json.replace("@timestamp", "timestamp");
            return new ObjectMapper().readValue(json, AuditMessage.class);
        }
        catch (IOException e)
        {
            return new AuditMessage();
        }
    }

    private AuditActivity parseAuditActivity()
    {
        try
        {
            String json = m_JsonCaptor.getValue().string();
            json = json.replace("@timestamp", "timestamp");
            return new ObjectMapper().readValue(json, AuditActivity.class);
        }
        catch (IOException e)
        {
            return new AuditActivity();
        }
    }
}
