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

#ifndef INCLUDED_prelert_model_CDetectionRule_h
#define INCLUDED_prelert_model_CDetectionRule_h

#include <model/CModel.h>
#include <model/CRuleCondition.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <string>
#include <vector>

namespace prelert
{
namespace model
{

//! \brief A rule that dictates an action to be taken when certain conditions occur.
//!
//! DESCRIPTION:\n
//! A rule describes an action to be taken and the conditions under which
//! the action should be taken. A rule has an action and one or more conditions.
//! The conditions are combined according to the rule's connective which can
//! be either OR or AND. A rule can optionally have a target field specified.
//! When such target is not specified, the rule applies to the series that is
//! checked against the rule. When a target is specified, the rule applies to
//! all series that are contained within the target. For example, if the target
//! is the partition field and no targetFieldValue is specified, then if the
//! conditions trigger the rule, the rule will apply to all series within the
//! partition. However, when no target is specified, the rule will trigger only
//! for series that are described in the conditions themselves.
class MODEL_EXPORT CDetectionRule
{
    public:
        typedef std::vector<CRuleCondition> TRuleConditionVec;
        typedef CModel::TDouble1Vec TDouble1Vec;

        enum ERuleAction
        {
            E_FILTER_RESULTS
        };

        enum EConditionsConnective
        {
            E_OR,
            E_AND
        };
    public:

        //! Default constructor.
        //! The rule's action defaults to FILTER_RESULTS and the connective to OR.
        CDetectionRule(void);

        //! Set the rule's action.
        void action(ERuleAction ruleAction);

        //! Set the conditions' connective.
        void conditionsConnective(EConditionsConnective connective);

        //! Add a condition.
        void addCondition(const CRuleCondition &condition);

        //! Set the target field name.
        void targetFieldName(const std::string &targetFieldName);

        //! Set the target field value.
        void targetFieldValue(const std::string &targetFieldValue);

        //! Check whether the rule applies on a series.
        bool apply(ERuleAction action,
                   const CModel &model,
                   model_t::EFeature feature,
                   model_t::CResultType resultType,
                   const std::string &partitionFieldValue,
                   std::size_t pid,
                   std::size_t cid,
                   core_t::TTime time) const;

        //! Pretty-print the rule.
        std::string print(void) const;

    private:
        //! Check whether the given series is in the scope
        //! of the rule's target.
        bool isInScope(const CModel &model,
                       const std::string &partitionFieldValue,
                       std::size_t pid,
                       std::size_t cid) const;

        std::string print(ERuleAction ruleAction) const;
        std::string print(EConditionsConnective connective) const;

    private:
        //! The rule action.
        ERuleAction m_Action;

        //! The conditions that trigger the rule.
        TRuleConditionVec m_Conditions;

        //! The way the rule's conditions are logically connected (i.e. OR, AND).
        EConditionsConnective m_ConditionsConnective;

        //! The optional target field name. Empty when not specified.
        std::string m_TargetFieldName;

        //! The optional target field value. Empty when not specified.
        std::string m_TargetFieldValue;
};
}
}

#endif // INCLUDED_prelert_model_CDetectionRule_h
