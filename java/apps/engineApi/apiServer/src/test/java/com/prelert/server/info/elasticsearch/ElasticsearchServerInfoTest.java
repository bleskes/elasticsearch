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

package com.prelert.server.info.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.SystemUtils;
import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.TransportAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.prelert.server.info.ServerInfo;



/**
 * These tests rely on extensive mocking of the elasticsearch client.
 * The tests are of dubious value as there isn't a public constructor for
 * org.elasticsearch.monitor.os.OsInfo meaning the actual data objects
 * cannot be mocked.
 */
public class ElasticsearchServerInfoTest
{
    private List<NodeInfo> m_NodeInfo;
    private List<NodeStats> m_NodeStats;
    @Mock private Client m_Client;

    @Before
    public void setUp()
    {
        m_NodeInfo = new ArrayList<>();
        m_NodeStats = new ArrayList<>();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testClientNodeReturnsNoServerInfo() throws InterruptedException, ExecutionException
    {
        ImmutableMap<String, String> attributes = ImmutableSortedMap.<String, String>naturalOrder()
                                                                    .put("client", "true")
                                                                    .put("data", "false")
                                                                    .build();
        TransportAddress transportAddress = mock(TransportAddress.class);
        when(transportAddress.getHost()).thenReturn("localhost");
        when(transportAddress.getAddress()).thenReturn("127.0.0.1");

        DiscoveryNode clientNode = new DiscoveryNode("nname", "nodeId", transportAddress,
                                        attributes, mock(Version.class));

        m_NodeStats.add(new NodeStats(clientNode, 0, null, null, null, null, null, null, null,
                null, null, null));

        m_NodeInfo.add(new NodeInfo(mock(Version.class), mock(Build.class), clientNode, null, null,
                null, null, null, null, null, null, null));

        ElasticsearchServerInfo esi = new TestServerInfo(m_Client);
        ServerInfo info = esi.serverInfo();

        assertEquals(SystemUtils.OS_NAME, info.getOsName());
        assertEquals(SystemUtils.OS_VERSION, info.getOsVersion());
        assertNull(info.getHostname());
        assertNull(info.getTotalDiskMb());
        assertNull(info.getTotalMemoryMb());
        assertNull(info.getAvailableDiskMb());

    }

    private class TestServerInfo extends ElasticsearchServerInfo
    {
        public TestServerInfo(Client client)
        {
            super(client);
        }

        @Override
        protected NodeInfo[] nodesInfo()
        {
            return m_NodeInfo.toArray(new NodeInfo[m_NodeInfo.size()]);
        }

        @Override
        protected NodeStats[] nodesStats()
        {
            return m_NodeStats.toArray(new NodeStats[m_NodeStats.size()]);
        }
    }
}
