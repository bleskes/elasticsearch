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

import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelSizeStats;

class ElasticsearchJobDetailsMapper
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchJobDetailsMapper.class);

    private final Client m_Client;
    private final ObjectMapper m_ObjectMapper;

    public ElasticsearchJobDetailsMapper(Client client, ObjectMapper objectMapper)
    {
        m_Client = Objects.requireNonNull(client);
        m_ObjectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Maps an Elasticsearch source map to a {@link JobDetails} object
     * @param source The source of an Elasticsearch search response
     * @return the {@code JobDetails} object
     * @throws CannotMapJobFromJson if the job fails to be mapped
     */
    public JobDetails map(Map<String, Object> source)
    {
        JobDetails job;
        try
        {
            job = m_ObjectMapper.convertValue(source, JobDetails.class);
        }
        catch (IllegalArgumentException e)
        {
            String msg = "Cannot parse job from JSON";
            LOGGER.error(msg, e);
            throw new CannotMapJobFromJson(msg, e);
        }
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(job.getId());

        // Pull out the modelSizeStats document, and add this to the JobDetails
        LOGGER.trace("ES API CALL: get ID " + ModelSizeStats.TYPE +
                " type " + ModelSizeStats.TYPE + " from index " + elasticJobId.getIndex());
        GetResponse modelSizeStatsResponse = m_Client.prepareGet(
                elasticJobId.getIndex(), ModelSizeStats.TYPE, ModelSizeStats.TYPE).get();

        if (!modelSizeStatsResponse.isExists())
        {
            String msg = "No memory usage details for job with id " + job.getId();
            LOGGER.warn(msg);
        }
        else
        {
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object timestamp = modelSizeStatsResponse.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            modelSizeStatsResponse.getSource().put(ModelSizeStats.TIMESTAMP, timestamp);

            ModelSizeStats modelSizeStats = m_ObjectMapper.convertValue(
                modelSizeStatsResponse.getSource(), ModelSizeStats.class);
            job.setModelSizeStats(modelSizeStats);
        }
        return job;
    }
}
