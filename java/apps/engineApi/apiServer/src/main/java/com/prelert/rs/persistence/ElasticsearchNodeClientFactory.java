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

package com.prelert.rs.persistence;

import java.util.Objects;

import org.apache.log4j.Logger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.log4j.Log4jESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider;

public class ElasticsearchNodeClientFactory extends ElasticsearchFactory
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchNodeClientFactory.class);

    private final Node m_Node;

    private ElasticsearchNodeClientFactory(Node node)
    {
        super(node.client());
        m_Node = Objects.requireNonNull(node);
    }

    public static ElasticsearchFactory create(String elasticSearchHost,
            String networkPublishHost, String elasticSearchClusterName,
            String portRange, String numProcessors)
    {
        // Tell Elasticsearch to log via our log4j root logger
        ESLoggerFactory.setDefaultFactory(new Log4jESLoggerFactory());
        Node node = NodeBuilder.nodeBuilder()
                .settings(buildSettings(elasticSearchHost, networkPublishHost, portRange, numProcessors))
                .client(true)
                .clusterName(elasticSearchClusterName).node();
        return new ElasticsearchNodeClientFactory(node);
    }

    /**
     * Elasticsearch settings that instruct the node not to accept HTTP, not to
     * attempt multicast discovery and to only look for another node to connect
     * to on the given host.
     */
    private static Settings buildSettings(String host, String networkPublishHost,
            String portRange, String numProcessors)
    {
        // Multicast discovery is expected to be disabled on the Elasticsearch
        // data node, so disable it for this embedded node too and tell it to
        // expect the data node to be on the same machine
        Builder builder = Settings.builder()
                .put("http.enabled", "false")
                .put("network.publish_host", networkPublishHost)
                .put("discovery.zen.ping.unicast.hosts", host);

        if (networkPublishHost != null && networkPublishHost.isEmpty() == false)
        {
            LOGGER.info("Using network " + networkPublishHost + " for Elasticsearch publishing");
            builder.put("network.publish_host", networkPublishHost);
        }
        if (portRange != null && portRange.isEmpty() == false)
        {
            LOGGER.info("Using TCP port range " + portRange + " to connect to Elasticsearch");
            builder.put("transport.tcp.port", portRange);
        }
        if (numProcessors != null && numProcessors.isEmpty() == false)
        {
            LOGGER.info("Telling Elasticsearch there are " + numProcessors
                    + " processors on this machine");
            builder.put("processors", numProcessors);
        }

        return builder.build();
    }

    @Override
    public JobProvider newJobProvider()
    {
        return new ElasticsearchJobProvider(m_Node, m_Node.client(), numberOfReplicas());
    }
}
