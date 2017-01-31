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
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.cluster.AbstractDiffable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

public class Datafeed extends AbstractDiffable<Datafeed> implements ToXContent {

    private static final ParseField CONFIG_FIELD = new ParseField("config");
    private static final ParseField STATUS_FIELD = new ParseField("status");

    // Used for QueryPage
    public static final ParseField RESULTS_FIELD = new ParseField("datafeeds");

    public static final ConstructingObjectParser<Datafeed, Void> PARSER = new ConstructingObjectParser<>("datafeed",
            a -> new Datafeed(((DatafeedConfig.Builder) a[0]).build(), (DatafeedStatus) a[1]));

    static {
        PARSER.declareObject(ConstructingObjectParser.constructorArg(), DatafeedConfig.PARSER, CONFIG_FIELD);
        PARSER.declareField(ConstructingObjectParser.constructorArg(), (p, c) -> DatafeedStatus.fromString(p.text()), STATUS_FIELD,
                ObjectParser.ValueType.STRING);
    }

    private final DatafeedConfig config;
    private final DatafeedStatus status;

    public Datafeed(DatafeedConfig config, DatafeedStatus status) {
        this.config = config;
        this.status = status;
    }

    public Datafeed(StreamInput in) throws IOException {
        this.config = new DatafeedConfig(in);
        this.status = DatafeedStatus.fromStream(in);
    }

    public String getId() {
        return config.getId();
    }

    public String getJobId() {
        return config.getJobId();
    }

    public DatafeedConfig getConfig() {
        return config;
    }

    public DatafeedStatus getStatus() {
        return status;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        config.writeTo(out);
        status.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(CONFIG_FIELD.getPreferredName(), config);
        builder.field(STATUS_FIELD.getPreferredName(), status);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Datafeed that = (Datafeed) o;
        return Objects.equals(config, that.config) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(config, status);
    }

    // Class already extends from AbstractDiffable, so copied from ToXContentToBytes#toString()
    @Override
    public final String toString() {
        return Strings.toString(this);
    }
}
