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

package org.elasticsearch.xpack;

import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 *
 */
public interface XPackFeatureSet {

    String name();

    String description();

    boolean available();

    boolean enabled();

    Usage usage();

    abstract class Usage implements ToXContent, NamedWriteable {

        private static final String AVAILABLE_XFIELD = "available";
        private static final String ENABLED_XFIELD = "enabled";

        protected final String name;
        protected final boolean available;
        protected final boolean enabled;

        public Usage(StreamInput input) throws IOException {
            this(input.readString(), input.readBoolean(), input.readBoolean());
        }

        public Usage(String name, boolean available, boolean enabled) {
            this.name = name;
            this.available = available;
            this.enabled = enabled;
        }

        public String name() {
            return name;
        }

        public boolean available() {
            return available;
        }

        public boolean enabled() {
            return enabled;
        }

        @Override
        public String getWriteableName() {
            return name;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeBoolean(available);
            out.writeBoolean(enabled);
        }

        @Override
        public final XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            innerXContent(builder, params);
            return builder.endObject();
        }

        protected void innerXContent(XContentBuilder builder, Params params) throws IOException {
            builder.field(AVAILABLE_XFIELD, available);
            builder.field(ENABLED_XFIELD, enabled);
        }
    }

}
