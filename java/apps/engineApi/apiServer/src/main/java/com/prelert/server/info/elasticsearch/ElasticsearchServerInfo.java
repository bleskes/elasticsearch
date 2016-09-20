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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoAction;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsAction;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.prelert.server.info.ServerInfo;
import com.prelert.server.info.ServerInfoFactory;

public class ElasticsearchServerInfo implements ServerInfoFactory, Feature
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchServerInfo.class);

    private final Client m_Client;

    public ElasticsearchServerInfo(Client client)
    {
        m_Client = client;
    }

    /**
     * Required by the Feature interface.
     */
    @Override
    public boolean configure(FeatureContext context)
    {
        return true;
    }

    @Override
    public ServerInfo serverInfo()
    {
        ServerInfo serverInfo = new ServerInfo();

        List<NodeStats> nodesStats = this.nodesStats();

        if (nodesStats.isEmpty())
        {
            LOGGER.error("No NodeStats returned");
        }

        for (NodeStats nodeStat : nodesStats)
        {
            // use the first data node
            if (nodeStat.getNode().isDataNode())
            {
                try
                {
                    serverInfo.setTotalDiskMb(nodeStat.getFs().getTotal().getTotal().getMb());
                }
                catch (NullPointerException npe)
                {
                    LOGGER.warn("Failed to get total disk MB");
                }
                try
                {
                    serverInfo.setAvailableDiskMb(nodeStat.getFs().getTotal().getAvailable().getMb());
                }
                catch (NullPointerException npe)
                {
                    LOGGER.warn("Failed to get available disk MB");
                }
                try
                {
                    serverInfo.setTotalMemoryMb(nodeStat.getOs().getMem().getTotal().getMb());
                }
                catch (NullPointerException npe)
                {
                    LOGGER.warn("Failed to get total memory MB");
                }
                break;
            }
        }

        return serverInfo;
    }

    /**
     * JSON formatted server stats.
     * Includes memory usage, disk usage and the size of the
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
            if (!nodeInfo.getNode().isMasterNode() && !nodeInfo.getNode().isDataNode())
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
            if (!nodeStats.getNode().isMasterNode() && !nodeStats.getNode().isDataNode())
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

            return builder.bytes().utf8ToString();
        }
        catch (IOException e)
        {
            LOGGER.error("Error serialising server stats", e);
            return "";
        }
    }

    protected List<NodeInfo> nodesInfo()
    {
        LOGGER.trace("ES API CALL: node info all nodes");
        NodesInfoResponse response = NodesInfoAction.INSTANCE
                .newRequestBuilder(m_Client.admin().cluster()).all().get();
        return response.getNodes();
    }

    protected List<NodeStats> nodesStats()
    {
        LOGGER.trace("ES API CALL: node stats all nodes");
        NodesStatsResponse response = NodesStatsAction.INSTANCE
                .newRequestBuilder(m_Client.admin().cluster()).all().get();
        return response.getNodes();
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
