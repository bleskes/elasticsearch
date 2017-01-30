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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.MasterNodeReadOperationRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.action.support.master.TransportMasterNodeReadAction;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedStatus;
import org.elasticsearch.xpack.ml.job.metadata.MlMetadata;
import org.elasticsearch.xpack.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.persistent.PersistentTasksInProgress;
import org.elasticsearch.xpack.persistent.PersistentTasksInProgress.PersistentTaskInProgress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class GetDatafeedsStatsAction extends Action<GetDatafeedsStatsAction.Request, GetDatafeedsStatsAction.Response,
        GetDatafeedsStatsAction.RequestBuilder> {

    public static final GetDatafeedsStatsAction INSTANCE = new GetDatafeedsStatsAction();
    public static final String NAME = "cluster:admin/ml/datafeeds/stats/get";

    public static final String ALL = "_all";
    private static final String STATUS = "status";

    private GetDatafeedsStatsAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client, this);
    }

    @Override
    public Response newResponse() {
        return new Response();
    }

    public static class Request extends MasterNodeReadRequest<Request> {

        private String datafeedId;

        public Request(String datafeedId) {
            this.datafeedId = ExceptionsHelper.requireNonNull(datafeedId, DatafeedConfig.ID.getPreferredName());
        }

        Request() {}

        public String getDatafeedId() {
            return datafeedId;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            datafeedId = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(datafeedId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(datafeedId);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Request other = (Request) obj;
            return Objects.equals(datafeedId, other.datafeedId);
        }
    }

    public static class RequestBuilder extends MasterNodeReadOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, GetDatafeedsStatsAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse implements ToXContentObject {

        public static class DatafeedStats implements ToXContent, Writeable {

            private final String datafeedId;
            private final DatafeedStatus datafeedStatus;

            DatafeedStats(String datafeedId, DatafeedStatus datafeedStatus) {
                this.datafeedId = Objects.requireNonNull(datafeedId);
                this.datafeedStatus = Objects.requireNonNull(datafeedStatus);
            }

            DatafeedStats(StreamInput in) throws IOException {
                datafeedId = in.readString();
                datafeedStatus = DatafeedStatus.fromStream(in);
            }

            public String getDatafeedId() {
                return datafeedId;
            }

            public DatafeedStatus getDatafeedStatus() {
                return datafeedStatus;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                builder.startObject();
                builder.field(DatafeedConfig.ID.getPreferredName(), datafeedId);
                builder.field(STATUS, datafeedStatus);
                builder.endObject();

                return builder;
            }

            @Override
            public void writeTo(StreamOutput out) throws IOException {
                out.writeString(datafeedId);
                datafeedStatus.writeTo(out);
            }

            @Override
            public int hashCode() {
                return Objects.hash(datafeedId, datafeedStatus);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                GetDatafeedsStatsAction.Response.DatafeedStats other = (GetDatafeedsStatsAction.Response.DatafeedStats) obj;
                return Objects.equals(datafeedId, other.datafeedId) && Objects.equals(this.datafeedStatus, other.datafeedStatus);
            }
        }

        private QueryPage<DatafeedStats> datafeedsStats;

        public Response(QueryPage<DatafeedStats> datafeedsStats) {
            this.datafeedsStats = datafeedsStats;
        }

        public Response() {}

        public QueryPage<DatafeedStats> getResponse() {
            return datafeedsStats;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            datafeedsStats = new QueryPage<>(in, DatafeedStats::new);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            datafeedsStats.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            datafeedsStats.doXContentBody(builder, params);
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(datafeedsStats);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Response other = (Response) obj;
            return Objects.equals(datafeedsStats, other.datafeedsStats);
        }

        @Override
        public final String toString() {
            return Strings.toString(this);
        }
    }

    public static class TransportAction extends TransportMasterNodeReadAction<Request, Response> {

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                               ThreadPool threadPool, ActionFilters actionFilters,
                               IndexNameExpressionResolver indexNameExpressionResolver) {
            super(settings, GetDatafeedsStatsAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
        }

        @Override
        protected String executor() {
            return ThreadPool.Names.SAME;
        }

        @Override
        protected Response newResponse() {
            return new Response();
        }

        @Override
        protected void masterOperation(Request request, ClusterState state, ActionListener<Response> listener) throws Exception {
            logger.debug("Get stats for datafeed '{}'", request.getDatafeedId());

            Map<String, DatafeedStatus> statuses = new HashMap<>();
            PersistentTasksInProgress tasksInProgress = state.custom(PersistentTasksInProgress.TYPE);
            if (tasksInProgress != null) {
                Predicate<PersistentTaskInProgress<?>> predicate = ALL.equals(request.getDatafeedId()) ? p -> true :
                        p -> request.getDatafeedId().equals(((StartDatafeedAction.Request) p.getRequest()).getDatafeedId());
                for (PersistentTaskInProgress<?> taskInProgress : tasksInProgress.findEntries(StartDatafeedAction.NAME, predicate)) {
                    StartDatafeedAction.Request storedRequest = (StartDatafeedAction.Request) taskInProgress.getRequest();
                    statuses.put(storedRequest.getDatafeedId(), DatafeedStatus.STARTED);
                }
            }

            List<Response.DatafeedStats> stats = new ArrayList<>();
            MlMetadata mlMetadata = state.metaData().custom(MlMetadata.TYPE);
            if (ALL.equals(request.getDatafeedId())) {
                Collection<DatafeedConfig> datafeeds = mlMetadata.getDatafeeds().values();
                for (DatafeedConfig datafeed : datafeeds) {
                    DatafeedStatus status = statuses.getOrDefault(datafeed.getId(), DatafeedStatus.STOPPED);
                    stats.add(new Response.DatafeedStats(datafeed.getId(), status));
                }
            } else {
                DatafeedConfig datafeed = mlMetadata.getDatafeed(request.getDatafeedId());
                if (datafeed == null) {
                    throw ExceptionsHelper.missingDatafeedException(request.getDatafeedId());
                }
                DatafeedStatus status = statuses.getOrDefault(datafeed.getId(), DatafeedStatus.STOPPED);
                stats.add(new Response.DatafeedStats(datafeed.getId(), status));
            }
            QueryPage<Response.DatafeedStats> statsPage = new QueryPage<>(stats, stats.size(), DatafeedConfig.RESULTS_FIELD);
            listener.onResponse(new Response(statsPage));
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
        }
    }
}
