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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class MockClientBuilder
{
    @Mock private Client m_Client;

    @Mock private AdminClient m_AdminClient;
    @Mock private ClusterAdminClient m_ClusterAdminClient;
    @Mock private IndicesAdminClient m_IndicesAdminClient;
    @Mock private ActionFuture<IndicesExistsResponse> m_IndexNotExistsResponseFuture;

    public MockClientBuilder(String clusterName)
    {
        m_Client = mock(Client.class);
        m_AdminClient = mock(AdminClient.class);
        m_ClusterAdminClient = mock(ClusterAdminClient.class);
        m_IndicesAdminClient = mock(IndicesAdminClient.class);

        when(m_Client.admin()).thenReturn(m_AdminClient);
        when(m_AdminClient.cluster()).thenReturn(m_ClusterAdminClient);
        when(m_AdminClient.indices()).thenReturn(m_IndicesAdminClient);
        Settings settings = Settings.builder().put("cluster.name", clusterName).build();
        when(m_Client.settings()).thenReturn(settings);
    }

    public MockClientBuilder addClusterStatusYellowResponse(String index, TimeValue timeout) throws InterruptedException, ExecutionException
    {
        ClusterHealthRequestBuilder clusterHealthRequestBuilder = mock(ClusterHealthRequestBuilder.class);
        when(m_ClusterAdminClient.prepareHealth(index)).thenReturn(clusterHealthRequestBuilder);
        when(clusterHealthRequestBuilder.get(timeout)).thenReturn(mock(ClusterHealthResponse.class));
        return this;
    }

    @SuppressWarnings({ "unchecked" })
    public MockClientBuilder addClusterStatusYellowResponse() throws InterruptedException, ExecutionException
    {
        ListenableActionFuture<ClusterHealthResponse> actionFuture = mock(ListenableActionFuture.class);
        ClusterHealthRequestBuilder clusterHealthRequestBuilder = mock(ClusterHealthRequestBuilder.class);

        when(m_ClusterAdminClient.prepareHealth()).thenReturn(clusterHealthRequestBuilder);
        when(clusterHealthRequestBuilder.setWaitForYellowStatus()).thenReturn(clusterHealthRequestBuilder);
        when(clusterHealthRequestBuilder.execute()).thenReturn(actionFuture);
        when(actionFuture.actionGet()).thenReturn(mock(ClusterHealthResponse.class));
        return this;
    }

    @SuppressWarnings({ "unchecked" })
    public MockClientBuilder addClusterStatusYellowResponse(String index) throws InterruptedException, ExecutionException
    {
        ListenableActionFuture<ClusterHealthResponse> actionFuture = mock(ListenableActionFuture.class);
        ClusterHealthRequestBuilder clusterHealthRequestBuilder = mock(ClusterHealthRequestBuilder.class);

        when(m_ClusterAdminClient.prepareHealth(index)).thenReturn(clusterHealthRequestBuilder);
        when(clusterHealthRequestBuilder.setWaitForYellowStatus()).thenReturn(clusterHealthRequestBuilder);
        when(clusterHealthRequestBuilder.execute()).thenReturn(actionFuture);
        when(actionFuture.actionGet()).thenReturn(mock(ClusterHealthResponse.class));
        return this;
    }

    @SuppressWarnings({ "unchecked" })
    public MockClientBuilder addClusterStatusRedResponse() throws InterruptedException, ExecutionException
    {
        ListenableActionFuture<ClusterHealthResponse> actionFuture = mock(ListenableActionFuture.class);
        ClusterHealthRequestBuilder clusterHealthRequestBuilder = mock(ClusterHealthRequestBuilder.class);

        when(m_ClusterAdminClient.prepareHealth()).thenReturn(clusterHealthRequestBuilder);
        when(clusterHealthRequestBuilder.setWaitForYellowStatus()).thenReturn(clusterHealthRequestBuilder);
        when(clusterHealthRequestBuilder.execute()).thenReturn(actionFuture);
        ClusterHealthResponse response = mock(ClusterHealthResponse.class);
        when(response.getStatus()).thenReturn(ClusterHealthStatus.RED);
        when(actionFuture.actionGet()).thenReturn(response);
        return this;
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
        when(actionFuture.actionGet()).thenReturn(new IndicesExistsResponse(exists));
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MockClientBuilder addIndicesExistsResponse2(String index, boolean exists) throws InterruptedException, ExecutionException
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
        when(getRequestBuilder.setFetchSource(false)).thenReturn(getRequestBuilder);
        when(getRequestBuilder.setFields()).thenReturn(getRequestBuilder);
        when(m_Client.prepareGet(index, type, id)).thenReturn(getRequestBuilder);
        return this;
    }

    public MockClientBuilder prepareCreate(String index)
    {
        CreateIndexRequestBuilder createIndexRequestBuilder = mock(CreateIndexRequestBuilder.class);
        CreateIndexResponse response = mock(CreateIndexResponse.class);
        when(createIndexRequestBuilder.setSettings(any(Settings.Builder.class))).thenReturn(createIndexRequestBuilder);
        when(createIndexRequestBuilder.addMapping(any(String.class), any(XContentBuilder.class))).thenReturn(createIndexRequestBuilder);
        when(createIndexRequestBuilder.get()).thenReturn(response);
        when(m_IndicesAdminClient.prepareCreate(eq(index))).thenReturn(createIndexRequestBuilder);
        return this;
    }

    @SuppressWarnings("unchecked")
    public MockClientBuilder prepareIndex(String index, String source)
    {
        IndexRequestBuilder builder = mock(IndexRequestBuilder.class);
        ListenableActionFuture<IndexResponse> actionFuture = mock(ListenableActionFuture.class);

        when(m_Client.prepareIndex(eq(index), any(), any())).thenReturn(builder);
        when(builder.setSource(eq(source))).thenReturn(builder);
        when(builder.execute()).thenReturn(actionFuture);
        when(actionFuture.actionGet()).thenReturn(mock(IndexResponse.class));
        return this;
    }

    public MockClientBuilder throwMissingIndexOnPrepareGet(String index, String type, String id)
    {
        doThrow(new IndexNotFoundException(index)).when(m_Client).prepareGet(index, type, id);
        return this;
    }

    public Client build()
    {
        return m_Client;
    }

    public void verifyIndexCreated(String index)
    {
        verify(m_IndicesAdminClient).prepareCreate(eq(index));
    }

    public void resetIndices()
    {
        reset(m_IndicesAdminClient);
    }

}
