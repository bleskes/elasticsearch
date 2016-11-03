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
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.results.ReservedFieldNames;

import java.io.IOException;
import java.util.Objects;

class ElasticsearchJobDetailsMapper
{
    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchJobDetailsMapper.class);

    private final Client client;
    private final ParseFieldMatcher parseFieldMatcher;

    public ElasticsearchJobDetailsMapper(Client client, ParseFieldMatcher parseFieldMatcher)
    {
        this.client = Objects.requireNonNull(client);
        this.parseFieldMatcher = Objects.requireNonNull(parseFieldMatcher);
    }

    /**
     * Maps an Elasticsearch source map to a {@link JobDetails} object
     * @param source The source of an Elasticsearch search response
     * @return the {@code JobDetails} object
     */
    public JobDetails map(BytesReference source)
    {
        XContentParser parser;
        try {
            parser = XContentFactory.xContent(source).createParser(source);
        } catch (IOException e) {
            throw new ElasticsearchParseException("failed to parser job", e);
        }
        JobDetails job = JobDetails.PARSER.apply(parser, () -> parseFieldMatcher);
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
            BytesReference source = modelSizeStatsResponse.getSourceAsBytesRef();
            XContentParser parser;
            try {
                parser = XContentFactory.xContent(source).createParser(source);
            } catch (IOException e) {
                throw new ElasticsearchParseException("failed to parser model size stats", e);
            }
            ModelSizeStats modelSizeStats = ModelSizeStats.PARSER.apply(parser, () -> parseFieldMatcher);
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
