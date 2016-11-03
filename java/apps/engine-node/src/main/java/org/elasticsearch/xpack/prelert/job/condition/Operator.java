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
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

/**
 * Enum representing logical comparisons on doubles
 */
public enum Operator implements Writeable {
    EQ("eq") {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) == 0;
        }
    },
    GT("gt") {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) > 0;
        }
    },
    GTE("gte") {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) >= 0;
        }
    },
    LT("lt") {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) < 0;
        }
    },
    LTE("lte") {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) <= 0;
        }
    },
    MATCH("match") {
        @Override
        public boolean match(Pattern pattern, String field) {
            Matcher match = pattern.matcher(field);
            return match.matches();
        }

        @Override
        public boolean expectsANumericArgument() {
            return false;
        }
    };

    public static final ParseField OPERATOR_FIELD = new ParseField("operator");
    private final String name;

    private Operator(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean test(double lhs, double rhs) {
        return false;
    }

    public boolean match(Pattern pattern, String field) {
        return false;
    }

    public boolean expectsANumericArgument() {
        return true;
    }

    public static Operator fromString(String name) {
        Set<Operator> all = EnumSet.allOf(Operator.class);

        String ucName = name.toUpperCase(Locale.ROOT);
        for (Operator type : all) {
            if (type.toString().equals(ucName)) {
                return type;
            }
        }
        throw ExceptionsHelper.parseException(
                Messages.getMessage(Messages.JOB_CONFIG_CONDITION_UNKNOWN_OPERATOR, name),
                ErrorCodes.CONDITION_UNKNOWN_OPERATOR
                );
    }

    public static Operator readFromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown Operator ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }
}
