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
package org.elasticsearch.xpack.prelert.job.condition;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentParser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

/**
 * A class that describes a condition.
 * The {@linkplain Operator} enum defines the available
 * comparisons a condition can use.
 */
public class Condition extends ToXContentToBytes implements Writeable {
    public static final ParseField CONDITION_FIELD = new ParseField("condition");
    public static final ParseField FILTER_VALUE_FIELD = new ParseField("value");

    public static final ConstructingObjectParser<Condition, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            CONDITION_FIELD.getPreferredName(), a -> new Condition((Operator) a[0], (String) a[1]));

    static {
        PARSER.declareField(ConstructingObjectParser.constructorArg(), p -> {
            if (p.currentToken() == XContentParser.Token.VALUE_STRING) {
                return Operator.fromString(p.text());
            }
            throw new IllegalArgumentException("Unsupported token [" + p.currentToken() + "]");
        }, Operator.OPERATOR_FIELD, ValueType.STRING);
        PARSER.declareField(ConstructingObjectParser.constructorArg(), p -> {
            if (p.currentToken() == XContentParser.Token.VALUE_STRING) {
                return p.text();
            }
            if (p.currentToken() == XContentParser.Token.VALUE_NULL) {
                return null;
            }
            throw new IllegalArgumentException("Unsupported token [" + p.currentToken() + "]");
        }, FILTER_VALUE_FIELD, ValueType.STRING_OR_NULL);
    }

    private final Operator op;
    private final String filterValue;

    public Condition(StreamInput in) throws IOException {
        op = Operator.readFromStream(in);
        filterValue = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        op.writeTo(out);
        out.writeOptionalString(filterValue);
    }

    @JsonCreator
    public Condition(@JsonProperty(value = "operator") Operator op, @JsonProperty(value = "value") String filterValue) {
        if (filterValue == null) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NULL),
                    ErrorCodes.CONDITION_INVALID_ARGUMENT);
        }

        if (op.expectsANumericArgument()) {
            try {
                Double.parseDouble(filterValue);
            } catch (NumberFormatException nfe) {
                String msg = Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NUMBER, filterValue);
                throw ExceptionsHelper.parseException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
            }
        } else {
            try {
                Pattern.compile(filterValue);
            } catch (PatternSyntaxException e) {
                String msg = Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_REGEX, filterValue);
                throw ExceptionsHelper.parseException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
            }
        }
        this.op = op;
        this.filterValue = filterValue;
    }

    public Operator getOperator() {
        return op;
    }

    public String getValue() {
        return filterValue;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Operator.OPERATOR_FIELD.getPreferredName(), op.getName());
        builder.field(FILTER_VALUE_FIELD.getPreferredName(), filterValue);
        builder.endObject();
        return builder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, filterValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Condition other = (Condition) obj;
        return Objects.equals(this.op, other.op) &&
                Objects.equals(this.filterValue, other.filterValue);
    }
}
