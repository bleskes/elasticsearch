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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.SystemUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.TransportAddress;
import org.junit.Test;
import org.mockito.Mockito;
import org.elasticsearch.Build;
import org.elasticsearch.Version;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.prelert.server.info.CpuInfo;
import com.prelert.server.info.ServerInfo;


/**
 * These tests rely on extensive mocking of the elasticsearch client.
 * The tests are of dubious value as there isn't a public constructor for
 * org.elasticsearch.monitor.os.OsInfo meaning the actual data objects
 * cannot be mocked.
 */
public class ElasticsearchServerInfoTest
{
    @Test
    public void testClientNodeReturnsNoCpuInfo()
    throws InterruptedException, ExecutionException
    {
        ImmutableMap<String, String> attributes = ImmutableSortedMap.<String, String>naturalOrder()
                                                                    .put("client", "true")
                                                                    .put("data", "false")
                                                                    .build();

        DiscoveryNode clientNode = new DiscoveryNode("nname", "nodeId", mock(TransportAddress.class),
                                        attributes, mock(Version.class));




        NodeInfo nodeInfo = new NodeInfo(mock(Version.class),
                                        mock(Build.class),
                                        clientNode,
                                        null, null, null, null, null, null, null, null,
                                        null, null);

        NodesInfoResponse response = new NodesInfoResponse(mock(ClusterName.class),
                                                            new NodeInfo[] {nodeInfo});



        Client client = mock(Client.class);
        AdminClient ac = mock(AdminClient.class);
        ClusterAdminClient cac = mock(ClusterAdminClient.class);
        @SuppressWarnings("unchecked")
        ActionFuture<NodesInfoResponse> af = mock(ActionFuture.class);

        when(client.admin()).thenReturn(ac);
        when(ac.cluster()).thenReturn(cac);
        when(cac.nodesInfo(Mockito.any())).thenReturn(af);
        when(af.get()).thenReturn(response);

        ElasticsearchServerInfo esi = new ElasticsearchServerInfo(client);
        CpuInfo cpuInfo = esi.cpuInfo();

        assertNull(cpuInfo.getCores());
        assertNull(cpuInfo.getFrequencyMHz());
        assertNull(cpuInfo.getModel());
        assertNull(cpuInfo.getVendor());
    }


    @Test
    public void testClientNodeReturnsNoServerInfo()
    throws InterruptedException, ExecutionException
    {
        ImmutableMap<String, String> attributes = ImmutableSortedMap.<String, String>naturalOrder()
                                                                    .put("client", "true")
                                                                    .put("data", "false")
                                                                    .build();

        DiscoveryNode clientNode = new DiscoveryNode("nname", "nodeId", mock(TransportAddress.class),
                                        attributes, mock(Version.class));




        NodeStats nodeStat = new NodeStats(clientNode, 0,
                                        null, null, null, null, null, null, null, null,
                                        null, null);

        NodesStatsResponse response = new NodesStatsResponse(mock(ClusterName.class),
                                                            new NodeStats[] {nodeStat});


        NodeInfo nodeInfo = new NodeInfo(mock(Version.class),
                mock(Build.class),
                clientNode,
                null, null, null, null, null, null, null, null,
                null, null);

        NodesInfoResponse infoResponse = new NodesInfoResponse(mock(ClusterName.class),
                                    new NodeInfo[] {nodeInfo});


        Client client = mock(Client.class);
        AdminClient ac = mock(AdminClient.class);
        ClusterAdminClient cac = mock(ClusterAdminClient.class);
        @SuppressWarnings("unchecked")
        ActionFuture<NodesStatsResponse> af = mock(ActionFuture.class);
        @SuppressWarnings("unchecked")
        ActionFuture<NodesInfoResponse> afInfo = mock(ActionFuture.class);

        when(client.admin()).thenReturn(ac);
        when(ac.cluster()).thenReturn(cac);
        when(cac.nodesStats(Mockito.any())).thenReturn(af);
        when(af.get()).thenReturn(response);

        when(cac.nodesInfo(Mockito.any())).thenReturn(afInfo);
        when(afInfo.get()).thenReturn(infoResponse);

        ElasticsearchServerInfo esi = new ElasticsearchServerInfo(client);
        ServerInfo info = esi.serverInfo();

        assertEquals(SystemUtils.OS_NAME, info.getOsName());
        assertEquals(SystemUtils.OS_VERSION, info.getOsVersion());
        assertNull(info.getCpuInfo());
        assertNull(info.getHostname());
        assertNull(info.getTotalDiskMb());
        assertNull(info.getTotalMemoryMb());
        assertNull(info.getAvailableDiskMb());

    }
}
