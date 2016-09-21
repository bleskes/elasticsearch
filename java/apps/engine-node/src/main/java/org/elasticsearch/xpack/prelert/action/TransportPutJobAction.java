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
package org.elasticsearch.xpack.prelert.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.xpack.prelert.PrelertServices;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportPutJobAction extends TransportMasterNodeAction<PutJobRequest, PutJobResponse> {

    private final PrelertServices prelertServices;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public TransportPutJobAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                 ThreadPool threadPool, ActionFilters actionFilters,
                                 IndexNameExpressionResolver indexNameExpressionResolver, PrelertServices prelertServices) {
        super(settings, PutJobAction.NAME, transportService, clusterService, threadPool, actionFilters,
                indexNameExpressionResolver, PutJobRequest::new);
        this.prelertServices = prelertServices;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected PutJobResponse newResponse() {
        return new PutJobResponse();
    }

    @Override
    protected void masterOperation(PutJobRequest request, ClusterState state, ActionListener<PutJobResponse> listener) throws Exception {
        JobConfiguration jobConfiguration = objectMapper.readValue(request.getJobConfiguration().toBytesRef().bytes, JobConfiguration.class);
        JobDetails jobDetails = prelertServices.getJobManager().createJob(jobConfiguration, request.isOverwrite());
        listener.onResponse(new PutJobResponse(jobDetails, objectMapper));
    }

    @Override
    protected ClusterBlockException checkBlock(PutJobRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
