/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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

package org.elasticsearch.xpack.ml.job.persistence;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.CheckedConsumer;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.action.bulk.byscroll.DeleteByQueryRequest;
import org.elasticsearch.action.bulk.byscroll.BulkByScrollResponse;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.xpack.ml.action.MlDeleteByQueryAction;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.CategorizerState;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.Quantiles;


import java.util.List;
import java.util.function.Consumer;

public class JobStorageDeletionTask extends Task {
    private final Logger logger;

    public JobStorageDeletionTask(long id, String type, String action, String description, TaskId parentTask) {
        super(id, type, action, description, parentTask);
        this.logger = Loggers.getLogger(getClass());
    }

    public void delete(String jobId, Client client, ClusterState state,
                       CheckedConsumer<Boolean, Exception> finishedHandler,
                       Consumer<Exception> failureHandler) {

        final String indexName = AnomalyDetectorsIndex.getPhysicalIndexFromState(state, jobId);
        final String indexPattern = indexName + "-*";
        final String aliasName = AnomalyDetectorsIndex.jobResultsAliasedName(jobId);

        CheckedConsumer<IndicesAliasesResponse, Exception> deleteAliasHandler = indicesAliasesResponse -> {
            if (!indicesAliasesResponse.isAcknowledged()) {
                logger.warn("Delete Alias request not acknowledged for alias [" + aliasName + "].");
            } else {
                logger.info("Done deleting alias [" + aliasName + "]");
            }

            finishedHandler.accept(true);
        };

        // Step 5. Delete categorizer state done, delete the alias
        ActionListener<Boolean> deleteCategorizerStateHandler = ActionListener.wrap(
                bulkItemResponses -> {
                    IndicesAliasesRequest request = new IndicesAliasesRequest()
                            .addAliasAction(IndicesAliasesRequest.AliasActions.remove().alias(aliasName).index(indexName));
                    client.admin().indices().aliases(request, ActionListener.wrap(deleteAliasHandler,
                            e -> {
                                if (e instanceof IndexNotFoundException) {
                                    logger.warn("Alias [" + aliasName + "] not found. Continuing to delete job.");
                                    try {
                                        finishedHandler.accept(false);
                                    } catch (Exception e1) {
                                        failureHandler.accept(e1);
                                    }
                                } else {
                                    // all other exceptions should die
                                    failureHandler.accept(e);
                                }
                            }));
                },
                failureHandler::accept);

        // Step 4. Delete model state done, delete the categorizer state
        ActionListener<BulkResponse> deleteStateHandler = ActionListener.wrap(response -> {
                    deleteCategorizerState(jobId, client, deleteCategorizerStateHandler);
                },
                failureHandler::accept
        );

        // Step 3. Delete quantiles done, delete the model state
        ActionListener<DeleteResponse> deleteQuantilesHandler = ActionListener.wrap(deleteResponse -> {
                    deleteModelState(jobId, client, deleteStateHandler);
                },
                failureHandler::accept
        );


        // Step 2. DBQ done, delete the state
        // -------
        // TODO norelease more robust handling of failures?
        ActionListener<BulkByScrollResponse> dbqHandler = ActionListener.wrap(bulkByScrollResponse -> {
                    if (bulkByScrollResponse.isTimedOut()) {
                        logger.warn("DeleteByQuery for indices [" + indexName + ", " + indexPattern + "] timed out.");
                    }
                    if (!bulkByScrollResponse.getBulkFailures().isEmpty()) {
                        logger.warn("[" + bulkByScrollResponse.getBulkFailures().size()
                                + "] failures encountered while running DeleteByQuery on indices [" + indexName + ", "
                                + indexPattern + "]. ");
                    }
                    deleteQuantiles(jobId, client, deleteQuantilesHandler);
                },
                failureHandler::accept
        );


        // Step 1. DeleteByQuery on the index, matching all docs with the right job_id
        // -------
        logger.info("Running DBQ on [" + indexName + "," + indexPattern + "] for job [" + jobId + "]");
        SearchRequest searchRequest = new SearchRequest(indexName, indexPattern);
        DeleteByQueryRequest request = new DeleteByQueryRequest(searchRequest);
        ConstantScoreQueryBuilder query = new ConstantScoreQueryBuilder(new TermQueryBuilder(Job.ID.getPreferredName(), jobId));
        searchRequest.source(new SearchSourceBuilder().query(query));
        searchRequest.indicesOptions(JobProvider.addIgnoreUnavailable(IndicesOptions.lenientExpandOpen()));
        request.setSlices(5);

        client.execute(MlDeleteByQueryAction.INSTANCE, request, dbqHandler);
    }

    public void deleteQuantiles(String jobId, Client client, ActionListener<DeleteResponse> finishedHandler) {
        client.prepareDelete(AnomalyDetectorsIndex.jobStateIndexName(), Quantiles.TYPE.getPreferredName(), Quantiles.documentId(jobId))
                .execute(finishedHandler);
    }

    private void deleteModelState(String jobId, Client client, ActionListener<BulkResponse> listener) {

        JobProvider jobProvider = new JobProvider(client, Settings.EMPTY);
        jobProvider.modelSnapshots(jobId, 0, 10000,
                page -> {
                    List<ModelSnapshot> deleteCandidates = page.results();

                    // Delete the snapshot and any associated state files
                    JobDataDeleter deleter = new JobDataDeleter(client, jobId);
                    for (ModelSnapshot deleteCandidate : deleteCandidates) {
                        deleter.deleteModelSnapshot(deleteCandidate);
                    }

                    deleter.commit(listener);
                },
                listener::onFailure);
    }

    private void deleteCategorizerState(String jobId, Client client, ActionListener<Boolean> finishedHandler) {
        int docNum = 0;
        while (true) {
            String docId = CategorizerState.categorizerStateDocId(jobId, ++docNum);
            DeleteRequest deleteRequest =
                    client.prepareDelete(AnomalyDetectorsIndex.jobStateIndexName(), CategorizerState.TYPE, docId).request();
            DeleteResponse response = client.delete(deleteRequest).actionGet();
            if (response.getResult() == DocWriteResponse.Result.NOT_FOUND) {
                break;
            }
        }

        finishedHandler.onResponse(true);
    }
}
