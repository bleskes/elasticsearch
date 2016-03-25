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

package org.elasticsearch.marvel.action;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.util.Objects;

public class MonitoringBulkResponse extends ActionResponse {

    private long tookInMillis;
    private Error error;

    MonitoringBulkResponse() {
    }

    public MonitoringBulkResponse(long tookInMillis) {
        this(tookInMillis, null);
    }

    public MonitoringBulkResponse(long tookInMillis, Error error) {
        this.tookInMillis = tookInMillis;
        this.error = error;
    }

    public TimeValue getTook() {
        return new TimeValue(tookInMillis);
    }

    public long getTookInMillis() {
        return tookInMillis;
    }

    /**
     * Returns HTTP status
     * <ul>
     * <li>{@link RestStatus#OK} if monitoring bulk request was successful</li>
     * <li>{@link RestStatus#INTERNAL_SERVER_ERROR} if monitoring bulk request was partially successful or failed completely</li>
     * </ul>
     */
    public RestStatus status() {
        return error == null ? RestStatus.OK : RestStatus.INTERNAL_SERVER_ERROR;
    }

    public Error getError() {
        return error;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        tookInMillis = in.readVLong();
        error = in.readOptionalWriteable(Error::new);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVLong(tookInMillis);
        out.writeOptionalWriteable(error);
    }

    public static class Error implements Writeable<Error>, ToXContent {

        private final Throwable cause;
        private final RestStatus status;

        public Error(Throwable t) {
            cause = Objects.requireNonNull(t);
            status = ExceptionsHelper.status(t);
        }

        Error(StreamInput in) throws IOException {
            this(in.<Throwable>readThrowable());
        }

        /**
         * The failure message.
         */
        public String getMessage() {
            return this.cause.toString();
        }

        /**
         * The rest status.
         */
        public RestStatus getStatus() {
            return this.status;
        }

        /**
         * The actual cause of the failure.
         */
        public Throwable getCause() {
            return cause;
        }

        @Override
        public Error readFrom(StreamInput in) throws IOException {
            return new Error(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeThrowable(getCause());
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            ElasticsearchException.toXContent(builder, params, cause);
            builder.endObject();
            return builder;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Error [");
            sb.append("cause=").append(cause);
            sb.append(", status=").append(status);
            sb.append(']');
            return sb.toString();
        }
    }
}
