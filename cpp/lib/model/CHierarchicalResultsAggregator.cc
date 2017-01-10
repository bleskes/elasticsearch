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

#include <model/CHierarchicalResultsAggregator.h>

#include <core/CLogger.h>
#include <core/CPersistUtils.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/RestoreMacros.h>

#include <maths/Constants.h>
#include <maths/CTools.h>
#include <maths/ProbabilityAggregators.h>

#include <model/CAnomalyScore.h>
#include <model/CLimits.h>
#include <model/CModelConfig.h>
#include <model/CProbabilityAndInfluenceCalculator.h>

#include <boost/bind.hpp>
#include <boost/range.hpp>
#include <boost/ref.hpp>

#include <algorithm>
#include <string>
#include <utility>
#include <vector>

namespace ml
{
namespace model
{

namespace
{

typedef CHierarchicalResults::TStrPtr TStrPtr;
typedef CHierarchicalResults::TStrPtrStrPtrPr TStrPtrStrPtrPr;
typedef CHierarchicalResults::TStrPtrStrPtrPrDoublePr TStrPtrStrPtrPrDoublePr;
typedef CHierarchicalResults::TStrPtrStrPtrPrDoublePrVec TStrPtrStrPtrPrDoublePrVec;

//! \brief Creates new detector equalizers.
class CDetectorEqualizerFactory
{
    public:
        typedef CDetectorEqualizer TDetectorEqualizer;

        TDetectorEqualizer make(const std::string &/*name1*/,
                                const std::string &/*name2*/,
                                const std::string &/*name3*/,
                                const std::string &/*name4*/) const
        {
            return TDetectorEqualizer();
        }

        TDetectorEqualizer make(const std::string &/*name1*/,
                                const std::string &/*name2*/) const
        {
            return TDetectorEqualizer();
        }

        TDetectorEqualizer make(const std::string &/*name*/) const
        {
            return TDetectorEqualizer();
        }
};

//! Check if the underlying strings are equal.
bool equal(const TStrPtrStrPtrPr &lhs, const TStrPtrStrPtrPr &rhs)
{
    return *lhs.first == *rhs.first && *lhs.second == *rhs.second;
}

//! Compute the probability of \p influence.
bool influenceProbability(const TStrPtrStrPtrPrDoublePrVec &influences,
                          TStrPtr influencerName,
                          TStrPtr influencerValue,
                          double p,
                          double &result)
{
    TStrPtrStrPtrPr influence(influencerName, influencerValue);

    std::size_t k = static_cast<std::size_t>(
                            std::lower_bound(influences.begin(),
                                             influences.end(),
                                             influence,
                                             maths::COrderings::SFirstLess())
                          - influences.begin());

    if (k < influences.size() && equal(influences[k].first, influence))
    {
        result = influences[k].second == 1.0 ?
                 p : ::exp(influences[k].second * ::log(p));
        return true;
    }

    return false;
}

//! Insert \p value into a vector ordered by \p key.
template<typename KEY, typename TYPE, typename TYPE_VEC>
void insert(const KEY &key, TYPE value, std::vector<std::pair<KEY, TYPE_VEC> > &collection)
{
    typedef std::pair<KEY, TYPE_VEC> TKeyTypeVecPr;
    typedef std::vector<TKeyTypeVecPr> TKeyTypeVecPrVec;
    typedef typename TKeyTypeVecPrVec::iterator TKeyTypeVecPrVecItr;

    TKeyTypeVecPrVecItr i = std::lower_bound(collection.begin(), collection.end(),
                                             key, maths::COrderings::SFirstLess());

    if (i == collection.end())
    {
        collection.push_back(TKeyTypeVecPr(key, TYPE_VEC(1, value)));
    }
    else if (i->first != key)
    {
        collection.insert(i, TKeyTypeVecPr(key, TYPE_VEC(1, value)));
    }
    else
    {
        i->second.push_back(value);
    }
}

const std::string BUCKET_TAG("a");
const std::string INFLUENCER_BUCKET_TAG("b");
const std::string INFLUENCER_TAG("c");
const std::string PARTITION_TAG("d");
const std::string PERSON_TAG("e");
const std::string LEAF_TAG("f");

} // unnamed::

CHierarchicalResultsAggregator::CHierarchicalResultsAggregator(const CModelConfig &modelConfig) :
        TBase(TDetectorEqualizer()),
        m_Job(E_NoOp),
        m_DecayRate(modelConfig.decayRate()),
        m_MaximumAnomalousProbability(modelConfig.maximumAnomalousProbability())
{
    this->refresh(modelConfig);
}

void CHierarchicalResultsAggregator::setJob(EJob job)
{
    m_Job = job;
}

void CHierarchicalResultsAggregator::refresh(const CModelConfig &modelConfig)
{
    m_DecayRate = modelConfig.decayRate();
    m_MaximumAnomalousProbability = modelConfig.maximumAnomalousProbability();
    for (std::size_t i = 0u; i < model_t::NUMBER_AGGREGATION_STYLES; ++i)
    {
        for (std::size_t j = 0u; j < model_t::NUMBER_AGGREGATION_PARAMS; ++j)
        {
            m_Parameters[i][j] = modelConfig.aggregationStyleParam(static_cast<model_t::EAggregationStyle>(i),
                                                                   static_cast<model_t::EAggregationParam>(j));
        }
    }
}

void CHierarchicalResultsAggregator::clear(void)
{
    this->TBase::clear();
}

void CHierarchicalResultsAggregator::visit(const CHierarchicalResults &/*results*/, const TNode &node, bool pivot)
{
    if (isLeaf(node))
    {
        this->aggregateLeaf(node);
    }
    else
    {
        this->aggregateNode(node, pivot);
    }
}

void CHierarchicalResultsAggregator::propagateForwardByTime(double time)
{
    if (time < 0.0)
    {
        LOG_ERROR("Can't propagate normalizer backwards in time");
        return;
    }
    double factor = ::exp(-m_DecayRate * CDetectorEqualizer::largestProbabilityToCorrect() * time);
    this->age(boost::bind(&TDetectorEqualizer::age, _1, factor));
}

void CHierarchicalResultsAggregator::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(BUCKET_TAG, boost::bind(&TDetectorEqualizer::acceptPersistInserter,
                                                 boost::cref(this->bucketElement()), _1));
    core::CPersistUtils::persist(INFLUENCER_BUCKET_TAG, this->influencerBucketSet(), inserter);
    core::CPersistUtils::persist(INFLUENCER_TAG, this->influencerSet(), inserter);
    core::CPersistUtils::persist(PARTITION_TAG, this->partitionSet(), inserter);
    core::CPersistUtils::persist(PERSON_TAG, this->personSet(), inserter);
    core::CPersistUtils::persist(LEAF_TAG, this->leafSet(), inserter);
}

bool CHierarchicalResultsAggregator::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE(BUCKET_TAG, traverser.traverseSubLevel(boost::bind(&TDetectorEqualizer::acceptRestoreTraverser,
                                                                   boost::ref(this->bucketElement()), _1)))
        RESTORE(INFLUENCER_BUCKET_TAG, core::CPersistUtils::restore(INFLUENCER_BUCKET_TAG,
                                                                    this->influencerBucketSet(),
                                                                    traverser));
        RESTORE(INFLUENCER_TAG, core::CPersistUtils::restore(INFLUENCER_TAG, this->influencerSet(), traverser));
        RESTORE(PARTITION_TAG, core::CPersistUtils::restore(PARTITION_TAG, this->partitionSet(), traverser));
        RESTORE(PERSON_TAG, core::CPersistUtils::restore(PERSON_TAG, this->personSet(), traverser));
        RESTORE(LEAF_TAG, core::CPersistUtils::restore(LEAF_TAG, this->leafSet(), traverser));
    }
    while (traverser.next());
    return true;
}

uint64_t CHierarchicalResultsAggregator::checksum(void) const
{
    uint64_t seed = static_cast<uint64_t>(m_DecayRate);
    seed = maths::CChecksum::calculate(seed, m_Parameters);
    seed = maths::CChecksum::calculate(seed, m_MaximumAnomalousProbability);
    return this->TBase::checksum(seed);
}

void CHierarchicalResultsAggregator::aggregateLeaf(const TNode &node)
{
    if (isSimpleCount(node))
    {
        return;
    }

    int detector = node.s_Spec.s_Detector;
    double probability = node.probability();
    if (!maths::CMathsFuncs::isFinite(probability))
    {
        probability = 1.0;
    }
    probability = maths::CTools::truncate(probability, maths::CTools::smallestProbability(), 1.0);
    this->correctProbability(node, false, detector, probability);
    model_t::EAggregationStyle style =
            isAttribute(node) ? model_t::E_AggregateAttributes : model_t::E_AggregatePeople;

    node.s_AnnotatedProbability.s_Probability = probability;
    node.s_DetectorProbabilities.push_back(TDetectorProbability(detector, style, probability));
    node.s_SmallestChildProbability = probability;
    node.s_RawAnomalyScore = maths::CTools::deviation(probability);
}

void CHierarchicalResultsAggregator::aggregateNode(const TNode &node, bool pivot)
{
    typedef std::pair<int, std::size_t> TIntSizePr;
    typedef std::vector<double> TDoubleVec;
    typedef std::pair<TIntSizePr, TDoubleVec> TIntSizePrDoubleVecPr;
    typedef std::vector<TIntSizePrDoubleVecPr> TIntSizePrDoubleVecPrVec;
    typedef core::CSmallVector<double, 1> TDouble1Vec;
    typedef std::pair<int, TDouble1Vec> TIntDouble1VecPr;
    typedef std::vector<TIntDouble1VecPr> TIntDouble1VecPrVec;
    typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1> TMinAccumulator;

    LOG_TRACE("node = " << node.print() << ", pivot = " << pivot);

    bool skip = true;
    TMinAccumulator pMinChild;
    TMinAccumulator pMinDescendent;
    TIntSizePrDoubleVecPrVec probabilities[model_t::E_AggregateAttributes + 1];
    for (std::size_t i = 0u; i < boost::size(probabilities); ++i)
    {
        probabilities[i].reserve(node.s_Children.size());
    }

    for (std::size_t i = 0u; i < node.s_Children.size(); ++i)
    {
        const TNode &child = *node.s_Children[i];

        if (isSimpleCount(child))
        {
            continue;
        }

        double p = child.probability();
        if (   pivot
            && !isRoot(node)
            && !influenceProbability(child.s_AnnotatedProbability.s_Influences,
                                     node.s_Spec.s_PersonFieldName,
                                     node.s_Spec.s_PersonFieldValue, p, p))
        {
            LOG_ERROR("Couldn't find influence for " << child.print());
            continue;
        }

        skip = false;
        pMinChild.add(p);
        if (isTypeForWhichWeWriteResults(node, pivot))
        {
            pMinDescendent.add(p);
        }
        pMinDescendent.add(child.s_SmallestDescendantProbability);

        const TDetectorProbabilityVec &dp = child.s_DetectorProbabilities;
        for (std::size_t j = 0u; j < dp.size(); ++j)
        {
            model_t::EAggregationStyle style =
                    static_cast<model_t::EAggregationStyle>(dp[j].s_AggregationStyle);
            std::size_t hash = 0;
            if (pivot && !isRoot(node))
            {
                if (!influenceProbability(child.s_AnnotatedProbability.s_Influences,
                                          node.s_Spec.s_PersonFieldName,
                                          node.s_Spec.s_PersonFieldValue,
                                          dp[j].s_Probability, p))
                {
                    LOG_ERROR("Couldn't find influence for " << child.print());
                    continue;
                }

                core::CHashing::CMurmurHash2String hasher;
                boost::hash_combine(hash, hasher(child.s_Spec.s_PartitionFieldValue));
                if (child.s_Spec.s_IsPopulation)
                {
                    boost::hash_combine(hash, hasher(child.s_Spec.s_PersonFieldValue));
                }
            }
            switch (style)
            {
            case model_t::E_AggregatePeople:
            case model_t::E_AggregateAttributes:
                insert(std::make_pair(dp[j].s_Detector, hash), p, probabilities[style]);
                break;

            case model_t::E_AggregateDetectors:
                LOG_ERROR("Unexpected aggregation style for " << child.print());
                continue;
            }
        }
    }

    if (skip)
    {
        return;
    }

    LOG_TRACE("child probabilities = "
              << core::CContainerPrinter::print(probabilities));

    node.s_SmallestChildProbability =
            maths::CTools::truncate(pMinChild[0], maths::CTools::smallestProbability(), 1.0);
    node.s_SmallestDescendantProbability =
            maths::CTools::truncate(pMinDescendent[0], maths::CTools::smallestProbability(), 1.0);

    TIntDouble1VecPrVec detectorProbabilities_;
    int detector = -3;
    int aggregation = (   pivot
                       || isPartition(node)
                       || (isPopulation(node) && isPerson(node))) ?
                      static_cast<int>(model_t::E_AggregatePeople) : -1;

    for (int i = 0u; i < static_cast<int>(boost::size(probabilities)); ++i)
    {
        if (probabilities[i].empty())
        {
            continue;
        }

        const double *params = m_Parameters[i];
        const TIntSizePrDoubleVecPrVec &probabilitiesi = probabilities[i];
        if (probabilitiesi.empty())
        {
            continue;
        }

        for (std::size_t j = 0u; j < probabilitiesi.size(); ++j)
        {
            TIntSizePr key = probabilitiesi[j].first;
            double probability;
            if (probabilitiesi[j].second.size() == 1)
            {
                probability = probabilitiesi[j].second[0];
            }
            else
            {
                double rawAnomalyScore;
                CAnomalyScore::compute(params[model_t::E_JointProbabilityWeight],
                                       params[model_t::E_ExtremeProbabilityWeight],
                                       static_cast<std::size_t>(params[model_t::E_MinExtremeSamples]),
                                       static_cast<std::size_t>(params[model_t::E_MaxExtremeSamples]),
                                       m_MaximumAnomalousProbability,
                                       probabilitiesi[j].second,
                                       rawAnomalyScore, probability);
            }
            if (!maths::CMathsFuncs::isFinite(probability))
            {
                probability = 1.0;
            }

            insert(key.first, probability, detectorProbabilities_);

            switch (detector)
            {
            case -3: detector = key.first; break;
            case -2: break;
            default: detector = (detector != key.first ? -2 : key.first); break;
            }

            switch (aggregation)
            {
            case -1: aggregation = i; break;
            default: aggregation = (aggregation != i ? static_cast<int>(model_t::E_AggregatePeople) : i); break;
            }
        }
    }

    TDoubleVec detectorProbabilities;
    detectorProbabilities.reserve(detectorProbabilities_.size());
    for (std::size_t i = 0u; i < detectorProbabilities_.size(); ++i)
    {
        double probability;
        TIntDouble1VecPr &probabilitiesi = detectorProbabilities_[i];
        if (probabilitiesi.second.size() == 1)
        {
            probability = probabilitiesi.second[0];
        }
        else
        {
            const double *params = m_Parameters[model_t::E_AggregatePeople];
            double rawAnomalyScore;
            CAnomalyScore::compute(params[model_t::E_JointProbabilityWeight],
                                   params[model_t::E_ExtremeProbabilityWeight],
                                   static_cast<std::size_t>(params[model_t::E_MinExtremeSamples]),
                                   static_cast<std::size_t>(params[model_t::E_MaxExtremeSamples]),
                                   m_MaximumAnomalousProbability,
                                   probabilitiesi.second,
                                   rawAnomalyScore, probability);
        }
        detectorProbabilities.push_back(
                this->correctProbability(node, pivot, probabilitiesi.first, probability));
    }

    LOG_TRACE("detector = " << detector
              << ", aggregation = " << aggregation
              << ", detector probabilities = " << core::CContainerPrinter::print(detectorProbabilities));

    const double *params = m_Parameters[model_t::E_AggregateDetectors];
    CAnomalyScore::compute(params[model_t::E_JointProbabilityWeight],
                           params[model_t::E_ExtremeProbabilityWeight],
                           static_cast<std::size_t>(params[model_t::E_MinExtremeSamples]),
                           static_cast<std::size_t>(params[model_t::E_MaxExtremeSamples]),
                           m_MaximumAnomalousProbability,
                           detectorProbabilities,
                           node.s_RawAnomalyScore, node.s_AnnotatedProbability.s_Probability);
    node.s_DetectorProbabilities.push_back(TDetectorProbability(detector, aggregation, node.probability()));
    LOG_TRACE("probability = " << node.probability());
}

double CHierarchicalResultsAggregator::correctProbability(const TNode &node,
                                                          bool pivot,
                                                          int detector,
                                                          double probability)
{
    typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1, std::greater<double> > TMaxAccumulator;

    if (probability < CDetectorEqualizer::largestProbabilityToCorrect())
    {
        CDetectorEqualizerFactory factory;
        TDetectorEqualizerPtrVec equalizers;
        this->elements(node, pivot, factory, equalizers);
        TMaxAccumulator corrected;
        for (std::size_t i = 0u; i < equalizers.size(); ++i)
        {
            switch (m_Job)
            {
            case E_UpdateAndCorrect:
            {
                equalizers[i]->add(detector, probability);
                double pc = equalizers[i]->correct(detector, probability);
                corrected.add(pc);
                break;
            }
            case E_Correct:
                corrected.add(equalizers[i]->correct(detector, probability));
                break;
            case E_NoOp:
                break;
            }
        }
        if (corrected.count() > 0)
        {
            probability = corrected[0];
        }
    }

    return probability;
}

}
}
