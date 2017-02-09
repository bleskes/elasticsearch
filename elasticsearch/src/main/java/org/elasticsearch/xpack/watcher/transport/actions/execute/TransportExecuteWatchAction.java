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

package org.elasticsearch.xpack.watcher.transport.actions.execute;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.watcher.condition.AlwaysCondition;
import org.elasticsearch.xpack.watcher.execution.ActionExecutionMode;
import org.elasticsearch.xpack.watcher.execution.ExecutionService;
import org.elasticsearch.xpack.watcher.execution.ManualExecutionContext;
import org.elasticsearch.xpack.watcher.history.WatchRecord;
import org.elasticsearch.xpack.watcher.input.simple.SimpleInput;
import org.elasticsearch.xpack.watcher.support.xcontent.WatcherParams;
import org.elasticsearch.xpack.watcher.transport.actions.WatcherTransportAction;
import org.elasticsearch.xpack.watcher.trigger.TriggerEvent;
import org.elasticsearch.xpack.watcher.trigger.TriggerService;
import org.elasticsearch.xpack.watcher.trigger.manual.ManualTriggerEvent;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.elasticsearch.xpack.watcher.watch.Watch;
import org.elasticsearch.xpack.watcher.watch.WatchStore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalArgument;

/**
 * Performs the watch execution operation.
 */
public class TransportExecuteWatchAction extends WatcherTransportAction<ExecuteWatchRequest, ExecuteWatchResponse> {

    private final ExecutionService executionService;
    private final WatchStore watchStore;
    private final Clock clock;
    private final TriggerService triggerService;
    private final Watch.Parser watchParser;

    @Inject
    public TransportExecuteWatchAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                       ThreadPool threadPool, ActionFilters actionFilters,
                                       IndexNameExpressionResolver indexNameExpressionResolver, ExecutionService executionService,
                                       Clock clock, XPackLicenseState licenseState, WatchStore watchStore, TriggerService triggerService,
                                       Watch.Parser watchParser) {
        super(settings, ExecuteWatchAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver,
                licenseState, ExecuteWatchRequest::new);
        this.executionService = executionService;
        this.watchStore = watchStore;
        this.clock = clock;
        this.triggerService = triggerService;
        this.watchParser = watchParser;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected ExecuteWatchResponse newResponse() {
        return new ExecuteWatchResponse();
    }

    @Override
    protected void masterOperation(ExecuteWatchRequest request, ClusterState state, ActionListener<ExecuteWatchResponse> listener)
            throws ElasticsearchException {
        try {
            Watch watch;
            boolean knownWatch;
            if (request.getId() != null) {
                watch = watchStore.get(request.getId());
                if (watch == null) {
                    throw new ResourceNotFoundException("watch [{}] does not exist", request.getId());
                }
                knownWatch = true;
            } else if (request.getWatchSource() != null) {
                assert !request.isRecordExecution();
                watch = watchParser.parse(ExecuteWatchRequest.INLINE_WATCH_ID, false, request.getWatchSource(), request.getXContentType());
                knownWatch = false;
            } else {
                throw illegalArgument("no watch provided");
            }

            String triggerType = watch.trigger().type();
            TriggerEvent triggerEvent = triggerService.simulateEvent(triggerType, watch.id(), request.getTriggerData());

            ManualExecutionContext.Builder ctxBuilder = ManualExecutionContext.builder(watch, knownWatch,
                    new ManualTriggerEvent(triggerEvent.jobName(), triggerEvent), executionService.defaultThrottlePeriod());

            DateTime executionTime = clock.now(DateTimeZone.UTC);
            ctxBuilder.executionTime(executionTime);
            for (Map.Entry<String, ActionExecutionMode> entry : request.getActionModes().entrySet()) {
                ctxBuilder.actionMode(entry.getKey(), entry.getValue());
            }
            if (request.getAlternativeInput() != null) {
                ctxBuilder.withInput(new SimpleInput.Result(new Payload.Simple(request.getAlternativeInput())));
            }
            if (request.isIgnoreCondition()) {
                ctxBuilder.withCondition(AlwaysCondition.RESULT_INSTANCE);
            }
            ctxBuilder.recordExecution(request.isRecordExecution());

            WatchRecord record = executionService.execute(ctxBuilder.build());
            XContentBuilder builder = XContentFactory.jsonBuilder();
            record.toXContent(builder, WatcherParams.builder().hideSecrets(true).debug(request.isDebug()).build());
            ExecuteWatchResponse response = new ExecuteWatchResponse(record.id().value(), builder.bytes(), XContentType.JSON);
            listener.onResponse(response);
        } catch (Exception e) {
            logger.error((Supplier<?>) () -> new ParameterizedMessage("failed to execute [{}]", request.getId()), e);
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(ExecuteWatchRequest request, ClusterState state) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.WRITE, WatchStore.INDEX);
    }


}
