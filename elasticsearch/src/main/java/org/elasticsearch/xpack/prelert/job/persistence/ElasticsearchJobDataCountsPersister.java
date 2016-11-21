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

import java.io.IOException;
import java.util.Locale;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.xpack.prelert.job.DataCounts;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


public class ElasticsearchJobDataCountsPersister implements JobDataCountsPersister {

    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchJobDataCountsPersister.class);
    private Client client;

    public ElasticsearchJobDataCountsPersister(Client client) {
        this.client = client;
    }

    private XContentBuilder serialiseCounts(DataCounts counts) throws IOException {
        XContentBuilder builder = jsonBuilder();
        return counts.toXContent(builder, ToXContent.EMPTY_PARAMS);
    }

    @Override
    public void persistDataCounts(String jobId, DataCounts counts) {
        try {
            XContentBuilder content = serialiseCounts(counts);

            client.prepareIndex(ElasticsearchPersister.getJobIndexName(jobId), DataCounts.TYPE.getPreferredName(),
                    jobId + DataCounts.DOCUMENT_SUFFIX)
            .setSource(content).execute().actionGet();
        }
        catch (IOException ioe) {
            LOGGER.warn("Error serialising DataCounts stats", ioe);
        }
        catch (IndexNotFoundException e) {
            String msg = String.format(Locale.ROOT, "Error writing the job '%s' status stats.", jobId);
            LOGGER.warn(msg, e);
        }
    }
}
