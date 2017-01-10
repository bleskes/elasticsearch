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
#include <api/CDetectionRulesJsonParser.h>

#include <core/CLogger.h>
#include <core/CStringUtils.h>

namespace ml
{
namespace api
{

namespace
{
const std::string RULE_ACTION("ruleAction");
const std::string FILTER_RESULTS("FILTER_RESULTS");
const std::string CONDITIONS_CONNECTIVE("conditionsConnective");
const std::string AND("AND");
const std::string OR("OR");
const std::string RULE_CONDITIONS("ruleConditions");
const std::string TARGET_FIELD_NAME("targetFieldName");
const std::string TARGET_FIELD_VALUE("targetFieldValue");
const std::string CONDITION_TYPE("conditionType");
const std::string CATEGORICAL("CATEGORICAL");
const std::string NUMERICAL_ACTUAL("NUMERICAL_ACTUAL");
const std::string NUMERICAL_TYPICAL("NUMERICAL_TYPICAL");
const std::string NUMERICAL_DIFF_ABS("NUMERICAL_DIFF_ABS");
const std::string CONDITION("condition");
const std::string OPERATOR("operator");
const std::string LT("LT");
const std::string LTE("LTE");
const std::string GT("GT");
const std::string GTE("GTE");
const std::string VALUE("value");
const std::string FIELD_NAME("fieldName");
const std::string FIELD_VALUE("fieldValue");
const std::string VALUE_LIST("valueList");
}

CDetectionRulesJsonParser::CDetectionRulesJsonParser(TStrPatternSetUMap &listsByIdMap)
        : m_ListsByIdMap(listsByIdMap)
{
}

bool CDetectionRulesJsonParser::parseRules(const std::string &json, TDetectionRuleVec &rules)
{
    LOG_DEBUG("Parsing detection rules");

    rules.clear();
    rapidjson::Document doc;
    if (doc.Parse<0>(json.c_str()).HasParseError())
    {
        LOG_ERROR("An error occurred while parsing detection rules from JSON: "
                + std::string(doc.GetParseError()));
        return false;
    }

    if (!doc.IsArray())
    {
        LOG_ERROR("Could not parse detection rules from non-array JSON object: " << json);
        return false;
    }

    if (doc.Empty())
    {
        return true;
    }

    rules.resize(doc.Size());

    for (unsigned int i = 0; i < doc.Size(); ++i)
    {
        if (!doc[i].IsObject())
        {
            LOG_ERROR("Could not parse detection rules: "
                    << "expected detection rules array to contain objects. JSON: " << json);
            rules.clear();
            return false;
        }

        model::CDetectionRule &rule = rules[i];

        rapidjson::Value &ruleObject = doc[i];

        bool isValid = true;

        // Required fields
        isValid &= parseRuleAction(ruleObject, rule);
        isValid &= parseConditionsConnective(ruleObject, rule);
        isValid &= parseRuleConditions(ruleObject, rule);

        if (isValid == false)
        {
            LOG_ERROR("Failed to parse detection rules from JSON: " << json);
            rules.clear();
            return false;
        }

        // Optional fields
        if (hasStringMember(ruleObject, TARGET_FIELD_NAME))
        {
            rule.targetFieldName(ruleObject[TARGET_FIELD_NAME.c_str()].GetString());
        }
        if (hasStringMember(ruleObject, TARGET_FIELD_VALUE))
        {
            rule.targetFieldValue(ruleObject[TARGET_FIELD_VALUE.c_str()].GetString());
        }
    }

    return true;
}

bool CDetectionRulesJsonParser::hasStringMember(const rapidjson::Value &object,
                                                const std::string &name)
{
    const char *nameAsCStr = name.c_str();
    return object.HasMember(nameAsCStr) && object[nameAsCStr].IsString();
}

bool CDetectionRulesJsonParser::hasArrayMember(const rapidjson::Value &object,
                                               const std::string &name)
{
    const char *nameAsCStr = name.c_str();
    return object.HasMember(nameAsCStr) && object[nameAsCStr].IsArray();
}

bool CDetectionRulesJsonParser::parseRuleAction(const rapidjson::Value &ruleObject,
                                                model::CDetectionRule &rule)
{
    if (!hasStringMember(ruleObject, RULE_ACTION))
    {
        LOG_ERROR("Missing rule field: " << RULE_ACTION);
        return false;
    }

    const std::string &ruleAction = ruleObject[RULE_ACTION.c_str()].GetString();
    if (ruleAction == FILTER_RESULTS)
    {
        rule.action(model::CDetectionRule::E_FILTER_RESULTS);
    }
    else
    {
        LOG_ERROR("Invalid ruleAction: " << ruleAction);
        return false;
    }
    return true;
}

bool CDetectionRulesJsonParser::parseConditionsConnective(const rapidjson::Value &ruleObject,
                                                          model::CDetectionRule &rule)
{
    if (!hasStringMember(ruleObject, CONDITIONS_CONNECTIVE))
    {
        LOG_ERROR("Missing rule field: " << CONDITIONS_CONNECTIVE);
        return false;
    }

    const std::string &connective = ruleObject[CONDITIONS_CONNECTIVE.c_str()].GetString();
    if (connective == OR)
    {
        rule.conditionsConnective(model::CDetectionRule::E_OR);
    }
    else if (connective == AND)
    {
        rule.conditionsConnective(model::CDetectionRule::E_AND);
    }
    else
    {
        LOG_ERROR("Invalid conditionsConnective: " << connective);
        return false;
    }
    return true;
}

bool CDetectionRulesJsonParser::parseRuleConditions(const rapidjson::Value &ruleObject,
                                                    model::CDetectionRule &rule)
{
    if (!hasArrayMember(ruleObject, RULE_CONDITIONS))
    {
        LOG_ERROR("Missing rule field: " << RULE_CONDITIONS);
        return false;
    }

    const rapidjson::Value &array = ruleObject[RULE_CONDITIONS.c_str()];
    if (array.Empty())
    {
        LOG_ERROR("At least one ruleCondition is required");
        return false;
    }

    for (unsigned int i = 0; i < array.Size(); ++i)
    {
        model::CRuleCondition ruleCondition;
        const rapidjson::Value &conditionObject = array[i];

        if (!conditionObject.IsObject())
        {
            LOG_ERROR("Unexpected ruleCondition type: array ruleConditions is expected to contain objects");
            return false;
        }

        bool isValid = true;

        // Required fields
        isValid &= parseRuleConditionType(conditionObject, ruleCondition);
        if (ruleCondition.isNumerical())
        {
            isValid &= parseCondition(conditionObject, ruleCondition);
        }
        else if (ruleCondition.isCategorical())
        {
            isValid &= this->parseValueList(conditionObject, ruleCondition);
        }

        if (isValid == false)
        {
            return false;
        }

        // Optional fields
        if (hasStringMember(conditionObject, FIELD_NAME))
        {
            ruleCondition.fieldName(conditionObject[FIELD_NAME.c_str()].GetString());
        }
        if (hasStringMember(conditionObject, FIELD_VALUE))
        {
            ruleCondition.fieldValue(conditionObject[FIELD_VALUE.c_str()].GetString());
        }

        rule.addCondition(ruleCondition);
    }
    return true;
}

bool CDetectionRulesJsonParser::parseValueList(const rapidjson::Value &conditionObject,
                                               model::CRuleCondition &ruleCondition)
{
    if (!hasStringMember(conditionObject, VALUE_LIST))
    {
        LOG_ERROR("Missing ruleCondition field: " << VALUE_LIST);
        return false;
    }
    const std::string &listId = conditionObject[VALUE_LIST.c_str()].GetString();
    ruleCondition.valueList(m_ListsByIdMap[listId]);
    return true;
}

bool CDetectionRulesJsonParser::parseRuleConditionType(const rapidjson::Value &ruleConditionObject,
                                                       model::CRuleCondition &ruleCondition)
{
    if (!hasStringMember(ruleConditionObject, CONDITION_TYPE))
    {
        LOG_ERROR("Missing ruleCondition field: " << CONDITION_TYPE);
        return false;
    }

    const std::string &type = ruleConditionObject[CONDITION_TYPE.c_str()].GetString();
    if (type == CATEGORICAL)
    {
        ruleCondition.type(model::CRuleCondition::E_CATEGORICAL);
    }
    else if (type == NUMERICAL_ACTUAL)
    {
        ruleCondition.type(model::CRuleCondition::E_NUMERICAL_ACTUAL);
    }
    else if (type == NUMERICAL_TYPICAL)
    {
        ruleCondition.type(model::CRuleCondition::E_NUMERICAL_TYPICAL);
    }
    else if (type == NUMERICAL_DIFF_ABS)
    {
        ruleCondition.type(model::CRuleCondition::E_NUMERICAL_DIFF_ABS);
    }
    else
    {
        LOG_ERROR("Invalid conditionType: " << type);
        return false;
    }
    return true;
}

bool CDetectionRulesJsonParser::parseCondition(const rapidjson::Value &ruleConditionObject,
                                               model::CRuleCondition &ruleCondition)
{
    if (!ruleConditionObject.HasMember(CONDITION.c_str()))
    {
        LOG_ERROR("Missing ruleCondition field: " << CONDITION);
        return false;
    }
    const rapidjson::Value &conditionObject = ruleConditionObject[CONDITION.c_str()];
    if (!conditionObject.IsObject())
    {
        LOG_ERROR("Unexpected type for condition; object was expected");
        return false;
    }

    return parseConditionOperator(conditionObject, ruleCondition)
            && parseConditionThreshold(conditionObject, ruleCondition);
}

bool CDetectionRulesJsonParser::parseConditionOperator(const rapidjson::Value &conditionObject,
                                                       model::CRuleCondition &ruleCondition)
{
    if (!hasStringMember(conditionObject, OPERATOR))
    {
        LOG_ERROR("Missing condition field: " << OPERATOR);
        return false;
    }

    const std::string &operatorString = conditionObject[OPERATOR.c_str()].GetString();
    if (operatorString == LT)
    {
        ruleCondition.condition().s_Op = model::CRuleCondition::E_LT;
    }
    else if (operatorString == LTE)
    {
        ruleCondition.condition().s_Op = model::CRuleCondition::E_LTE;
    }
    else if (operatorString == GT)
    {
        ruleCondition.condition().s_Op = model::CRuleCondition::E_GT;
    }
    else if (operatorString == GTE)
    {
        ruleCondition.condition().s_Op = model::CRuleCondition::E_GTE;
    }
    else
    {
        LOG_ERROR("Invalid operator value: " << operatorString);
        return false;
    }
    return true;
}

bool CDetectionRulesJsonParser::parseConditionThreshold(const rapidjson::Value &conditionObject,
                                                        model::CRuleCondition &ruleCondition)
{
    if (!hasStringMember(conditionObject, VALUE))
    {
        LOG_ERROR("Missing condition field: " << VALUE);
        return false;
    }

    const std::string valueString = conditionObject[VALUE.c_str()].GetString();
    if (core::CStringUtils::stringToType(valueString, ruleCondition.condition().s_Threshold) == false)
    {
        LOG_ERROR("Invalid operator value: " << valueString);
        return false;
    }
    return true;
}

}
}
