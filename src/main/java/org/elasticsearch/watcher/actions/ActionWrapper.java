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

package org.elasticsearch.watcher.actions;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.actions.throttler.ActionThrottler;
import org.elasticsearch.watcher.actions.throttler.Throttler;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.license.LicenseService;
import org.elasticsearch.watcher.support.WatcherDateTimeUtils;
import org.elasticsearch.watcher.support.clock.Clock;
import org.elasticsearch.watcher.transform.ExecutableTransform;
import org.elasticsearch.watcher.transform.Transform;
import org.elasticsearch.watcher.transform.TransformRegistry;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;

/**
 *
 */
public class ActionWrapper implements ToXContent {

    private String id;
    private final @Nullable ExecutableTransform transform;
    private final ActionThrottler throttler;
    private final ExecutableAction action;

    public ActionWrapper(String id, ExecutableAction action) {
        this(id, null, null, action);
    }

    public ActionWrapper(String id, ActionThrottler throttler, @Nullable ExecutableTransform transform, ExecutableAction action) {
        this.id = id;
        this.throttler = throttler;
        this.transform = transform;
        this.action = action;
    }

    public String id() {
        return id;
    }

    public ExecutableTransform transform() {
        return transform;
    }

    public Throttler throttler() {
        return throttler;
    }

    public ExecutableAction action() {
        return action;
    }

    public ActionWrapper.Result execute(WatchExecutionContext ctx) throws IOException {
        ActionWrapper.Result result = ctx.actionsResults().get(id);
        if (result != null) {
            return result;
        }
        if (!ctx.skipThrottling(id)) {
            Throttler.Result throttleResult = throttler.throttle(id, ctx);
            if (throttleResult.throttle()) {
                return new ActionWrapper.Result(id, new Action.Result.Throttled(action.type(), throttleResult.reason()));
            }
        }
        Payload payload = ctx.transformedPayload();
        Transform.Result transformResult = null;
        if (transform != null) {
            try {
                transformResult = transform.execute(ctx, payload);
                payload = transformResult.payload();
            } catch (Exception e) {
                action.logger.error("failed to execute action [{}/{}]. failed to transform payload.", e, ctx.watch().id(), id);
                return new ActionWrapper.Result(id, new Action.Result.Failure(action.type(), "Failed to transform payload. error: " + ExceptionsHelper.detailedMessage(e)));
            }
        }
        try {
            Action.Result actionResult = action.execute(id, ctx, payload);
            return new ActionWrapper.Result(id, transformResult, actionResult);
        } catch (Exception e) {
            action.logger.error("failed to execute action [{}/{}]", e, ctx.watch().id(), id);
            return new ActionWrapper.Result(id, new Action.Result.Failure(action.type(), ExceptionsHelper.detailedMessage(e)));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionWrapper that = (ActionWrapper) o;

        if (!id.equals(that.id)) return false;
        if (transform != null ? !transform.equals(that.transform) : that.transform != null) return false;
        return action.equals(that.action);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (transform != null ? transform.hashCode() : 0);
        result = 31 * result + action.hashCode();
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        TimeValue throttlePeriod = throttler.throttlePeriod();
        if (throttlePeriod != null) {
            builder.field(Throttler.Field.THROTTLE_PERIOD.getPreferredName(), throttlePeriod.getMillis());
        }
        if (transform != null) {
            builder.startObject(Transform.Field.TRANSFORM.getPreferredName())
                    .field(transform.type(), transform, params)
                    .endObject();
        }
        builder.field(action.type(), action, params);
        return builder.endObject();
    }

    static ActionWrapper parse(String watchId, String actionId, XContentParser parser,
                               ActionRegistry actionRegistry, TransformRegistry transformRegistry,
                               Clock clock, LicenseService licenseService) throws IOException {

        assert parser.currentToken() == XContentParser.Token.START_OBJECT;

        ExecutableTransform transform = null;
        TimeValue throttlePeriod = null;
        ExecutableAction action = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else {
                if (Transform.Field.TRANSFORM.match(currentFieldName)) {
                    transform = transformRegistry.parse(watchId, parser);
                } else if (Throttler.Field.THROTTLE_PERIOD.match(currentFieldName)) {
                    try {
                        throttlePeriod = WatcherDateTimeUtils.parseTimeValue(parser, null);
                    } catch (WatcherDateTimeUtils.ParseException pe) {
                        throw new ActionException("could not parse action [{}/{}]. failed to parse field [{}] as time value", pe, watchId, actionId, currentFieldName);
                    }
                } else {
                    // it's the type of the action
                    ActionFactory actionFactory = actionRegistry.factory(currentFieldName);
                    if (actionFactory == null) {
                        throw new ActionException("could not parse action [{}/{}]. unknown action type [{}]", watchId, actionId, currentFieldName);
                    }
                    action = actionFactory.parseExecutable(watchId, actionId, parser);
                }
            }
        }
        if (action == null) {
            throw new ActionException("could not parse watch action [{}/{}]. missing action type", watchId, actionId);
        }

        ActionThrottler throttler = new ActionThrottler(clock, throttlePeriod, licenseService);
        return new ActionWrapper(actionId, throttler, transform, action);
    }

    public static class Result implements ToXContent {

        private final String id;
        private final @Nullable Transform.Result transform;
        private final Action.Result action;

        public Result(String id, Action.Result action) {
            this(id, null, action);
        }

        public Result(String id, @Nullable Transform.Result transform, Action.Result action) {
            this.id = id;
            this.transform = transform;
            this.action = action;
        }

        public String id() {
            return id;
        }

        public Transform.Result transform() {
            return transform;
        }

        public Action.Result action() {
            return action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Result result = (Result) o;

            if (!id.equals(result.id)) return false;
            if (transform != null ? !transform.equals(result.transform) : result.transform != null) return false;
            return action.equals(result.action);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (transform != null ? transform.hashCode() : 0);
            result = 31 * result + action.hashCode();
            return result;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(Field.ID.getPreferredName(), id);
            if (transform != null) {
                builder.startObject(Transform.Field.TRANSFORM.getPreferredName())
                        .field(transform.type(), transform, params)
                        .endObject();
            }
            builder.field(action.type(), action, params);
            return builder.endObject();
        }
    }

    interface Field {
        ParseField ID = new ParseField("id");
    }
}
