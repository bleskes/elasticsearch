/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

public class PageParams extends ToXContentToBytes implements Writeable {

    public static final ParseField PAGE = new ParseField("page");
    public static final ParseField SKIP = new ParseField("skip");
    public static final ParseField TAKE = new ParseField("take");

    public static final ConstructingObjectParser<PageParams, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            PAGE.getPreferredName(), a -> new PageParams((int) a[0], (int) a[1]));

    public static final int MAX_SKIP_TAKE_SUM = 10000;

    static {
        PARSER.declareInt(ConstructingObjectParser.constructorArg(), SKIP);
        PARSER.declareInt(ConstructingObjectParser.constructorArg(), TAKE);
    }

    private final int skip;
    private final int take;

    public PageParams(StreamInput in) throws IOException {
        this(in.readVInt(), in.readVInt());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(skip);
        out.writeVInt(take);
    }

    public PageParams(int skip, int take) {
        if (skip < 0) {
            throw new IllegalArgumentException("Parameter [" + SKIP.getPreferredName() + "] cannot be < 0");
        }
        if (take < 0) {
            throw new IllegalArgumentException("Parameter [" + TAKE.getPreferredName() + "] cannot be < 0");
        }
        if (skip + take > MAX_SKIP_TAKE_SUM) {
            throw new IllegalArgumentException("The sum of parameters [" + SKIP.getPreferredName() + "] and ["
                    + TAKE.getPreferredName() + "] cannot be higher than " + MAX_SKIP_TAKE_SUM + ".");
        }
        this.skip = skip;
        this.take = take;
    }

    public int getSkip() {
        return skip;
    }

    public int getTake() {
        return take;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(SKIP.getPreferredName(), skip);
        builder.field(TAKE.getPreferredName(), take);
        builder.endObject();
        return builder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skip, take);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PageParams other = (PageParams) obj;
        return Objects.equals(skip, other.skip) &&
                Objects.equals(take, other.take);
    }

}
