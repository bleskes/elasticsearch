
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;

@JsonInclude(Include.NON_NULL)
public class DetectionRule extends ToXContentToBytes implements Writeable {
    public static final ParseField DETECTION_RULE_FIELD = new ParseField("detection_rule");
    public static final ParseField RULE_ACTION_FIELD = new ParseField("rule_action");
    public static final ParseField TARGET_FIELD_NAME_FIELD = new ParseField("target_field_name");
    public static final ParseField TARGET_FIELD_VALUE_FIELD = new ParseField("target_field_value");
    public static final ParseField CONDITIONS_CONNECTIVE_FIELD = new ParseField("conditions_connective");
    public static final ParseField RULE_CONDITIONS_FIELD = new ParseField("rule_conditions");

    public static final ObjectParser<DetectionRule, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(
            DETECTION_RULE_FIELD.getPreferredName(), DetectionRule::new);

    static {
        PARSER.declareField(DetectionRule::setConditionsConnective, p -> {
            if (p.currentToken() == XContentParser.Token.VALUE_STRING) {
                return Connective.fromString(p.text());
            }
            throw new IllegalArgumentException("Unsupported token [" + p.currentToken() + "]");
        }, CONDITIONS_CONNECTIVE_FIELD, ValueType.STRING);
        PARSER.declareObjectArray(DetectionRule::setRuleConditions,
                (parser, parseFieldMatcher) -> RuleCondition.PARSER.apply(parser, parseFieldMatcher), RULE_CONDITIONS_FIELD);
        PARSER.declareString(DetectionRule::setTargetFieldName, TARGET_FIELD_NAME_FIELD);
        PARSER.declareString(DetectionRule::setTargetFieldValue, TARGET_FIELD_VALUE_FIELD);
    }

    private RuleAction ruleAction = RuleAction.FILTER_RESULTS;
    private String targetFieldName;
    private String targetFieldValue;
    private Connective conditionsConnective = Connective.OR;
    private List<RuleCondition> ruleConditions = new ArrayList<>();

    public DetectionRule(StreamInput in) throws IOException {
        ruleAction = RuleAction.FILTER_RESULTS;
        conditionsConnective = Connective.readFromStream(in);
        int size = in.readVInt();
        ruleConditions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ruleConditions.add(new RuleCondition(in));
        }
        targetFieldName = in.readOptionalString();
        targetFieldValue = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        conditionsConnective.writeTo(out);
        out.writeVInt(ruleConditions.size());
        for (RuleCondition condition : ruleConditions) {
            condition.writeTo(out);
        }
        out.writeOptionalString(targetFieldName);
        out.writeOptionalString(targetFieldValue);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(CONDITIONS_CONNECTIVE_FIELD.getPreferredName(), conditionsConnective.getName());
        builder.field(RULE_CONDITIONS_FIELD.getPreferredName(), ruleConditions);
        if (targetFieldName != null) {
            builder.field(TARGET_FIELD_NAME_FIELD.getPreferredName(), targetFieldName);
        }
        if (targetFieldValue != null) {
            builder.field(TARGET_FIELD_VALUE_FIELD.getPreferredName(), targetFieldValue);
        }
        builder.endObject();
        return builder;
    }

    @JsonCreator
    public DetectionRule() {
    }

    public RuleAction getRuleAction() {
        return ruleAction;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldValue(String targetFieldValue) {
        this.targetFieldValue = targetFieldValue;
    }

    public String getTargetFieldValue() {
        return targetFieldValue;
    }

    public void setConditionsConnective(Connective conditionsConnective) {
        this.conditionsConnective = conditionsConnective;
    }

    public Connective getConditionsConnective() {
        return conditionsConnective;
    }

    public void setRuleConditions(List<RuleCondition> ruleConditions) {
        this.ruleConditions = ruleConditions;
    }

    public List<RuleCondition> getRuleConditions() {
        return ruleConditions;
    }

    public Set<String> extractReferencedLists() {
        return ruleConditions.stream().map(RuleCondition::getValueList).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DetectionRule == false) {
            return false;
        }

        DetectionRule other = (DetectionRule) obj;
        return Objects.equals(ruleAction, other.ruleAction) && Objects.equals(targetFieldName, other.targetFieldName)
                && Objects.equals(targetFieldValue, other.targetFieldValue)
                && Objects.equals(conditionsConnective, other.conditionsConnective) && Objects.equals(ruleConditions, other.ruleConditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleAction, targetFieldName, targetFieldValue, conditionsConnective, ruleConditions);
    }
}
