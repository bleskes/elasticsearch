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

import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.monitor.os.OsInfo;

import com.prelert.server.info.CpuInfo;
import com.prelert.server.info.ServerInfo;
import com.prelert.server.info.ServerInfoFactory;

public class ElasticsearchServerInfo implements ServerInfoFactory
{
    public static final String ATTRIBUTES = "attributes";
    public static final String CLIENT = "client";


    private static Logger LOGGER = Logger.getLogger(ElasticsearchServerInfo.class);
    private final Client m_Client;

    public ElasticsearchServerInfo(Client client)
    {
        m_Client = client;
    }

    @Override
    public ServerInfo serverInfo()
    {
        ServerInfo serverInfo = new ServerInfo();

        NodeInfo[] nodeInfos = this.nodesInfo();

        if (nodeInfos.length == 0)
        {
            LOGGER.error("No NodeInfo returned");
        }

        for (NodeInfo nodeInfo : nodeInfos)
        {
            // use the first data node
            if (nodeInfo.getNode().isClientNode() && nodeInfo.getNode().isDataNode() == false)
            {
                continue;
            }

            CpuInfo cpuInfo = new CpuInfo();
            populateCpuInfo(cpuInfo, nodeInfo.getOs().getCpu());
            serverInfo.setCpuInfo(cpuInfo);

            serverInfo.setHostname(nodeInfo.getHostname());
            serverInfo.setTotalMemoryMb(nodeInfo.getOs().getMem().getTotal().getMb());
            break;
        }


        NodeStats[] nodesStats = this.nodesStats();

        if (nodesStats.length == 0)
        {
            LOGGER.error("No NodeStats returned");
        }

        for (NodeStats nodeStat : nodesStats)
        {
            // use the first data node
            if (nodeStat.getNode().isClientNode() && nodeStat.getNode().isDataNode() == false)
            {
                continue;
            }

            serverInfo.setTotalDiskMb(nodeStat.getFs().getTotal().getTotal().getMb());
            serverInfo.setAvailableDiskMb(nodeStat.getFs().getTotal().getAvailable().getMb());
        }

        return serverInfo;
    }

    @Override
    public CpuInfo cpuInfo()
    {
        CpuInfo cpuInfo = new CpuInfo();

        NodeInfo[] nodeInfos = this.nodesInfo();

        if (nodeInfos.length == 0)
        {
            LOGGER.error("No NodeInfo returned");
        }

        // Use the first data node
        for (NodeInfo nodeInfo : nodesInfo())
        {
            if (nodeInfo.getNode().isClientNode() && nodeInfo.getNode().isDataNode() == false)
            {
                continue;
            }

            populateCpuInfo(cpuInfo, nodeInfo.getOs().getCpu());
            break;
        }


        return cpuInfo;
    }

    private void populateCpuInfo(CpuInfo cpuInfo, OsInfo.Cpu cpu)
    {
        cpuInfo.setModel(cpu.getModel());
        cpuInfo.setVendor(cpu.getVendor());
        cpuInfo.setCores(cpu.getTotalCores());
        cpuInfo.setFrequencyMHz(cpu.getMhz());
    }

    private NodeInfo[] nodesInfo()
    {
        try
        {
            NodesInfoResponse response = m_Client.admin().cluster().nodesInfo(
                new NodesInfoRequestBuilder(m_Client.admin().cluster()).all().request()).get();

            return response.getNodes();
        }
        catch (InterruptedException | ExecutionException e)
        {
            LOGGER.error("Error getting NodesInfo", e);
            return new NodeInfo[] {};
        }
    }

    private NodeStats[] nodesStats()
    {
        try
        {
            NodesStatsResponse response = m_Client.admin().cluster().nodesStats(
                new NodesStatsRequestBuilder(m_Client.admin().cluster()).all().request()).get();

            return response.getNodes();
        }
        catch (InterruptedException | ExecutionException e)
        {
            LOGGER.error("Error getting NodesStats", e);
            return new NodeStats[] {};
        }
    }
}
