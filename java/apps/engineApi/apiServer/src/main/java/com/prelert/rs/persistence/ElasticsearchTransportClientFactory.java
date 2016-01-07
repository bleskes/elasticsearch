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

import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider;

public class ElasticsearchTransportClientFactory extends ElasticsearchFactory
{
    public static ElasticsearchFactory create(String hostPortPairsList, String elasticSearchClusterName)
    {
        List<HostPortPair> hostAndPortList = HostPortPair.ofList(hostPortPairsList);
        TransportClient client = TransportClient.builder()
                .settings(Settings.builder().put(CLUSTER_NAME_KEY, elasticSearchClusterName).build())
                .build();
        for (HostPortPair hostAndPort : hostAndPortList)
        {
            client.addTransportAddress(
                    new InetSocketTransportAddress(hostAndPort.getHost(), hostAndPort.getPort()));
        }
        return new ElasticsearchTransportClientFactory(client);
    }

    private ElasticsearchTransportClientFactory(Client client)
    {
        super(client);
    }

    @Override
    public JobProvider newJobProvider()
    {
        return new ElasticsearchJobProvider(null, getClient());
    }
}
