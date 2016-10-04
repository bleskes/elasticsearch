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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeReadAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.PrelertServices;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.validation.PaginationParamsValidator;

public class TransportGetJobsAction extends TransportMasterNodeReadAction<GetJobsRequest, GetJobsResponse> {

    private final PrelertServices prelertServices;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public TransportGetJobsAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                  ThreadPool threadPool, ActionFilters actionFilters,
                                  IndexNameExpressionResolver indexNameExpressionResolver, PrelertServices prelertServices) {
        super(settings, GetJobsAction.NAME, transportService, clusterService, threadPool, actionFilters,
                indexNameExpressionResolver, GetJobsRequest::new);
        this.prelertServices = prelertServices;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected GetJobsResponse newResponse() {
        return new GetJobsResponse();
    }

    @Override
    protected void masterOperation(GetJobsRequest request, ClusterState state, ActionListener<GetJobsResponse> listener) throws Exception {
        new PaginationParamsValidator(request.getSkip(), request.getTake()).validate();
        QueryPage<JobDetails> jobsPage = prelertServices.getJobManager().getJobs(request.getSkip(), request.getTake());
        listener.onResponse(new GetJobsResponse(jobsPage, objectMapper));
    }

    @Override
    protected ClusterBlockException checkBlock(GetJobsRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
