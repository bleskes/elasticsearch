
package org.elasticsearch.prelert.job.condition;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enum representing logical comparisons on doubles
 */
public enum Operator {
    EQ {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) == 0;
        }
    },
    GT {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) > 0;
        }
    },
    GTE {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) >= 0;
        }
    },
    LT {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) < 0;
        }
    },
    LTE {
        @Override
        public boolean test(double lhs, double rhs) {
            return Double.compare(lhs, rhs) <= 0;
        }
    },
    MATCH {
        @Override
        public boolean match(Pattern pattern, String field) {
            Matcher match = pattern.matcher(field);
            return match.matches();
        }

        @Override
        public boolean expectsANumericArgument() {
            return false;
        }
    },
    NONE;

    public boolean test(double lhs, double rhs) {
        return false;
    }

    public boolean match(Pattern pattern, String field) {
        return false;
    }

    public boolean expectsANumericArgument() {
        return true;
    }

    @JsonCreator
    public static Operator fromString(String name) throws UnknownOperatorException {
        Set<Operator> all = EnumSet.allOf(Operator.class);

        String ucName = name.toUpperCase();
        for (Operator type : all) {
            if (type.toString().equals(ucName)) {
                return type;
            }
        }

        throw new UnknownOperatorException(name);
    }
}
