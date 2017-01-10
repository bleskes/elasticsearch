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

#include <model/CAnnotatedProbability.h>

#include <core/CLogger.h>
#include <core/CPersistUtils.h>

#include <maths/COrderings.h>

#include <model/CStringStore.h>

namespace ml
{
namespace model
{
namespace
{

const std::string PROBABILITY_TAG("a");
const std::string ATTRIBUTE_PROBABILITIES_TAG("b");
const std::string INFLUENCE_NAME_TAG("c");
const std::string INFLUENCE_VALUE_TAG("d");
const std::string INFLUENCE_TAG("e");
const std::string CURRENT_BUCKET_VALUE_TAG("f");
const std::string CURRENT_BUCKET_COUNT_TAG("g");
const std::string BASELINE_BUCKET_COUNT_TAG("h");
const std::string BASELINE_BUCKET_MEAN_TAG("i");
const std::string ATTRIBUTE_TAG("j");
const std::string FEATURE_TAG("k");
const std::string DESCRIPTIVE_DATA_TAG("l");
const std::string ANOMALY_TYPE_TAG("m");
const std::string CORRELATED_ATTRIBUTE_TAG("n");

}

SAttributeProbability::SAttributeProbability(void) :
        s_Cid(0),
        s_Probability(1.0),
        s_Type(model_t::CResultType::E_Unconditional),
        s_Feature(model_t::E_IndividualCountByBucketAndPerson)
{
}

SAttributeProbability::SAttributeProbability(std::size_t cid,
                                             const TStrPtr &attribute,
                                             double probability,
                                             model_t::CResultType type,
                                             model_t::EFeature feature,
                                             const TStrPtr1Vec &correlatedAttributes,
                                             const TSizeDoublePr1Vec &correlated) :
        s_Cid(cid),
        s_Attribute(attribute),
        s_Probability(probability),
        s_Type(type),
        s_Feature(feature),
        s_CorrelatedAttributes(correlatedAttributes),
        s_Correlated(correlated)
{
}

bool SAttributeProbability::operator<(const SAttributeProbability &other) const
{
    return maths::COrderings::lexicographical_compare(s_Probability,
                                                      *s_Attribute,
                                                      s_Feature,
                                                      s_Type.asUint(),
                                                      s_Correlated,
                                                      other.s_Probability,
                                                      *other.s_Attribute,
                                                      other.s_Feature,
                                                      other.s_Type.asUint(),
                                                      other.s_Correlated);
}

void SAttributeProbability::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(ATTRIBUTE_TAG, *s_Attribute);
    // We don't persist s_Cid because it isn't used in restored results.
    inserter.insertValue(ANOMALY_TYPE_TAG, s_Type.asUint());
    for (std::size_t i = 0u; i < s_CorrelatedAttributes.size(); ++i)
    {
        inserter.insertValue(CORRELATED_ATTRIBUTE_TAG, *s_CorrelatedAttributes[i]);
    }
    // We don't persist s_Correlated because it isn't used in restored results.
    core::CPersistUtils::persist(PROBABILITY_TAG, s_Probability, inserter);
    core::CPersistUtils::persist(FEATURE_TAG, s_Feature, inserter);
    core::CPersistUtils::persist(DESCRIPTIVE_DATA_TAG, s_DescriptiveData, inserter);
    core::CPersistUtils::persist(CURRENT_BUCKET_VALUE_TAG, s_CurrentBucketValue, inserter);
    core::CPersistUtils::persist(BASELINE_BUCKET_MEAN_TAG, s_BaselineBucketMean, inserter);
}

bool SAttributeProbability::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == ATTRIBUTE_TAG)
        {
            s_Attribute = CStringStore::names().get(traverser.value());
        }
        else if (name == ANOMALY_TYPE_TAG)
        {
            unsigned int type;
            if (!core::CStringUtils::stringToType(traverser.value(), type))
            {
                LOG_ERROR("Failed to restore " << traverser.name() << " / " << traverser.value());
                return false;
            }
            s_Type = model_t::CResultType(type);
        }
        else if (name == CORRELATED_ATTRIBUTE_TAG)
        {
            s_CorrelatedAttributes.push_back(CStringStore::names().get(traverser.value()));
        }
        else if (name == PROBABILITY_TAG)
        {
            if (!core::CPersistUtils::restore(PROBABILITY_TAG, s_Probability, traverser))
            {
                LOG_ERROR("Failed to restore " << traverser.name() << " / " << traverser.value());
                return false;
            }
        }
        else if (name == FEATURE_TAG)
        {
            std::size_t feature;
            if (!core::CPersistUtils::restore(FEATURE_TAG, feature, traverser))
            {
                LOG_ERROR("Failed to restore " << traverser.name() << " / " << traverser.value());
                return false;
            }
            s_Feature =  model_t::EFeature(feature);
        }
        else if (name == DESCRIPTIVE_DATA_TAG)
        {
            typedef std::vector<TSizeDoublePr> TSizeDoublePrVec;
            TSizeDoublePrVec d;
            if (!core::CPersistUtils::restore(DESCRIPTIVE_DATA_TAG, d, traverser))
            {
                LOG_ERROR("Failed to restore " << traverser.name() << " / " << traverser.value());
                return false;
            }
            for (std::size_t i = 0u; i != d.size(); ++i)
            {
                s_DescriptiveData.push_back(TDescriptiveDataDoublePr(
                                                annotated_probability::EDescriptiveData(d[i].first),
                                                d[i].second));
            }
        }
        else if (name == CURRENT_BUCKET_VALUE_TAG)
        {
            if (!core::CPersistUtils::restore(CURRENT_BUCKET_VALUE_TAG, s_CurrentBucketValue, traverser))
            {
                LOG_ERROR("Failed to restore " << traverser.name() << " / " << traverser.value());
                return false;
            }
        }
        else if (name == BASELINE_BUCKET_MEAN_TAG)
        {
            if (!core::CPersistUtils::restore(BASELINE_BUCKET_MEAN_TAG, s_BaselineBucketMean, traverser))
            {
                LOG_ERROR("Failed to restore " << traverser.name() << " / " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());
    return true;
}

void SAttributeProbability::addDescriptiveData(annotated_probability::EDescriptiveData key, double value)
{
    s_DescriptiveData.push_back(TDescriptiveDataDoublePr(key, value));
}

SAnnotatedProbability::SAnnotatedProbability(void) :
        s_Probability(),
        s_ResultType(model_t::CResultType::E_Final)
{
}

SAnnotatedProbability::SAnnotatedProbability(double p) :
        s_Probability(p),
        s_ResultType(model_t::CResultType::E_Final)
{
}

void SAnnotatedProbability::addDescriptiveData(annotated_probability::EDescriptiveData key, double value)
{
    s_DescriptiveData.push_back(TDescriptiveDataDoublePr(key, value));
}

void SAnnotatedProbability::swap(SAnnotatedProbability &other)
{
    std::swap(s_Probability, other.s_Probability);
    s_AttributeProbabilities.swap(other.s_AttributeProbabilities);
    s_Influences.swap(other.s_Influences);
    s_DescriptiveData.swap(other.s_DescriptiveData);
    std::swap(s_CurrentBucketCount, other.s_CurrentBucketCount);
    std::swap(s_BaselineBucketCount, other.s_BaselineBucketCount);
}

void SAnnotatedProbability::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    core::CPersistUtils::persist(PROBABILITY_TAG, s_Probability, inserter);

    core::CPersistUtils::persist(ATTRIBUTE_PROBABILITIES_TAG, s_AttributeProbabilities, inserter);

    for (TStrPtrStrPtrPrDoublePrVecCItr i = s_Influences.begin(); i != s_Influences.end(); ++i)
    {
        inserter.insertValue(INFLUENCE_NAME_TAG, *i->first.first);
        inserter.insertValue(INFLUENCE_VALUE_TAG, *i->first.second);
        inserter.insertValue(INFLUENCE_TAG, i->second);
    }

    if (s_CurrentBucketCount)
    {
        core::CPersistUtils::persist(CURRENT_BUCKET_COUNT_TAG, *s_CurrentBucketCount, inserter);
    }
    if (s_BaselineBucketCount)
    {
        core::CPersistUtils::persist(BASELINE_BUCKET_COUNT_TAG, *s_BaselineBucketCount, inserter);
    }
}

bool SAnnotatedProbability::isInterim(void) const
{
    return s_ResultType.isInterim();
}

bool SAnnotatedProbability::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        TStrPtr influencerName;
        TStrPtr influencerValue;
        double d;

        if (name == PROBABILITY_TAG)
        {
            if (!core::CPersistUtils::restore(PROBABILITY_TAG, s_Probability, traverser))
            {
                LOG_ERROR("Restore error for " << traverser.name() << " / " << traverser.value());
                return false;
            }
        }
        else if (name == ATTRIBUTE_PROBABILITIES_TAG)
        {
            if (!core::CPersistUtils::restore(ATTRIBUTE_PROBABILITIES_TAG,
                                              s_AttributeProbabilities,
                                              traverser))
            {
                LOG_ERROR("Restore error for " << traverser.name() << " / " << traverser.value());
                return false;
            }
        }
        else if (name == INFLUENCE_NAME_TAG)
        {
            influencerName = CStringStore::influencers().get(traverser.value());
        }
        else if (name == INFLUENCE_VALUE_TAG)
        {
            influencerValue = CStringStore::influencers().get(traverser.value());
        }
        else if (name == INFLUENCE_TAG)
        {
            if (!core::CStringUtils::stringToType(traverser.value(), d))
            {
                LOG_ERROR("Restore error for " << traverser.name() << " / " << traverser.value());
                return false;
            }
            s_Influences.push_back(TStrPtrStrPtrPrDoublePr(
                                        TStrPtrStrPtrPr(influencerName, influencerValue), d));
        }
        else if (name == CURRENT_BUCKET_COUNT_TAG)
        {
            uint64_t i;
            if (!core::CPersistUtils::restore(CURRENT_BUCKET_COUNT_TAG, i, traverser))
            {
                LOG_ERROR("Restore error for " << traverser.name() << " / " << traverser.value());
                return false;
            }
            s_CurrentBucketCount.reset(i);
        }
        else if (name == BASELINE_BUCKET_COUNT_TAG)
        {
            if (!core::CPersistUtils::restore(BASELINE_BUCKET_COUNT_TAG, d, traverser))
            {
                LOG_ERROR("Restore error for " << traverser.name() << " / " << traverser.value());
                return false;
            }
            s_BaselineBucketCount.reset(d);
        }
    }
    while (traverser.next());
    return true;
}

}
}
