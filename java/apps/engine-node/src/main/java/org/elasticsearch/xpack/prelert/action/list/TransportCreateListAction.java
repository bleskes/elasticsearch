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

package org.elasticsearch.xpack.prelert.action.list;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.index.TransportIndexAction;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.persistence.ListDocument;

import java.io.IOException;

public class TransportCreateListAction extends TransportMasterNodeAction<CreateListRequest, CreateListResponse> {

    private final TransportIndexAction transportIndexAction;

    // TODO these need to be moved to a settings object later. See #20
    private static final String PRELERT_INFO_INDEX = "prelert-int";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public TransportCreateListAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                     ThreadPool threadPool, ActionFilters actionFilters,
                                     IndexNameExpressionResolver indexNameExpressionResolver,
                                     TransportIndexAction transportIndexAction) {
        super(settings, CreateListAction.NAME, transportService, clusterService, threadPool, actionFilters,
                indexNameExpressionResolver, CreateListRequest::new);
        this.transportIndexAction = transportIndexAction;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected CreateListResponse newResponse() {
        return new CreateListResponse();
    }

    @Override
    protected void masterOperation(CreateListRequest request, ClusterState state, ActionListener<CreateListResponse> listener) throws Exception {
        ListDocument listDocument;
        try {
            listDocument = objectMapper.readValue(request.getRequest().toBytesRef().bytes, ListDocument.class);
        } catch (JsonMappingException e) {
            throw new ElasticsearchParseException("Missing required properties for List", e);
        }
        final String listId = listDocument.getId();
        IndexRequest indexRequest = new IndexRequest(PRELERT_INFO_INDEX, ListDocument.TYPE, listId);
        indexRequest.source(request.getRequest());
        transportIndexAction.execute(indexRequest, new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                listener.onResponse(new CreateListResponse());
            }

            @Override
            public void onFailure(Exception e) {
                logger.error("Could not create list with ID [" + listId + "]", e);
                throw new ResourceNotFoundException("Could not create list with ID [" + listId + "]", e);
            }
        });
    }

    @Override
    protected ClusterBlockException checkBlock(CreateListRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
