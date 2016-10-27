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
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.elasticsearch.ElasticsearchParseException;
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
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class RuleCondition extends ToXContentToBytes implements Writeable {
    public static final ParseField CONDITION_TYPE_FIELD = new ParseField("conditionType");
    public static final ParseField RULE_CONDITION_FIELD = new ParseField("ruleCondition");
    public static final ParseField FIELD_NAME_FIELD = new ParseField("fieldName");
    public static final ParseField FIELD_VALUE_FIELD = new ParseField("fieldValue");
    public static final ParseField VALUE_LIST_FIELD = new ParseField("valueList");

    public static final ConstructingObjectParser<RuleCondition, ParseFieldMatcherSupplier> PARSER =
            new ConstructingObjectParser<>(RULE_CONDITION_FIELD.getPreferredName(),
                    a -> new RuleCondition((RuleConditionType) a[0], (String) a[1], (String) a[2], (Condition) a[3], (String) a[4]));

    static {
        PARSER.declareField(ConstructingObjectParser.constructorArg(), p -> {
            if (p.currentToken() == XContentParser.Token.VALUE_STRING) {
                return RuleConditionType.forString(p.text());
            }
            throw new IllegalArgumentException("Unsupported token [" + p.currentToken() + "]");
        }, CONDITION_TYPE_FIELD, ValueType.STRING);
        PARSER.declareStringOrNull(ConstructingObjectParser.optionalConstructorArg(), FIELD_NAME_FIELD);
        PARSER.declareStringOrNull(ConstructingObjectParser.optionalConstructorArg(), FIELD_VALUE_FIELD);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), Condition.PARSER, Condition.CONDITION_FIELD);
        PARSER.declareStringOrNull(ConstructingObjectParser.optionalConstructorArg(), VALUE_LIST_FIELD);
    }

    private final RuleConditionType conditionType;
    private final String fieldName;
    private final String fieldValue;
    private final Condition condition;
    private final String valueList;

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
    public RuleCondition(@JsonProperty("conditionType") RuleConditionType conditionType, @JsonProperty("fieldName") String fieldName,
            @JsonProperty("fieldValue") String fieldValue, @JsonProperty("condition") Condition condition,
            @JsonProperty("valueList") String valueList) {
        this.conditionType = conditionType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.condition = condition;
        this.valueList = valueList;

        verifyFieldsBoundToType(this);
        verifyFieldValueRequiresFieldName(this);
    }

    public RuleCondition(RuleCondition ruleCondition) {
        this.conditionType = ruleCondition.conditionType;
        this.fieldName = ruleCondition.fieldName;
        this.fieldValue = ruleCondition.fieldValue;
        this.condition = ruleCondition.condition;
        this.valueList = ruleCondition.valueList;
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

    /**
     * The field name for which the rule applies. Can be null, meaning rule
     * applies to all results.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * The value of the field name for which the rule applies. When set, the
     * rule applies only to the results that have the fieldName/fieldValue pair.
     * When null, the rule applies to all values for of the specified field
     * name. Only applicable when fieldName is not null.
     */
    public String getFieldValue() {
        return fieldValue;
    }

    public Condition getCondition() {
        return condition;
    }

    /**
     * The unique identifier of a list. Required when the rule type is
     * categorical. Should be null for all other types.
     */
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
        return new RuleCondition(RuleConditionType.CATEGORICAL, fieldName, null, null, valueList);
    }

    private static void verifyFieldsBoundToType(RuleCondition ruleCondition) throws ElasticsearchParseException {
        switch (ruleCondition.getConditionType()) {
        case CATEGORICAL:
            verifyCategorical(ruleCondition);
            break;
        case NUMERICAL_ACTUAL:
        case NUMERICAL_TYPICAL:
        case NUMERICAL_DIFF_ABS:
            verifyNumerical(ruleCondition);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    private static void verifyCategorical(RuleCondition ruleCondition) throws ElasticsearchParseException {
        checkCategoricalHasNoField(Condition.CONDITION_FIELD.getPreferredName(), ruleCondition.getCondition());
        checkCategoricalHasNoField(RuleCondition.FIELD_VALUE_FIELD.getPreferredName(), ruleCondition.getFieldValue());
        checkCategoricalHasField(RuleCondition.VALUE_LIST_FIELD.getPreferredName(), ruleCondition.getValueList());
    }

    private static void checkCategoricalHasNoField(String fieldName, Object fieldValue) throws ElasticsearchParseException {
        if (fieldValue != null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_INVALID_OPTION, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }

    private static void checkCategoricalHasField(String fieldName, Object fieldValue) throws ElasticsearchParseException {
        if (fieldValue == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_CATEGORICAL_MISSING_OPTION, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void verifyNumerical(RuleCondition ruleCondition) throws ElasticsearchParseException {
        checkNumericalHasNoField(RuleCondition.VALUE_LIST_FIELD.getPreferredName(), ruleCondition.getValueList());
        checkNumericalHasField(Condition.CONDITION_FIELD.getPreferredName(), ruleCondition.getCondition());
        if (ruleCondition.getFieldName() != null && ruleCondition.getFieldValue() == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_WITH_FIELD_NAME_REQUIRES_FIELD_VALUE);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
        checkNumericalConditionOparatorsAreValid(ruleCondition);
    }

    private static void checkNumericalHasNoField(String fieldName, Object fieldValue) throws ElasticsearchParseException {
        if (fieldValue != null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_INVALID_OPTION, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION);
        }
    }

    private static void checkNumericalHasField(String fieldName, Object fieldValue) throws ElasticsearchParseException {
        if (fieldValue == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_MISSING_OPTION, fieldName);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    private static void verifyFieldValueRequiresFieldName(RuleCondition ruleCondition) throws ElasticsearchParseException {
        if (ruleCondition.getFieldValue() != null && ruleCondition.getFieldName() == null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_MISSING_FIELD_NAME,
                    ruleCondition.getFieldValue());
            throw ExceptionsHelper.parseException(msg, ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD);
        }
    }

    static EnumSet<Operator> VALID_CONDITION_OPERATORS = EnumSet.of(Operator.LT, Operator.LTE, Operator.GT, Operator.GTE);

    private static void checkNumericalConditionOparatorsAreValid(RuleCondition ruleCondition) throws ElasticsearchParseException {
        Operator operator = ruleCondition.getCondition().getOperator();
        if (!VALID_CONDITION_OPERATORS.contains(operator)) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_DETECTION_RULE_CONDITION_NUMERICAL_INVALID_OPERATOR, operator);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
        }
    }
}
