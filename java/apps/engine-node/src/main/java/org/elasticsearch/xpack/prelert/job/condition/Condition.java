
package org.elasticsearch.xpack.prelert.job.condition;

import java.util.Objects;

/**
 * A class that describes a condition.
 * The {@linkplain Operator} enum defines the available
 * comparisons a condition can use.
 */
public class Condition {
    private Operator op;
    private String filterValue;

    /**
     * Operation defaults to {@linkplain Operator#NONE}
     * and the filter is an empty string
     *
     * @param
     */
    public Condition() {
        op = Operator.NONE;
    }

    public Condition(Operator op, String filterString) {
        this.op = op;
        filterValue = filterString;
    }

    public Operator getOperator() {
        return op;
    }

    public void setOperator(Operator op) {
        this.op = op;
    }

    public String getValue() {
        return filterValue;
    }

    public void setValue(String value) {
        filterValue = value;
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
