/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.results.ReservedFieldNames;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

class ElasticsearchJobDetailsMapper
{
    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchJobDetailsMapper.class);

    private final Client client;
    private final ObjectMapper objectMapper;

    public ElasticsearchJobDetailsMapper(Client client, ObjectMapper objectMapper)
    {
        this.client = Objects.requireNonNull(client);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Maps an Elasticsearch source map to a {@link JobDetails} object
     * @param source The source of an Elasticsearch search response
     * @return the {@code JobDetails} object
     */
    public JobDetails map(Map<String, Object> source)
    {
        JobDetails job = objectMapper.convertValue(source, JobDetails.class);
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(job.getId());

        addModelSizeStats(job, elasticJobId);
        addBucketProcessingTime(job, elasticJobId);

        return job;
    }

    private void addModelSizeStats(JobDetails job, ElasticsearchJobId elasticJobId)
    {
        // Pull out the modelSizeStats document, and add this to the JobDetails
        LOGGER.trace("ES API CALL: get ID " + ModelSizeStats.TYPE +
                " type " + ModelSizeStats.TYPE + " from index " + elasticJobId.getIndex());
        GetResponse modelSizeStatsResponse = client.prepareGet(
                elasticJobId.getIndex(), ModelSizeStats.TYPE.getPreferredName(), ModelSizeStats.TYPE.getPreferredName()).get();

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
            modelSizeStatsResponse.getSource().put(ModelSizeStats.TIMESTAMP_FIELD.getPreferredName(), timestamp);

            ModelSizeStats modelSizeStats = objectMapper.convertValue(
                    modelSizeStatsResponse.getSource(), ModelSizeStats.class);
            job.setModelSizeStats(modelSizeStats);
        }
    }

    private void addBucketProcessingTime(JobDetails job, ElasticsearchJobId elasticJobId)
    {
        // Pull out the modelSizeStats document, and add this to the JobDetails
        LOGGER.trace("ES API CALL: get ID " + ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE +
                " type " + ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS + " from index " + elasticJobId.getIndex());
        GetResponse procTimeResponse = client.prepareGet(
                elasticJobId.getIndex(), ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE,
                ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS).get();

        if (!procTimeResponse.isExists())
        {
            String msg = "No average bucket processing time details for job with id " + job.getId();
            LOGGER.warn(msg);
        }
        else
        {
            Object averageTime = procTimeResponse.getSource()
                    .get(ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS);
            if (averageTime instanceof Double)
            {
                job.setAverageBucketProcessingTimeMs((Double)averageTime);
            }
        }
    }
}
