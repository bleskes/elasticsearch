/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.action.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeReadAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.PrelertServices;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.util.Optional;

public class TransportGetJobAction extends TransportMasterNodeReadAction<GetJobRequest, GetJobResponse> {

    private final PrelertServices prelertServices;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public TransportGetJobAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                 ThreadPool threadPool, ActionFilters actionFilters,
                                 IndexNameExpressionResolver indexNameExpressionResolver, PrelertServices prelertServices) {
        super(settings, GetJobsAction.NAME, transportService, clusterService, threadPool, actionFilters,
                indexNameExpressionResolver, GetJobRequest::new);
        this.prelertServices = prelertServices;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected GetJobResponse newResponse() {
        return new GetJobResponse();
    }

    @Override
    protected void masterOperation(GetJobRequest request, ClusterState state, ActionListener<GetJobResponse> listener) throws Exception {
        logger.debug("Get job '" + request.getJobId() + "'");
        Optional<JobDetails> optionalJob = prelertServices.getJobManager().getJob(request.getJobId());
        SingleDocument jobDocument = optionalJob.isPresent() ? createJobDocument(optionalJob.get()) : SingleDocument.empty(JobDetails.TYPE);
        if (jobDocument.isExists()) {
            logger.debug("Returning job '" + optionalJob.get().getJobId() + "'");
        }
        else {
            logger.debug(String.format("Cannot find job '%s'", request.getJobId()));
        }
        listener.onResponse(new GetJobResponse(jobDocument));
    }

    private SingleDocument createJobDocument(JobDetails job) throws JsonProcessingException {
        byte[] asBytes = objectMapper.writeValueAsBytes(job);
        return new SingleDocument(JobDetails.TYPE, new BytesArray(asBytes));
    }

    @Override
    protected ClusterBlockException checkBlock(GetJobRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
