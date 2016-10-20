
package org.elasticsearch.xpack.prelert.job.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.JobDetails;


public class ElasticsearchJobDataCountsPersister implements JobDataCountsPersister {
    private static final int RETRY_ON_CONFLICT = 3;

    private Client client;
    private Logger logger;

    public ElasticsearchJobDataCountsPersister(Client client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    @Override
    public void persistDataCounts(String jobId, DataCounts counts) {
        // NORELEASE - Should these stats be stored in memory?
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        try {
            UpdateRequestBuilder updateBuilder = client.prepareUpdate(elasticJobId.getIndex(),
                    JobDetails.TYPE, elasticJobId.getId());
            updateBuilder.setRetryOnConflict(RETRY_ON_CONFLICT);

            Map<String, Object> updates = new HashMap<>();
            updates.put(DataCounts.PROCESSED_RECORD_COUNT_STR, counts.getProcessedRecordCount());
            updates.put(DataCounts.PROCESSED_FIELD_COUNT_STR, counts.getProcessedFieldCount());
            updates.put(DataCounts.INPUT_RECORD_COUNT_STR, counts.getInputRecordCount());
            updates.put(DataCounts.INPUT_BYTES_STR, counts.getInputBytes());
            updates.put(DataCounts.INPUT_FIELD_COUNT_STR, counts.getInputFieldCount());
            updates.put(DataCounts.INVALID_DATE_COUNT_STR, counts.getInvalidDateCount());
            updates.put(DataCounts.MISSING_FIELD_COUNT_STR, counts.getMissingFieldCount());
            updates.put(DataCounts.OUT_OF_ORDER_TIME_COUNT_STR, counts.getOutOfOrderTimeStampCount());
            updates.put(DataCounts.FAILED_TRANSFORM_COUNT_STR, counts.getFailedTransformCount());
            updates.put(DataCounts.EXCLUDED_RECORD_COUNT_STR, counts.getExcludedRecordCount());
            updates.put(DataCounts.LATEST_RECORD_TIME_STR, counts.getLatestRecordTimeStamp());

            Map<String, Object> countUpdates = new HashMap<>();
            countUpdates.put(JobDetails.COUNTS.getPreferredName(), updates);

            updateBuilder.setDoc(countUpdates).setRefreshPolicy(RefreshPolicy.IMMEDIATE);

            logger.trace("ES API CALL: update ID {} type {} in index {} using map of new values", jobId, JobDetails.TYPE, elasticJobId.getIndex());
            client.update(updateBuilder.request()).get();
        } catch (IndexNotFoundException | InterruptedException | ExecutionException e) {
            String msg = String.format("Error writing the job '%s' status stats.", elasticJobId.getId());

            logger.warn(msg, e);
        }
    }
}
