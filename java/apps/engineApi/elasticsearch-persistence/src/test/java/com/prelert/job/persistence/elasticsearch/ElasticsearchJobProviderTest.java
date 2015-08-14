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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.quantiles.Quantiles;

public class ElasticsearchJobProviderTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private Node m_Node;

    @Before
    public void setUp() throws InterruptedException, ExecutionException
    {
        MockitoAnnotations.initMocks(this);
        Settings settings = mock(Settings.class);
        when(settings.get("cluster.name")).thenReturn("nodeName");
        when(m_Node.settings()).thenReturn(settings);

    }

    @Test
    public void testClose() throws InterruptedException, ExecutionException, IOException
    {
        MockClientBuilder clientBuilder = new MockClientBuilder()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);
        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        provider.close();

        verify(m_Node).close();
    }

    @Test
    public void testGetQuantiles_GivenNoIndexForJob() throws InterruptedException,
            ExecutionException, UnknownJobException
    {
        m_ExpectedException.expect(UnknownJobException.class);
        m_ExpectedException.expectMessage("Cannot read persisted quantiles");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.MISSING_JOB_ERROR));

        MockClientBuilder clientBuilder = new MockClientBuilder()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .throwMissingIndexOnPrepareGet("foo", Quantiles.TYPE, Quantiles.QUANTILES_ID);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        provider.getQuantiles("foo");
    }

    @Test
    public void testGetQuantiles_GivenNoQuantilesForJob() throws InterruptedException,
            ExecutionException, UnknownJobException
    {
        GetResponse getResponse = createGetResponse(false, null);

        MockClientBuilder clientBuilder = new MockClientBuilder()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("foo", Quantiles.TYPE, Quantiles.QUANTILES_ID, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Quantiles quantiles = provider.getQuantiles("foo");

        assertEquals("", quantiles.getState());
    }

    @Test
    public void testGetQuantiles_GivenQuantilesHaveDifferentVersion() throws InterruptedException,
            ExecutionException, UnknownJobException
    {
        Map<String, Object> source = new HashMap<>();
        source.put(Quantiles.VERSION, "-1");
        source.put(Quantiles.QUANTILE_STATE, "state");
        GetResponse getResponse = createGetResponse(true, source);

        MockClientBuilder clientBuilder = new MockClientBuilder()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("foo", Quantiles.TYPE, Quantiles.QUANTILES_ID, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Quantiles quantiles = provider.getQuantiles("foo");

        assertEquals("", quantiles.getState());
    }

    @Test
    public void testGetQuantiles_GivenQuantilesHaveCurrentVersionAndNonEmptyState()
            throws InterruptedException, ExecutionException, UnknownJobException
    {
        Map<String, Object> source = new HashMap<>();
        source.put(Quantiles.VERSION, Quantiles.CURRENT_VERSION);
        source.put(Quantiles.QUANTILE_STATE, "state");
        GetResponse getResponse = createGetResponse(true, source);

        MockClientBuilder clientBuilder = new MockClientBuilder()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("foo", Quantiles.TYPE, Quantiles.QUANTILES_ID, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Quantiles quantiles = provider.getQuantiles("foo");

        assertEquals("state", quantiles.getState());
    }

    @Test
    public void testGetQuantiles_GivenQuantilesHaveCurrentVersionAndEmptyState()
            throws InterruptedException, ExecutionException, UnknownJobException
    {
        Map<String, Object> source = new HashMap<>();
        source.put(Quantiles.VERSION, Quantiles.CURRENT_VERSION);
        GetResponse getResponse = createGetResponse(true, source);

        MockClientBuilder clientBuilder = new MockClientBuilder()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("foo", Quantiles.TYPE, Quantiles.QUANTILES_ID, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Quantiles quantiles = provider.getQuantiles("foo");

        assertEquals("", quantiles.getState());
    }

    private ElasticsearchJobProvider createProvider(Client client)
    {
        return new ElasticsearchJobProvider(m_Node, client);
    }

    private static GetResponse createGetResponse(boolean exists, Map<String, Object> source)
    {
        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(exists);
        when(getResponse.getSource()).thenReturn(source);
        return getResponse;
    }
}
