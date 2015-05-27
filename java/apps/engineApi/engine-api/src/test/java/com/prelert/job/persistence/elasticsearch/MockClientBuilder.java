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

package com.prelert.job.persistence.elasticsearch;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.indices.IndexMissingException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class MockClientBuilder
{
    @Mock private Client m_Client;

    @Mock private AdminClient m_AdminClient;
    @Mock private IndicesAdminClient m_IndicesAdminClient;
    @Mock private ActionFuture<IndicesExistsResponse> m_IndexNotExistsResponseFuture;

    public MockClientBuilder()
    {
        m_Client = mock(Client.class);
        m_AdminClient = mock(AdminClient.class);
        m_IndicesAdminClient = mock(IndicesAdminClient.class);

        when(m_Client.admin()).thenReturn(m_AdminClient);
        when(m_AdminClient.indices()).thenReturn(m_IndicesAdminClient);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MockClientBuilder addIndicesExistsResponse(String index, boolean exists) throws InterruptedException, ExecutionException
    {
        ActionFuture actionFuture = mock(ActionFuture.class);
        ArgumentCaptor<IndicesExistsRequest> requestCaptor = ArgumentCaptor.forClass(IndicesExistsRequest.class);

        when(m_IndicesAdminClient.exists(requestCaptor.capture())).thenReturn(actionFuture);
        doAnswer(invocation ->
        {
            IndicesExistsRequest request = (IndicesExistsRequest) invocation.getArguments()[0];
            return request.indices()[0].equals(index) ? actionFuture : null;
        }).when(m_IndicesAdminClient).exists(any(IndicesExistsRequest.class));
        when(actionFuture.get()).thenReturn(new IndicesExistsResponse(exists));
        return this;
    }

    public MockClientBuilder prepareGet(String index, String type, String id, GetResponse response)
    {
        GetRequestBuilder getRequestBuilder = mock(GetRequestBuilder.class);
        when(getRequestBuilder.get()).thenReturn(response);
        when(m_Client.prepareGet(index, type, id)).thenReturn(getRequestBuilder);
        return this;
    }

    public MockClientBuilder throwMissingIndexOnPrepareGet(String index, String type, String id)
    {
        doThrow(new IndexMissingException(null)).when(m_Client).prepareGet(index, type, id);
        return this;
    }

    public Client build()
    {
        return m_Client;
    }
}
