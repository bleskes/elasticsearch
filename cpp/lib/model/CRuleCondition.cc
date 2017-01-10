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

#include <core/CLogger.h>
#include <core/CPatternSet.h>
#include <core/CStringUtils.h>

#include <model/CDataGatherer.h>
#include <model/CRuleCondition.h>


namespace ml
{
namespace model
{

namespace
{
const CModel::TSizeDoublePr1Vec EMPTY_CORRELATED;
const core::CPatternSet EMPTY_LIST;
}

CRuleCondition::SCondition::SCondition(EConditionOperator op, double threshold)
        : s_Op(op),
          s_Threshold(threshold)
{
}

bool CRuleCondition::SCondition::test(double value) const
{
    switch (s_Op)
    {
        case E_LT:
            return value < s_Threshold;
        case E_LTE:
            return value <= s_Threshold;
        case E_GT:
            return value > s_Threshold;
        case E_GTE:
            return value >= s_Threshold;
    }
    return false;
}

CRuleCondition::CRuleCondition(void)
    : m_Type(E_NUMERICAL_ACTUAL),
      m_Condition(E_LT, 0.0),
      m_FieldName(),
      m_FieldValue(),
      m_ValueList(EMPTY_LIST)
{
}

void CRuleCondition::type(ERuleConditionType ruleType)
{
    m_Type = ruleType;
}

void CRuleCondition::fieldName(const std::string &fieldName)
{
    m_FieldName = fieldName;
}

void CRuleCondition::fieldValue(const std::string &fieldValue)
{
    m_FieldValue = fieldValue;
}

CRuleCondition::SCondition &CRuleCondition::condition(void)
{
    return m_Condition;
}

void CRuleCondition::valueList(const core::CPatternSet &valueList)
{
    m_ValueList = TPatternSetCRef(valueList);
}

bool CRuleCondition::isCategorical(void) const
{
    return m_Type == E_CATEGORICAL;
}

bool CRuleCondition::isNumerical(void) const
{
    return !this->isCategorical();
}

bool CRuleCondition::test(const CModel &model,
                          model_t::EFeature feature,
                          model_t::CResultType resultType,
                          bool isScoped,
                          std::size_t pid,
                          std::size_t cid,
                          core_t::TTime time) const
{
    const CDataGatherer &gatherer = model.dataGatherer();
    const std::string &fieldValue = model.isPopulation() ?
            gatherer.attributeName(cid) : gatherer.personName(pid);
    if (m_FieldValue.empty() == false)
    {
        if (isScoped)
        {
            // We need to check the condition for the series indicated by fieldName/fieldValue.
            bool successfullyResolvedId = model.isPopulation() ?
                    gatherer.attributeId(m_FieldValue, cid) : gatherer.personId(m_FieldValue, pid);
            if (successfullyResolvedId == false)
            {
                return false;
            }
        }
        else
        {
            if (m_FieldValue != fieldValue)
            {
                return false;
            }
        }
    }

    if (this->isCategorical())
    {
        return m_ValueList.get().contains(fieldValue);
    }
    return this->checkCondition(model, feature, resultType, pid, cid, time);
}

bool CRuleCondition::checkCondition(const CModel &model,
                                    model_t::EFeature feature,
                                    model_t::CResultType resultType,
                                    std::size_t pid,
                                    std::size_t cid,
                                    core_t::TTime time) const
{
    TDouble1Vec value;
    switch (m_Type)
    {
        case E_CATEGORICAL:
            LOG_ERROR("Should never check numerical condition for categorical rule condition");
            return false;
        case E_NUMERICAL_ACTUAL:
            value = model.currentBucketValue(feature, pid, cid, time);
            break;
        case E_NUMERICAL_TYPICAL:
            value = model.baselineBucketMean(feature, pid, cid, resultType, EMPTY_CORRELATED, time);
            if (value.empty())
            {
                // Means prior is non-informative
                return false;
            }
            break;
        case E_NUMERICAL_DIFF_ABS:
            value = model.currentBucketValue(feature, pid, cid, time);
            TDouble1Vec typical = model.baselineBucketMean(
                    feature, pid, cid, resultType, EMPTY_CORRELATED, time);
            if (typical.empty())
            {
                // Means prior is non-informative
                return false;
            }
            if (value.size() != typical.size())
            {
                LOG_ERROR("Cannot apply rule condition: cannot calculate difference between " <<
                        "actual and typical values due to different dimensions.");
                return false;
            }
            for (std::size_t i = 0; i < value.size(); ++i)
            {
                value[i] = ::fabs(value[i] - typical[i]);
            }
            break;
    }
    if (value.empty())
    {
        LOG_ERROR("Value for rule comparison could not be calculated");
        return false;
    }
    if (value.size() > 1)
    {
        LOG_ERROR("Numerical rules do not support multivariate analysis");
        return false;
    }
    return m_Condition.test(value[0]);
}

std::string CRuleCondition::print(void) const
{
    std::string result = this->print(m_Type);
    if (m_FieldName.empty() == false)
    {
        result += "(" + m_FieldName;
        if (m_FieldValue.empty() == false)
        {
            result += ":" + m_FieldValue;
        }
        result += ")";
    }
    result += " ";
    if (this->isCategorical())
    {
        result += "IN LIST";
    }
    else
    {
        result += this->print(m_Condition.s_Op) + " "
                + core::CStringUtils::typeToString(m_Condition.s_Threshold);
    }
    return result;
}

std::string CRuleCondition::print(ERuleConditionType type) const
{
    switch (type)
    {
        case E_CATEGORICAL:
            return "";
        case E_NUMERICAL_ACTUAL:
            return "ACTUAL";
        case E_NUMERICAL_TYPICAL:
            return "TYPICAL";
        case E_NUMERICAL_DIFF_ABS:
            return "DIFF_ABS";
    }
    return std::string();
}

std::string CRuleCondition::print(EConditionOperator op) const
{
    switch (op)
    {
        case E_LT:
            return "<";
        case E_LTE:
            return "<=";
        case E_GT:
            return ">";
        case E_GTE:
            return ">=";
    }
    return std::string();
}

}
}
