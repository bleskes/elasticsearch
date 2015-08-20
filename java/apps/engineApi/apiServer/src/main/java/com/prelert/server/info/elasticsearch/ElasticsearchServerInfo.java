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

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
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


    /**
     * JSON formatted server stats.
     * Includes CPU load, memory usage, disk usage and the size of the
     * Elasticsearch indexes.
     */
    @Override
    public String serverStats()
    {
        NodeInfo apiNodeInfo = null;
        NodeStats apiNodeStats = null;
        NodeInfo esNodeInfo = null;
        NodeStats esNodeStats = null;

        for (NodeInfo nodeInfo : nodesInfo())
        {
            if (nodeInfo.getNode().isClientNode() && nodeInfo.getNode().isDataNode() == false)
            {
                apiNodeInfo = nodeInfo;
            }
            else
            {
                esNodeInfo = nodeInfo;
            }

            if (apiNodeInfo != null && esNodeInfo != null)
            {
                break;
            }
        }

        for (NodeStats nodeStats : nodesStats())
        {
            if (nodeStats.getNode().isClientNode() && nodeStats.getNode().isDataNode() == false)
            {
                apiNodeStats = nodeStats;
            }
            else
            {
                esNodeStats = nodeStats;
            }

            if (apiNodeStats != null && esNodeStats != null)
            {
                break;
            }
        }

        try
        {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.humanReadable(true).prettyPrint();

            builder.startObject();
            builder.field("timestamp", new Date());
            writeApiServerNode(builder, apiNodeInfo);
            writeElasticsearchNode(builder, esNodeInfo, esNodeStats);

            builder.endObject();

            return builder.bytes().toUtf8();
        }
        catch (IOException e)
        {
            LOGGER.error("Error serialising server stats", e);
            return "";
        }
    }



    private NodeInfo[] nodesInfo()
    {
        try
        {
            LOGGER.trace("ES API CALL: node info all nodes");
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
            LOGGER.trace("ES API CALL: node stats all nodes");
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



    private void writeApiServerNode(XContentBuilder builder, NodeInfo nodeInfo)
    {
        try
        {
            builder.startObject("apiServer");
            if (nodeInfo != null)
            {
                nodeInfo.getJvm().toXContent(builder, ToXContent.EMPTY_PARAMS);
                nodeInfo.getProcess().toXContent(builder, ToXContent.EMPTY_PARAMS);
            }
            builder.endObject();
        }
        catch (IOException e)
        {
            LOGGER.error("Error serialising NodeInfo", e);
        }

    }

    private void writeElasticsearchNode(XContentBuilder builder, NodeInfo nodeInfo, NodeStats nodeStats)
    {
        try
        {
            builder.startObject("elasticsearch");

            if (nodeInfo != null)
            {
                builder.startObject("info");
                nodeInfo.getOs().toXContent(builder, ToXContent.EMPTY_PARAMS);
                nodeInfo.getJvm().toXContent(builder, ToXContent.EMPTY_PARAMS);
                nodeInfo.getProcess().toXContent(builder, ToXContent.EMPTY_PARAMS);
                builder.endObject();
            }

            if (nodeStats != null)
            {
                builder.field("host", nodeStats.getHostname());
                builder.startObject("stats");
                nodeStats.getIndices().toXContent(builder, ToXContent.EMPTY_PARAMS);
                nodeStats.getOs().toXContent(builder, ToXContent.EMPTY_PARAMS);
                nodeStats.getProcess().toXContent(builder, ToXContent.EMPTY_PARAMS);
                nodeStats.getJvm().toXContent(builder, ToXContent.EMPTY_PARAMS);
                nodeStats.getFs().toXContent(builder, ToXContent.EMPTY_PARAMS);
                builder.endObject();
            }

            builder.endObject();
        }
        catch (IOException e)
        {
            LOGGER.error("Error serialising Elasticsearch node info & stats", e);
        }

    }
}
