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

package com.prelert.server.info.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.SystemUtils;
import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.indices.NodeIndicesStats;
import org.elasticsearch.monitor.fs.FsInfo;
import org.elasticsearch.monitor.fs.FsInfo.Path;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticsearch.monitor.os.OsInfo;
import org.elasticsearch.monitor.os.OsStats;
import org.elasticsearch.monitor.os.OsStats.Mem;
import org.elasticsearch.monitor.process.ProcessInfo;
import org.elasticsearch.monitor.process.ProcessStats;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.server.info.ServerInfo;
import com.prelert.utils.HostnameFinder;

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
        Map<String, String> attributes = new TreeMap<>();
        attributes.put("client", "true");
        attributes.put("data", "false");
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
        assertNotNull(info.getHostname());
        assertNull(info.getTotalDiskMb());
        assertNull(info.getTotalMemoryMb());
        assertNull(info.getAvailableDiskMb());
    }

    @Test
    public void testNodesStats() throws IOException
    {
        ElasticsearchServerInfo esi = Mockito.spy(new ElasticsearchServerInfo(m_Client));
        NodeInfo[] nodes = new NodeInfo[1];
        NodeInfo node = mock(NodeInfo.class);
        nodes[0] = node;
        DiscoveryNode discoveryNode = mock(DiscoveryNode.class);
        when(node.getNode()).thenReturn(discoveryNode);
        when(discoveryNode.isClientNode()).thenReturn(false);
        when(discoveryNode.isDataNode()).thenReturn(false);

        JvmInfo jvmInfo = JvmInfo.jvmInfo();
        StreamInput si = mock(StreamInput.class);
        when(si.readByte()).thenReturn((byte) 0x01);

        when(si.getVersion()).thenReturn(Version.V_0_90_0_Beta1);
        OsInfo osInfo = OsInfo.readOsInfo(si);
        when(node.getOs()).thenReturn(osInfo);
        when(node.getJvm()).thenReturn(jvmInfo);
        when(node.getProcess()).thenReturn(new ProcessInfo(12345678, false));

        NodeStats[] stats = new NodeStats[1];
        NodeStats stat = mock(NodeStats.class);
        stats[0] = stat;
        when(stat.getNode()).thenReturn(discoveryNode);
        when(stat.getHostname()).thenReturn("test_host");
        when(stat.getIndices()).thenReturn(mock(NodeIndicesStats.class));

        when(stat.getOs()).thenReturn(mock(OsStats.class));
        when(stat.getProcess()).thenReturn(mock(ProcessStats.class));
        when(stat.getJvm()).thenReturn(mock(org.elasticsearch.monitor.jvm.JvmStats.class));
        when(stat.getFs()).thenReturn(mock(FsInfo.class));

        Mockito.doReturn(nodes).when(esi).nodesInfo();
        Mockito.doReturn(stats).when(esi).nodesStats();
        String serverStats = esi.serverStats();
        assertTrue(serverStats.matches("(?s).*\"jvm\".*"));
        assertTrue(serverStats.matches("(?s).*12345678.*"));
        assertTrue(serverStats.matches("(?s).*test_host.*"));
    }


    @Test
    public void testServerInfo() throws IOException
    {
        ElasticsearchServerInfo esi = Mockito.spy(new ElasticsearchServerInfo(m_Client));
        DiscoveryNode discoveryNode = mock(DiscoveryNode.class);
        when(discoveryNode.isClientNode()).thenReturn(false);
        when(discoveryNode.isDataNode()).thenReturn(true);

        NodeStats[] stats = new NodeStats[1];
        NodeStats stat = mock(NodeStats.class);
        stats[0] = stat;
        when(stat.getNode()).thenReturn(discoveryNode);

        OsStats os = mock(OsStats.class);
        Mem mem = mock(Mem.class);
        when(os.getMem()).thenReturn(mem);
        when(mem.getTotal()).thenReturn(new ByteSizeValue(1357924680));
        Path p = mock(Path.class);
        when(p.getAvailable()).thenReturn(new ByteSizeValue(543210000));
        when(p.getTotal()).thenReturn(new ByteSizeValue(987650000));

        FsInfo fs = mock(FsInfo.class);
        when(fs.getTotal()).thenReturn(p);

        when(stat.getOs()).thenReturn(os);
        when(stat.getFs()).thenReturn(fs);

        Mockito.doReturn(stats).when(esi).nodesStats();

        ServerInfo si = esi.serverInfo();
        assertEquals(1295L, (long)si.getTotalMemoryMb());
        assertEquals(518l, (long)si.getAvailableDiskMb());
        assertEquals(941l, (long)si.getTotalDiskMb());

        assertEquals(HostnameFinder.findHostname(), si.getHostname());
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
