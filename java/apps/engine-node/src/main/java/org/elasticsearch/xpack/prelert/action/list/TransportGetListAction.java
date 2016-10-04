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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.TransportGetAction;
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
import org.elasticsearch.xpack.prelert.job.persistence.ListDocument;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

public class TransportGetListAction extends TransportMasterNodeReadAction<GetListRequest, GetListResponse> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransportGetAction transportGetAction;

    // TODO these need to be moved to a settings object later
    // See #20
    private static final String PRELERT_INFO_INDEX = "prelert-int";

    @Inject
    public TransportGetListAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                  ThreadPool threadPool, ActionFilters actionFilters,
                                  IndexNameExpressionResolver indexNameExpressionResolver,
                                  TransportGetAction transportGetAction) {
        super(settings, GetListAction.NAME, transportService, clusterService, threadPool, actionFilters,
                indexNameExpressionResolver, GetListRequest::new);
        this.transportGetAction = transportGetAction;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected GetListResponse newResponse() {
        return new GetListResponse();
    }

    @Override
    protected void masterOperation(GetListRequest request, ClusterState state, ActionListener<GetListResponse> listener) throws Exception {
        final String listId = request.getListId();
        GetRequest getRequest = new GetRequest(PRELERT_INFO_INDEX, ListDocument.TYPE, listId);
        transportGetAction.execute(getRequest, new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getDocResponse) {
                SingleDocument responseBody;
                if (getDocResponse.isExists()) {
                    responseBody = new SingleDocument(ListDocument.TYPE, getDocResponse.getSourceAsBytesRef());
                } else {
                    responseBody = SingleDocument.empty(ListDocument.TYPE);
                }

                try {
                    GetListResponse listResponse = new GetListResponse(responseBody);
                    listener.onResponse(listResponse);
                } catch (Exception e) {
                    this.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                throw new ResourceNotFoundException("List with id [" + listId + "] not found", e);
            }
        });
    }

    @Override
    protected ClusterBlockException checkBlock(GetListRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
