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

#include <model/CDetectionRule.h>

#include <model/CDataGatherer.h>

namespace ml
{
namespace model
{

CDetectionRule::CDetectionRule(void)
    : m_Action(E_FILTER_RESULTS),
      m_Conditions(),
      m_ConditionsConnective(E_OR),
      m_TargetFieldName(),
      m_TargetFieldValue()
{
    m_Conditions.reserve(1);
}

void CDetectionRule::action(ERuleAction action)
{
    m_Action = action;
}

void CDetectionRule::conditionsConnective(EConditionsConnective connective)
{
    m_ConditionsConnective = connective;
}

void CDetectionRule::addCondition(const CRuleCondition &condition)
{
    m_Conditions.push_back(condition);
}

void CDetectionRule::targetFieldName(const std::string &targetFieldName)
{
    m_TargetFieldName = targetFieldName;
}

void CDetectionRule::targetFieldValue(const std::string &targetFieldValue)
{
    m_TargetFieldValue = targetFieldValue;
}

bool CDetectionRule::apply(ERuleAction action,
                           const CModel &model,
                           model_t::EFeature feature,
                           model_t::CResultType resultType,
                           const std::string &partitionFieldValue,
                           std::size_t pid,
                           std::size_t cid,
                           core_t::TTime time) const
{
    if (m_Action != action)
    {
        return false;
    }

    if (this->isInScope(model, partitionFieldValue, pid, cid) == false)
    {
        return false;
    }

    for (std::size_t i = 0; i < m_Conditions.size(); ++i)
    {
        bool conditionResult = m_Conditions[i].test(
                model, feature, resultType, !m_TargetFieldName.empty(), pid, cid, time);
        switch (m_ConditionsConnective)
        {
            case E_OR:
                if (conditionResult == true)
                {
                    return true;
                }
                break;
            case E_AND:
                if (conditionResult == false)
                {
                    return false;
                }
                break;
        }
    }

    switch (m_ConditionsConnective)
    {
        case E_OR:
            return false;
        case E_AND:
            return true;
    }
    return false;
}

bool CDetectionRule::isInScope(const CModel &model,
                               const std::string &partitionFieldValue,
                               std::size_t pid,
                               std::size_t cid) const
{
    if (m_TargetFieldName.empty() || m_TargetFieldValue.empty())
    {
        return true;
    }

    const CDataGatherer &gatherer = model.dataGatherer();
    if (m_TargetFieldName == gatherer.partitionFieldName())
    {
        return m_TargetFieldValue == partitionFieldValue;
    }
    else if (m_TargetFieldName == gatherer.personFieldName())
    {
        return m_TargetFieldValue == gatherer.personName(pid);
    }
    else if (m_TargetFieldName == gatherer.attributeFieldName())
    {
        return m_TargetFieldValue == gatherer.attributeName(cid);
    }
    else
    {
        LOG_ERROR("Unexpected targetFieldName = " << m_TargetFieldName);
    }
    return false;
}

std::string CDetectionRule::print(void) const
{
    std::string result = this->print(m_Action);
    if (m_TargetFieldName.empty() == false)
    {
        result += " (" + m_TargetFieldName;
        if (m_TargetFieldValue.empty() == false)
        {
            result += ":" + m_TargetFieldValue;
        }
        result += ")";
    }
    result += " IF ";
    for (std::size_t i = 0; i < m_Conditions.size(); ++i)
    {
        result += m_Conditions[i].print();
        if (i < m_Conditions.size() - 1)
        {
            result += " ";
            result += this->print(m_ConditionsConnective);
            result += " ";
        }
    }
    return result;
}

std::string CDetectionRule::print(ERuleAction ruleAction) const
{
    switch (ruleAction)
    {
        case E_FILTER_RESULTS:
            return "FILTER_RESULTS";
    }
    return std::string();
}

std::string CDetectionRule::print(EConditionsConnective connective) const
{
    switch (connective)
    {
        case E_AND:
            return "AND";
        case E_OR:
            return "OR";
    }
    return std::string();
}

}
}
