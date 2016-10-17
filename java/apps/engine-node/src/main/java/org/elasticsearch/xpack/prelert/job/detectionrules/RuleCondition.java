
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import java.io.IOException;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class RuleCondition extends ToXContentToBytes implements Writeable {
    public static final ParseField RULE_CONDITION_FIELD = new ParseField("rule_condition");
    public static final ParseField CONDITION_TYPE_FIELD = new ParseField("condition_type");
    public static final ParseField FIELD_NAME_FIELD = new ParseField("field_name");
    public static final ParseField FIELD_VALUE_FIELD = new ParseField("field_value");
    public static final ParseField VALUE_LIST_FIELD = new ParseField("value_list");

    public static final ConstructingObjectParser<RuleCondition, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            RULE_CONDITION_FIELD.getPreferredName(), a -> new RuleCondition((RuleConditionType) a[0]));

    static {
        PARSER.declareField(ConstructingObjectParser.constructorArg(), p -> {
            if (p.currentToken() == XContentParser.Token.VALUE_STRING) {
                return RuleConditionType.forString(p.text());
            }
            throw new IllegalArgumentException("Unsupported token [" + p.currentToken() + "]");
        }, CONDITION_TYPE_FIELD, ValueType.STRING);
        PARSER.declareObject((ruleCondition, condition) -> ruleCondition.setCondition(condition), Condition.PARSER,
                Condition.CONDITION_FIELD);
        PARSER.declareStringOrNull((ruleCondition, fieldName) -> ruleCondition.setFieldName(fieldName), FIELD_NAME_FIELD);
        PARSER.declareStringOrNull((ruleCondition, fieldValue) -> ruleCondition.setFieldValue(fieldValue), FIELD_VALUE_FIELD);
        PARSER.declareStringOrNull((ruleCondition, valueList) -> ruleCondition.setValueList(valueList), VALUE_LIST_FIELD);
    }

    private final RuleConditionType conditionType;

    /**
     * The field name for which the rule applies. Can be null, meaning rule
     * applies to all results.
     */
    private String fieldName;

    /**
     * The value of the field name for which the rule applies. When set, the
     * rule applies only to the results that have the fieldName/fieldValue pair.
     * When null, the rule applies to all values for of the specified field
     * name. Only applicable when fieldName is not null.
     */
    private String fieldValue;

    private Condition condition;

    /**
     * The unique identifier of a list. Required when the rule type is
     * categorical. Should be null for all other types.
     */
    private String valueList;

    public RuleCondition(StreamInput in) throws IOException {
        conditionType = RuleConditionType.readFromStream(in);
        condition = in.readOptionalWriteable(Condition::new);
        fieldName = in.readOptionalString();
        fieldValue = in.readOptionalString();
        valueList = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        conditionType.writeTo(out);
        out.writeOptionalWriteable(condition);
        out.writeOptionalString(fieldName);
        out.writeOptionalString(fieldValue);
        out.writeOptionalString(valueList);
    }

    @JsonCreator
    public RuleCondition(@JsonProperty("condition_type") RuleConditionType conditionType) {
        this.conditionType = conditionType;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(CONDITION_TYPE_FIELD.getPreferredName(), conditionType);
        if (condition != null) {
            builder.field(Condition.CONDITION_FIELD.getPreferredName(), condition);
        }
        if (fieldName != null) {
            builder.field(FIELD_NAME_FIELD.getPreferredName(), fieldName);
        }
        if (fieldValue != null) {
            builder.field(FIELD_VALUE_FIELD.getPreferredName(), fieldValue);
        }
        if (valueList != null) {
            builder.field(VALUE_LIST_FIELD.getPreferredName(), valueList);
        }
        builder.endObject();
        return builder;
    }

    public RuleConditionType getConditionType() {
        return conditionType;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setValueList(String listId) {
        valueList = listId;
    }

    public String getValueList() {
        return valueList;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RuleCondition == false) {
            return false;
        }

        RuleCondition other = (RuleCondition) obj;
        return Objects.equals(conditionType, other.conditionType) && Objects.equals(fieldName, other.fieldName)
                && Objects.equals(fieldValue, other.fieldValue) && Objects.equals(condition, other.condition)
                && Objects.equals(valueList, other.valueList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionType, fieldName, fieldValue, condition, valueList);
    }

    public static RuleCondition createCategorical(String fieldName, String valueList) {
        RuleCondition condition = new RuleCondition(RuleConditionType.CATEGORICAL);
        condition.setFieldName(fieldName);
        condition.setValueList(valueList);
        return condition;
    }
}
