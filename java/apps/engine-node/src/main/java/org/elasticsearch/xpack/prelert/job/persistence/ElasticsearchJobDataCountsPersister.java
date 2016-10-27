
package org.elasticsearch.xpack.prelert.job.persistence;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.xpack.prelert.job.DataCounts;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


public class ElasticsearchJobDataCountsPersister implements JobDataCountsPersister {

    private Client client;
    private Logger logger;

    public ElasticsearchJobDataCountsPersister(Client client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    private XContentBuilder serialiseCounts(DataCounts counts) throws IOException {
        XContentBuilder builder = jsonBuilder();
        return counts.toXContent(builder, ToXContent.EMPTY_PARAMS);
    }


    @Override
    public void persistDataCounts(String jobId, DataCounts counts) {
        // NORELEASE - Should these stats be stored in memory?
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);


        try {
            XContentBuilder content = serialiseCounts(counts);

            client.prepareIndex(elasticJobId.getIndex(), DataCounts.TYPE.getPreferredName(), elasticJobId.getId() + "-data-counts")
                    .setSource(content).execute().actionGet();
        }
        catch (IOException ioe) {
            logger.warn("Error serialising DataCounts stats", ioe);
        }
        catch (IndexNotFoundException e) {
            String msg = String.format("Error writing the job '%s' status stats.", elasticJobId.getId());
            logger.warn(msg, e);
        }
    }
}
