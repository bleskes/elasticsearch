package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.manager.AutodetectProcessManager;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.TimeRange;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;

public class PostDataFlushAction extends Action<PostDataFlushAction.Request, PostDataFlushAction.Response,
        PostDataFlushAction.RequestBuilder> {

    public static final PostDataFlushAction INSTANCE = new PostDataFlushAction();
    public static final String NAME = "cluster:admin/prelert/data/post/flush";

    private PostDataFlushAction() {
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

    public static class Request extends MasterNodeRequest<Request> {

        private String jobId;
        private boolean calcInterim = false;
        private String start;
        private String end;
        private String advanceTime;

        private Request() {
        }

        public Request(String jobId) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, "jobId");
        }

        public String getJobId() {
            return jobId;
        }

        public boolean getCalcInterim() {
            return calcInterim;
        }

        public void setCalcInterim(boolean calcInterim) {
            this.calcInterim = calcInterim;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        public String getAdvanceTime() { return advanceTime; }

        public void setAdvanceTime(String advanceTime) {
            this.advanceTime = advanceTime;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            calcInterim = in.readBoolean();
            start = in.readOptionalString();
            end = in.readOptionalString();
            advanceTime = in.readOptionalString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            out.writeBoolean(calcInterim);
            out.writeOptionalString(start);
            out.writeOptionalString(end);
            out.writeOptionalString(advanceTime);
        }
    }

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, PostDataFlushAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends AcknowledgedResponse {

        private Response() {
        }

        private Response(boolean acknowledged) {
            super(acknowledged);
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        // NORELEASE This should be a master node operation that updates the job's state
        private final AutodetectProcessManager processManager;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ThreadPool threadPool, ActionFilters actionFilters,
                               IndexNameExpressionResolver indexNameExpressionResolver, AutodetectProcessManager processManager) {
            super(settings, PostDataFlushAction.NAME, false, threadPool, transportService, actionFilters,
                    indexNameExpressionResolver, PostDataFlushAction.Request::new);

            this.processManager = processManager;
        }

        @Override
        protected final void doExecute(PostDataFlushAction.Request request, ActionListener<PostDataFlushAction.Response> listener) {

            TimeRange timeRange = TimeRange.builder().startTime(request.getStart()).endTime(request.getEnd()).build();
            InterimResultsParams params = InterimResultsParams.builder()
                                            .calcInterim(request.getCalcInterim())
                                            .forTimeRange(timeRange)
                                            .advanceTime(request.getAdvanceTime())
                                            .build();

            processManager.flushJob(request.getJobId(), params);
            listener.onResponse(new Response(true));
        }
    }
}
 

