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

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.bulk.TransportBulkAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.ml.job.JobManager;
import org.elasticsearch.xpack.ml.job.config.MlFilter;
import org.elasticsearch.xpack.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.ml.job.persistence.JobProvider;
import org.elasticsearch.xpack.ml.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.Objects;


public class PutFilterAction extends Action<PutFilterAction.Request, PutFilterAction.Response, PutFilterAction.RequestBuilder> {

    public static final PutFilterAction INSTANCE = new PutFilterAction();
    public static final String NAME = "cluster:admin/xpack/ml/filters/put";

    private PutFilterAction() {
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

    public static class Request extends MasterNodeReadRequest<Request> implements ToXContent {

        public static Request parseRequest(XContentParser parser) {
            MlFilter filter = MlFilter.PARSER.apply(parser, null);
            return new Request(filter);
        }

        private MlFilter filter;

        Request() {

        }

        public Request(MlFilter filter) {
            this.filter = ExceptionsHelper.requireNonNull(filter, "filter");
        }

        public MlFilter getFilter() {
            return this.filter;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            filter = new MlFilter(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            filter.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            filter.toXContent(builder, params);
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(filter);
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
            return Objects.equals(filter, other.filter);
        }
    }

    public static class RequestBuilder extends MasterNodeOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, PutFilterAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends AcknowledgedResponse {

        public Response() {
            super(true);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            readAcknowledged(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            writeAcknowledged(out);
        }

    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final TransportBulkAction transportBulkAction;

        @Inject
        public TransportAction(Settings settings, ThreadPool threadPool,
                TransportService transportService, ActionFilters actionFilters,
                IndexNameExpressionResolver indexNameExpressionResolver,
                TransportBulkAction transportBulkAction) {
            super(settings, NAME, threadPool, transportService, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.transportBulkAction = transportBulkAction;
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {
            MlFilter filter = request.getFilter();
            final String filterId = filter.getId();
            IndexRequest indexRequest = new IndexRequest(AnomalyDetectorsIndex.ML_META_INDEX, MlFilter.TYPE.getPreferredName(), filterId);
            try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
                indexRequest.source(filter.toXContent(builder, ToXContent.EMPTY_PARAMS));
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to serialise filter with id [" + filter.getId() + "]", e);
            }
            BulkRequest bulkRequest = new BulkRequest().add(indexRequest);

            transportBulkAction.execute(bulkRequest, new ActionListener<BulkResponse>() {
                @Override
                public void onResponse(BulkResponse indexResponse) {
                    listener.onResponse(new Response());
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(new ResourceNotFoundException("Could not create filter with ID [" + filterId + "]", e));
                }
            });
        }
    }
}

