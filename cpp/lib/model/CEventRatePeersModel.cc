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

#include <model/CEventRatePeersModel.h>

#include <core/CAllocationStrategy.h>
#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStatistics.h>
#include <core/CStringUtils.h>

#include <maths/CBasicStatistics.h>
#include <maths/CBootstrapClusterer.h>
#include <maths/CCategoricalTools.h>
#include <maths/CChecksum.h>
#include <maths/CNormalMeanPrecConjugate.h>
#include <maths/COrderings.h>
#include <maths/CTools.h>
#include <maths/CXMeans.h>
#include <maths/ProbabilityAggregators.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CDataGatherer.h>
#include <model/CInterimBucketCorrector.h>
#include <model/CModelDetailsView.h>
#include <model/CModelTools.h>
#include <model/CPopulationModelDetail.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include <model/FrequencyPredicates.h>

#include <boost/bind.hpp>
#include <boost/tuple/tuple.hpp>

#include <algorithm>
#include <limits>

namespace prelert
{
namespace model
{

namespace
{

typedef CEventRatePeersModel::TSizeSizePrFeatureDataPrVec TSizeSizePrFeatureDataPrVec;
typedef TSizeSizePrFeatureDataPrVec::const_iterator TSizeSizePrFeatureDataPrVecCItr;
typedef std::pair<model_t::EFeature, TSizeSizePrFeatureDataPrVec> TFeatureSizeSizePrFeatureDataPrVecPr;
typedef std::vector<TFeatureSizeSizePrFeatureDataPrVecPr> TFeatureSizeSizePrFeatureDataPrVecPrVec;

const maths_t::TWeightStyleVec COUNT_WEIGHT(1, maths_t::E_SampleCountWeight);

namespace detail
{

typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
typedef std::pair<double, std::size_t> TDoubleSizePr;
typedef std::vector<double> TDoubleVec;
typedef std::vector<TDoubleVec> TDoubleVecVec;
typedef core::CSmallVector<double, 1> TDouble1Vec;
typedef core::CSmallVector<double, 4> TDouble4Vec;
typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;
typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<TSizeVec> TSizeVecVec;
typedef boost::unordered_map<std::size_t, TSizeVec> TSizeSizeVecUMap;
typedef CEventRatePeersModel::TSizeMomentsUMap TSizeMomentsUMap;
typedef TSizeMomentsUMap::const_iterator TSizeMomentsUMapCItr;
typedef std::vector<TSizeMomentsUMapCItr> TSizeMomentsUMapCItrVec;
typedef CEventRatePeersModel::TMoments TMoments;
typedef CEventRatePeersModel::TMomentsVec TMomentsVec;
typedef CEventRatePeersModel::TDoubleMoments TDoubleMoments;
typedef CEventRatePeersModel::TDoubleMomentsVec TDoubleMomentsVec;
typedef CEventRatePeersModel::TSizeSizeUMap TSizeSizeUMap;
typedef CEventRatePeersModel::TSizeSizeUMapCItr TSizeSizeUMapCItr;
typedef CEventRatePeersModel::TPriorPtr TPriorPtr;
typedef CEventRatePeersModel::TPriorPtrVec TPriorPtrVec;
typedef CEventRatePeersModel::TSizeDoubleUMap TSizeDoubleUMap;
typedef CEventRatePeersModel::TStrCRefStrCRefPr TStrCRefStrCRefPr;
typedef CEventRatePeersModel::TStrCRefStrCRefPrUInt64Map TStrCRefStrCRefPrUInt64Map;
typedef maths::CVectorNx1<double, 2> TVector;
typedef std::vector<TVector> TVectorVec;
typedef TVectorVec::const_iterator TVectorVecCItr;
typedef std::vector<TVectorVec> TVectorVecVec;
typedef boost::unordered_map<TVector, TSizeVec, TVector::CHash> TVectorSizeVecUMap;
typedef maths::CXMeans<TVector, maths::CGaussianInfoCriterion<TVector, maths::E_BIC> > TXMeans;

//! \brief Compare two moments by their means.
struct SMeanLess
{
    template<typename MOMENTS>
    bool operator()(const MOMENTS &lhs, const MOMENTS &rhs) const
    {
        return maths::CBasicStatistics::mean(lhs) < maths::CBasicStatistics::mean(rhs);
    }
};

//! \brief The state used to update the peer groups.
struct SUpdatePeersState
{
    SUpdatePeersState(std::size_t maxPeerGroups) : xmeans(maxPeerGroups) {}

    TSizeMomentsUMapCItrVec zeroVariancePeople;
    TMomentsVec moments;
    TSizeSizeVecUMap momentsPeople;
    TVectorVec points;
    TVectorSizeVecUMap pointsPeople;
    TVectorVecVec clusters;
    TSizeVec split;
    TXMeans xmeans;
    TDoubleVecVec samples;
    TDoubleVecVec weights;
    TDoubleVec counts;
    TPriorPtrVec models;
    TSizeVecVec lastPeerGroups;
    TSizeVecVec peerGroups;
};

//! Hash the values in the range [\p begin, \p end) and update
//! \p hashes.
//!
//! \param[in] gatherer The data gatherer.
//! \param[in] cid The attribute identifier of the data to hash.
//! \param[in] begin The start of the data to hash.
//! \param[in] end The end of the data to hash.
//! \param[out] result The hashes to update with [\p begin, \p end).
template<typename ITR>
void checksum(const CDataGatherer &gatherer,
              std::size_t cid,
              ITR begin, ITR end,
              TStrCRefStrCRefPrUInt64Map &result)
{
    for (ITR i = begin; i != end; ++i)
    {
        std::size_t pid = i->first;
        if (gatherer.isPersonActive(pid))
        {
#define KEY(pid, cid) TStrCRefStrCRefPr(boost::cref(gatherer.personName(pid)), \
                                        boost::cref(gatherer.attributeName(cid)))
            uint64_t &hash = result[KEY(pid, cid)];
#undef KEY
            hash = maths::CChecksum::calculate(hash, i->second);
        }
    }

}

//! Hash the values in \p map and update \p hashes.
//!
//! \param[in] gatherer The data gatherer.
//! \param[in] map The data to hash.
//! \param[out] result The hashes to update with \p map.
template<typename MAP>
void checksum(const CDataGatherer &gatherer,
              const MAP &map,
              TStrCRefStrCRefPrUInt64Map &result)
{
    for (typename MAP::const_iterator i = map.begin(); i != map.end(); ++i)
    {
        for (std::size_t cid = 0u; cid < i->second.size(); ++cid)
        {
            if (gatherer.isAttributeActive(cid))
            {
                checksum(gatherer,
                         cid,
                         i->second[cid].begin(),
                         i->second[cid].end(),
                         result);
            }
        }
    }
}

//! Increase the size of the value vector for \p feature in
//! \p map by \p m.
//!
//! \param[in] feature The feature of interest.
//! \param[in] m The number of additional attributes.
//! \param[out] map The map to resize.
template<typename MAP>
void resize(model_t::EFeature feature,
            std::size_t m,
            MAP &map)
{
    typename MAP::mapped_type &value = map[feature];
    core::CAllocationStrategy::resize(value, m + value.size());
}

//! Clear the attribute data from a map over vectors indexed by
//! attribute identifier.
//!
//! \param[in] cid The attribute identifier to clear.
//! \param[out] map The map from which to remove \p cid data.
template<typename MAP>
void clearAttribute(std::size_t cid, MAP &map)
{
    for (typename MAP::iterator i = map.begin(); i != map.end(); ++i)
    {
        if (cid < i->second.size())
        {
            i->second[cid].clear();
        }
    }
}

//! Clear the attribute data from a map over vectors of maps keyed
//! by person identifier.
//!
//! \param[in] pid The person identifier to clear.
//! \param[out] map The map from which to remove \p pid data.
template<typename MAP>
void clearPerson(std::size_t pid, MAP &map)
{
    for (typename MAP::iterator i = map.begin(); i != map.end(); ++i)
    {
        for (std::size_t cid = 0u; cid < i->second.size(); ++cid)
        {
            i->second[cid].erase(pid);
        }
    }
}

//! Compute the collection of moments of each person for which at
//! least two values have been observed.
//!
//! If fewer than two values have been observed for a person then
//! its unique value is added to the nearest moments.
//!
//! \param[out] state The moments (state.moments) and a map from
//! moments identifier to person identifier (state.momentsPeople)
//! are filled in in this data structure.
//! \param[in] moments A map from person identifier to their moments.
void extractMoments(SUpdatePeersState &state,
                    TSizeMomentsUMap &moments)
{
    state.zeroVariancePeople.clear();
    state.moments.clear();
    state.moments.reserve(moments.size());
    state.momentsPeople.clear();
    {
        std::size_t i = 0u;
        for (TSizeMomentsUMapCItr j = moments.begin(); j != moments.end(); ++j)
        {
            if (maths::CBasicStatistics::maximumLikelihoodVariance(j->second) > 0.0)
            {
                state.moments.push_back(j->second);
                state.momentsPeople[i++].push_back(j->first);
            }
            else
            {
                state.zeroVariancePeople.push_back(j);
            }
        }
    }
    std::sort(state.moments.begin(), state.moments.end(), SMeanLess());

    // Assign zero variance people to their nearest moments.
    for (std::size_t i = 0u; i < state.zeroVariancePeople.size(); ++i)
    {
        const TMoments &mi = state.zeroVariancePeople[i]->second;
        ptrdiff_t j =
            maths::CTools::truncate(  std::lower_bound(state.moments.begin(),
                                                       state.moments.end(),
                                                       mi, SMeanLess()) - state.moments.begin(),
                                    static_cast<ptrdiff_t>(1),
                                    static_cast<ptrdiff_t>(state.moments.size() - 1));
        std::size_t index =  ::fabs(maths::CBasicStatistics::mean(state.moments[j]) -
                                    maths::CBasicStatistics::mean(mi))
                           < ::fabs(maths::CBasicStatistics::mean(state.moments[j-1]) -
                                    maths::CBasicStatistics::mean(mi)) ?
                             j : j-1;
        state.moments[index] += state.zeroVariancePeople[i]->second;
        state.momentsPeople[index].push_back(state.zeroVariancePeople[i]->first);
    }
}

//! Construct a set of 2D points from the scaled mean and standard
//! deviation of each set of moments.
//!
//! \param[in,out] state The moments (state.moments) are used to
//! construct points (state.points) \f$x = (mean, \lambda sd)'\f$
//! where \f$\lambda\f$ is choosen s.t. the range of X- and Y-
//! coordinates is equal and a map from points to person identfiers.
void makePoints(SUpdatePeersState &state)
{
    // Get the scales to apply such that each dimension has the
    // same influence on the clustering.
    maths::CBasicStatistics::COrderStatisticsStack<double, 1> xMin;
    maths::CBasicStatistics::COrderStatisticsStack<double, 1, std::greater<double> > xMax;
    maths::CBasicStatistics::COrderStatisticsStack<double, 1> yMin;
    maths::CBasicStatistics::COrderStatisticsStack<double, 1, std::greater<double> > yMax;
    for (std::size_t i = 0u; i < state.moments.size(); ++i)
    {
        double x = maths::CBasicStatistics::mean(state.moments[i]);
        double y = maths::CBasicStatistics::maximumLikelihoodVariance(state.moments[i]);
        xMin.add(x);
        xMax.add(x);
        yMin.add(y);
        yMax.add(y);
    }
    double sx = 1.0 / (xMax[0] - xMin[0]);
    double sy = 1.0 / (::sqrt(yMax[0]) - ::sqrt(yMin[0]));
    LOG_TRACE("x-scale = " << sx << ", y-scale = " << sy);

    // Construct the scaled points and a lookup of people based
    // on their points.
    state.points.clear();
    state.points.reserve(state.moments.size());
    state.pointsPeople.clear();
    for (std::size_t i = 0u; i < state.moments.size(); ++i)
    {
        double x = maths::CBasicStatistics::mean(state.moments[i]);
        double y = ::sqrt(maths::CBasicStatistics::maximumLikelihoodVariance(state.moments[i]));
        double coords[] = { sx * x, sy * y };
        TVector p(boost::begin(coords), boost::end(coords));
        state.points.push_back(p);
        state.pointsPeople[p].swap(state.momentsPeople[i]);
    }
}

//! Compute the peer group to which each person belongs.
//!
//! \param[in] state For each cluster (in state.clusters) use the
//! map from point to person identifiers (state.pointsPeople) to
//! build peerGroups.
//! \param[out] peerGroups A map from person identifier to cluster
//! identifier.
void assignPeerGroups(SUpdatePeersState &state,
                      TSizeSizeUMap &peerGroups)
{
    for (std::size_t i = 0u; i < state.clusters.size(); ++i)
    {
        const TVectorVec &cluster = state.clusters[i];
        for (std::size_t j = 0u; j < cluster.size(); ++j)
        {
            const TSizeVec &pids = state.pointsPeople[cluster[j]];
            for (std::size_t k = 0u; k < pids.size(); ++k)
            {
                peerGroups[pids[k]] = j;
            }
        }
    }
}

//! Reinitialize \p vec to contain \p n cleared elements.
template<typename VEC>
void reinitialize(std::size_t n, VEC &vec)
{
    vec.resize(n);
    for (std::size_t i = 0u; i < n; ++i)
    {
        vec[i].clear();
    }
}

//! Create new models for each peer group in the clustering
//! of each attribute's people.
//!
//! \param[in,out] state For each cluster (in state.clusters)
//! compute a representative set of samples with which to seed
//! peerGroupModels.
//! \param[in] personSamples The number of times to sample
//! person models.
//! \param[in] peerGroupSamples The number of times to sample
//! peer group models.
//! \param[in] lastPeerGroups A map from person identifier
//! to cluster identifier for the last clustering.
//! \param[in] peerGroups A map from person identifier to
//! cluster identifier for the last clustering.
//! \param[in] moments A map from person identifier to their
//! moments.
//! \param[in] stability A map from person identifier to the
//! consistency of their clusters.
//! \param[in] prior The seed prior for a peer group model.
//! \param[in,out] peerGroupPriors The last set of peer group
//! models and filled in with the new ones.
void updatePeerGroupModels(SUpdatePeersState &state,
                           std::size_t personSamples,
                           std::size_t peerGroupSamples,
                           TSizeSizeUMap &lastPeerGroups,
                           TSizeSizeUMap &peerGroups,
                           TSizeMomentsUMap &moments,
                           TSizeDoubleUMap &stability,
                           maths::CPrior &prior,
                           TPriorPtrVec &peerGroupPriors)
{
    std::size_t N = state.clusters.size();
    std::size_t M = peerGroupPriors.size();

    // Re-initialize the relevant state.
    reinitialize(N, state.samples);
    reinitialize(N, state.peerGroups);
    reinitialize(M, state.lastPeerGroups);
    state.counts.clear();
    state.counts.resize(N, 0.0);
    state.weights.resize(N);
    for (std::size_t i = 0u; i < N; ++i)
    {
        state.weights[i].resize(M);
        for (std::size_t j = 0u; j < M; ++j)
        {
            state.weights[i][j] = 0.0;
        }
    }
    state.models.clear();
    state.models.reserve(N);
    for (std::size_t i = 0u; i < N; ++i)
    {
        state.models.push_back(TPriorPtr(prior.clone()));
    }

    // Compute the person samples and the weight in the peer
    // group samples.
    TDouble1Vec samples;
    for (TSizeSizeUMapCItr i = peerGroups.begin(); i != peerGroups.end(); ++i)
    {
        std::size_t pid   = i->first;
        std::size_t group = i->second;

        TSizeSizeUMapCItr j = lastPeerGroups.find(pid);

        TMoments mm = moments[pid];
        double n = static_cast<double>(maths::CBasicStatistics::count(mm));
        double m = static_cast<double>(maths::CBasicStatistics::moment<0>(mm));
        double v = static_cast<double>(maths::CBasicStatistics::moment<1>(mm));
        maths::CNormalMeanPrecConjugate normal(maths_t::E_IntegerData,
                                               maths::CBasicStatistics::accumulator(n, m, v));
        normal.sampleMarginalLikelihood(personSamples, samples);
        state.samples[group].insert(state.samples[group].end(),
                                    samples.begin(),
                                    samples.end());

        if (j != lastPeerGroups.end())
        {
            state.weights[group][j->second] += stability[pid];
        }

        state.counts[group] += n;
    }

    // Create the new peer group models and swap into place.
    TDouble1Vec sample(1);
    TDouble4Vec1Vec weight(1, TDouble4Vec(1, 1.0));
    for (std::size_t i = 0u; i < N; ++i)
    {
        TDoubleVec &samplesi = state.samples[i];
        const TDoubleVec &weightsi = state.weights[i];
        maths::CPrior &modeli = *state.models[i];

        double Z = static_cast<double>(samplesi.size());
        for (std::size_t j = 0u; j < weightsi.size(); ++j)
        {
            Z += weightsi[j];
        }
        double w = state.counts[i] / Z;

        std::sort(samplesi.begin(), samplesi.end());
        weight[0][0] = w;
        for (std::size_t j = 0u; j < samplesi.size(); ++j)
        {
            sample[0] = samplesi[j];
            modeli.addSamples(COUNT_WEIGHT, sample, weight);
        }

        for (std::size_t j = 0u; j < weightsi.size(); ++j)
        {
            if (weightsi[j] == 0.0)
            {
                continue;
            }
            peerGroupPriors[j]->sampleMarginalLikelihood(peerGroupSamples, samples);
            weight[0][0] = w * weightsi[j]
                             / static_cast<double>(samples.size());
            for (std::size_t k = 0u; k < samples.size(); ++k)
            {
                sample[0] = samples[k];
                modeli.addSamples(COUNT_WEIGHT, sample, weight);
            }
        }
    }
    state.models.swap(peerGroupPriors);
}

//! Update the person clustering stabilities.
//!
//! The stability are a smoothed measure of the stability of
//! a person's peer group in the range [0,1] with 0 representing
//! unstable and 1 representing unchanging. In particular, it
//! is an exponentially weighted moving average of the Jaccard
//! index between a person's consecutive peer groups.
//!
//! \param[in,out] state The set representation, in terms of
//! people each cluster contains, for the old and new clusterings
//! (in state.lastPeerGroups and state.peerGroups) are filled
//! in and used to compute the maximum Jaccard index between the
//! old clusters and each new cluster.
//! \param[in] lastPeerGroups A map from person identifier
//! to cluster identifier for the last clustering.
//! \param[in] peerGroups A map from person identifier to
//! cluster identifier for the last clustering.
//! \param[in] alpha The alpha coefficient to use for the alpha
//! beta filter for smoothing the Jaccard index.
//! \param[in,out] stability Updated with the Jaccard indexes
//! computed between the old and new clusterings.
void updateClusterStability(SUpdatePeersState &state,
                            TSizeSizeUMap &lastPeerGroups,
                            TSizeSizeUMap &peerGroups,
                            double alpha,
                            TSizeDoubleUMap &stability)
{
    for (TSizeSizeUMapCItr i = lastPeerGroups.begin();
         i != lastPeerGroups.end();
         ++i)
    {
        state.lastPeerGroups[i->second].push_back(i->first);
    }
    for (std::size_t i = 0u; i < state.lastPeerGroups.size(); ++i)
    {
        std::sort(state.lastPeerGroups[i].begin(),
                  state.lastPeerGroups[i].end());
    }
    for (TSizeSizeUMapCItr i = peerGroups.begin(); i != peerGroups.end(); ++i)
    {
        state.peerGroups[i->second].push_back(i->first);
    }
    for (std::size_t i = 0u; i < state.peerGroups.size(); ++i)
    {
        std::sort(state.peerGroups[i].begin(), state.peerGroups[i].end());
    }

    TDoubleVec jaccard(state.peerGroups.size(), 0.0);
    for (std::size_t i = 0u; i < jaccard.size(); ++i)
    {
        for (std::size_t j = 0u; j < state.lastPeerGroups.size(); ++j)
        {
            jaccard[i] = std::max(jaccard[i],
                                  maths::CSetTools::jaccard(state.peerGroups[i].begin(),
                                                            state.peerGroups[i].end(),
                                                            state.lastPeerGroups[j].begin(),
                                                            state.lastPeerGroups[j].end()));
        }
    }

    for (TSizeSizeUMapCItr i = peerGroups.begin(); i != peerGroups.end(); ++i)
    {
        double &s = stability[i->first];
        s = (1.0 - alpha) * s + alpha * jaccard[i->second];
    }
}

} // detail::

const std::string EMPTY_STRING("");

const maths_t::TWeightStyleVec SEASONAL_VARIANCE_WEIGHT(1, maths_t::E_SampleSeasonalVarianceScaleWeight);

} // unnamed::

CEventRatePeersModel::CEventRatePeersModel(const SModelParams &params,
                                           const TDataGathererPtr &dataGatherer,
                                           const TFeaturePriorPtrPrVec &newPriors,
                                           const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                                           const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                                           const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators) :
        CPopulationModel(params,
                         dataGatherer,
                         newPriors,
                         newMultivariatePriors,
                         newDecompositions,
                         influenceCalculators,
                         false),
        m_CurrentBucketStats(  dataGatherer->currentBucketStartTime()
                             - dataGatherer->bucketLength()),
        m_NewAttributeProbabilityPrior(
                maths::CMultinomialConjugate::nonInformativePrior(
                        std::numeric_limits<int>::max(), params.s_DecayRate)),
        m_LastBatchUpdateTime(maths::CIntegerTools::ceil(dataGatherer->currentBucketStartTime(),
                                                         BATCH_UPDATE_PERIOD))
{
}

CEventRatePeersModel::CEventRatePeersModel(const SModelParams &params,
                                           const TDataGathererPtr &dataGatherer,
                                           const TFeaturePriorPtrPrVec &newPriors,
                                           const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                                           const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                                           const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                                           core::CStateRestoreTraverser &/*traverser*/) :
        CPopulationModel(params,
                         dataGatherer,
                         TFeaturePriorPtrPrVec(),
                         TFeatureMultivariatePriorPtrPrVec(),
                         newDecompositions,
                         influenceCalculators,
                         true),
        m_CurrentBucketStats(  dataGatherer->currentBucketStartTime()
                             - dataGatherer->bucketLength()),
        m_NewAttributeProbabilityPrior(),
        m_LastBatchUpdateTime()
{
    CModelTools::shallowCopyConstantNewPriors(newPriors, this->newPriors());
    CModelTools::shallowCopyConstantNewPriors(newMultivariatePriors, this->newMultivariatePriors());
    // TODO
    //traverser.traverseSubLevel(boost::bind(&CEventRatePeersModel::acceptRestoreTraverser,
    //                                       this,
    //                                       boost::ref(params.s_ExtraDataRestoreFunc),
    //                                       _1));
}

CEventRatePeersModel::CEventRatePeersModel(bool isForPersistence,
                                           const CEventRatePeersModel &other) :
        CPopulationModel(isForPersistence, other),
        m_CurrentBucketStats(0), // Not needed for persistence so minimally constructed
        m_NewAttributeProbabilityPrior(other.m_NewAttributeProbabilityPrior),
        m_LastBatchUpdateTime(other.m_LastBatchUpdateTime),
        m_FeatureMoments(other.m_FeatureMoments),
        m_FeaturePeerGroups(other.m_FeaturePeerGroups),
        m_FeatureClusterStability(other.m_FeatureClusterStability)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }

    for (TFeaturePriorPtrVecVecMapCItr i = other.m_FeaturePeerGroupPriors.begin();
         i != other.m_FeaturePeerGroupPriors.end();
         ++i)
    {
        TPriorPtrVecVec &priors = m_FeaturePeerGroupPriors[i->first];
        priors.resize(i->second.size());
        for (std::size_t j = 0u; j < i->second.size(); ++j)
        {
            priors[j].reserve(i->second[j].size());
            for (std::size_t k = 0u; k < i->second[j].size(); ++k)
            {
                priors[j].push_back(TPriorPtr(i->second[j][k]->clone()));
            }
        }
    }
}

void CEventRatePeersModel::acceptPersistInserter(core::CStatePersistInserter &/*inserter*/) const
{
    // TODO
}

bool CEventRatePeersModel::acceptRestoreTraverser(const model_t::TAnyRestoreFunc &/*extraDataRestoreFunc*/,
                                                  core::CStateRestoreTraverser &/*traverser*/)
{
    // TODO
    return true;
}

CModel *CEventRatePeersModel::cloneForPersistence(void) const
{
    return new CEventRatePeersModel(true, *this);
}

model_t::EModelType CEventRatePeersModel::category(void) const
{
    return model_t::E_EventRateOnline;
}

bool CEventRatePeersModel::isEventRate(void) const
{
    return true;
}

bool CEventRatePeersModel::isMetric(void) const
{
    return false;
}

CEventRatePeersModel::TDouble1Vec
    CEventRatePeersModel::currentBucketValue(model_t::EFeature feature,
                                             std::size_t pid,
                                             std::size_t cid,
                                             core_t::TTime time) const
{
    return this->currentBucketValue(this->featureData(feature, time),
                                    feature, pid, cid, TDouble1Vec(1, 0.0));
}

CEventRatePeersModel::TDouble1Vec
    CEventRatePeersModel::baselineBucketMean(model_t::EFeature feature,
                                             std::size_t pid,
                                             std::size_t cid,
                                             model_t::CResultType type,
                                             const TSizeDoublePr1Vec &/*correlated*/,
                                             core_t::TTime /*time*/) const
{
    if (!model_t::isAttributeConditional(feature))
    {
        cid = 0u;
    }

    const TSizeMomentsUMap &attributeMoments = this->attributeMoments(feature, cid);
    if (attributeMoments.empty())
    {
        LOG_ERROR("No moments for attribute = " << this->attributeName(cid)
                  << " and feature = " << model_t::print(feature));
        return TDouble1Vec();
    }
    TSizeMomentsUMapCItr i = attributeMoments.find(pid);
    if (i == attributeMoments.end())
    {
        LOG_ERROR("No moments for person = " << this->personName(pid)
                  << ", attribute = " << this->attributeName(cid)
                  << " and feature = " << model_t::print(feature));
        return TDouble1Vec();
    }

    TDouble1Vec result(1, maths::CBasicStatistics::mean(i->second));
    this->correctBaselineForInterim(feature, pid, cid, type, this->currentBucketInterimCorrections(), result);
    TDouble1VecDouble1VecPr support = model_t::support(feature);
    return maths::CTools::truncate(result, support.first, support.second);
}

void CEventRatePeersModel::sampleBucketStatistics(core_t::TTime startTime,
                                                  core_t::TTime endTime,
                                                  CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (!gatherer.validateSampleTimes(startTime, endTime))
    {
        return;
    }

    m_CurrentBucketStats.s_InterimCorrections.clear();
    for (core_t::TTime bucketStartTime = startTime;
         bucketStartTime < endTime;
         bucketStartTime += bucketLength)
    {
        this->CModel::sampleBucketStatistics(bucketStartTime, bucketStartTime + bucketLength, resourceMonitor);

        // Currently, we only remember one bucket.
        m_CurrentBucketStats.s_StartTime = bucketStartTime;
        TSizeUInt64PrVec &personCounts = m_CurrentBucketStats.s_PersonCounts;
        gatherer.personNonZeroCounts(bucketStartTime, personCounts);
        this->applyFilter(model_t::E_XF_Over, false, this->personFilter(), personCounts);

        TFeatureSizeSizePrFeatureDataPrVecPrVec featureData;
        gatherer.featureData(bucketStartTime, bucketLength, featureData);
        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            model_t::EFeature feature = featureData[i].first;
            TSizeSizePrFeatureDataPrVec &data = m_CurrentBucketStats.s_FeatureData[feature];
            data.swap(featureData[i].second);
            LOG_TRACE(model_t::print(feature)
                      << " data = " << core::CContainerPrinter::print(data));

            this->applyFilters(feature, false, this->personFilter(), this->attributeFilter(), data);
        }
    }
}

void CEventRatePeersModel::sample(core_t::TTime startTime,
                                  core_t::TTime endTime,
                                  CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();
    if (!gatherer.validateSampleTimes(startTime, endTime))
    {
        return;
    }

    typedef std::pair<model_t::EFeature, core_t::TTime> TFeatureTimePr;
    typedef boost::tuple<TSizeVec, TDouble1VecVec, TDouble1Vec4VecVec> TSizeVecDouble1VecVecDouble1Vec4VecVecTr;
    typedef boost::unordered_map<std::size_t, TSizeVecDouble1VecVecDouble1Vec4VecVecTr> TSampleData;
    typedef TSampleData::iterator TSampleDataItr;
    typedef boost::unordered_map<std::size_t, TSampleData> TSizeSampleDataUMap;
    typedef TSizeSampleDataUMap::iterator TSizeSampleDataUMapItr;
    typedef std::map<TFeatureTimePr, TSizeSampleDataUMap> TFeatureTimePrSizeSampleDataUMapMap;
    typedef TFeatureTimePrSizeSampleDataUMapMap::iterator TFeatureTimePrSizeSampleDataUMapMapItr;

    this->createUpdateNewModels(startTime, resourceMonitor);
    m_CurrentBucketStats.s_InterimCorrections.clear();

    // We gather up the data and update the models at the end.
    TFeatureTimePrSizeSampleDataUMapMap peerGroupData;

    for (core_t::TTime bucketStartTime = startTime;
         bucketStartTime < endTime;
         bucketStartTime += bucketLength)
    {
        LOG_TRACE("Sampling [" << bucketStartTime
                  << "," << bucketStartTime + bucketLength << ")");

        gatherer.sampleNow(bucketStartTime);

        this->CPopulationModel::sample(bucketStartTime,
                                       bucketStartTime + bucketLength,
                                       resourceMonitor);

        // Currently, we only remember one bucket.
        m_CurrentBucketStats.s_StartTime = bucketStartTime;
        TSizeUInt64PrVec &personCounts = m_CurrentBucketStats.s_PersonCounts;
        gatherer.personNonZeroCounts(bucketStartTime, personCounts);
        this->applyFilter(model_t::E_XF_Over, true, this->personFilter(), personCounts);

        // Declared outside loop to minimize number of times it is created.
        TDouble1Vec sample(1);
        TDouble1Vec4Vec weight(1, TDouble1Vec(1));

        // Update the person and population count feature models.
        TFeatureSizeSizePrFeatureDataPrVecPrVec featureData;
        gatherer.featureData(bucketStartTime, bucketLength, featureData);
        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            model_t::EFeature feature = featureData[i].first;
            TSizeSizePrFeatureDataPrVec &data = m_CurrentBucketStats.s_FeatureData[feature];
            data.swap(featureData[i].second);
            LOG_TRACE(model_t::print(feature)
                      << " data = " << core::CContainerPrinter::print(data));

            switch (feature)
            {
            CASE_INDIVIDUAL_COUNT:
            CASE_INDIVIDUAL_METRIC:
            CASE_POPULATION_COUNT:
            CASE_POPULATION_METRIC:
                LOG_ERROR("Unexpected feature = " << model_t::print(feature));
                continue;

            case model_t::E_PeersAttributeTotalCountByPerson:
                continue;
            case model_t::E_PeersCountByBucketPersonAndAttribute:
            case model_t::E_PeersUniqueCountByBucketPersonAndAttribute:
            case model_t::E_PeersLowCountsByBucketPersonAndAttribute:
            case model_t::E_PeersHighCountsByBucketPersonAndAttribute:
            case model_t::E_PeersInfoContentByBucketPersonAndAttribute:
            case model_t::E_PeersLowInfoContentByBucketPersonAndAttribute:
            case model_t::E_PeersHighInfoContentByBucketPersonAndAttribute:
            case model_t::E_PeersLowUniqueCountByBucketPersonAndAttribute:
            case model_t::E_PeersHighUniqueCountByBucketPersonAndAttribute:
            case model_t::E_PeersTimeOfDayByBucketPersonAndAttribute:
            case model_t::E_PeersTimeOfWeekByBucketPersonAndAttribute:
                break;

            CASE_PEERS_METRIC:
                LOG_ERROR("Unexpected feature = " << model_t::print(feature));
                continue;
            }

            this->applyFilters(feature, true, this->personFilter(), this->attributeFilter(), data);

            core_t::TTime sampleTime = model_t::sampleTime(feature,
                                                           bucketStartTime,
                                                           bucketLength);

            TSizeSampleDataUMap &peerGroupFeatureData =
                    peerGroupData[TFeatureTimePr(feature, sampleTime)];
            const TSizeSizeUMapVec &attributePeerGroups = m_FeaturePeerGroups[feature];
            const TPriorPtrVecVec &attributePriors = m_FeaturePeerGroupPriors[feature];

            for (std::size_t j = 0u; j < data.size(); ++j)
            {
                std::size_t pid = CDataGatherer::extractPersonId(data[j]);
                std::size_t cid = CDataGatherer::extractAttributeId(data[j]);
                uint64_t count = CDataGatherer::extractData(data[j]).s_Count;
                double adjustedCount = model_t::offsetCountToZero(
                                           feature, static_cast<double>(count));
                LOG_TRACE("Adding " << adjustedCount
                          << " for person = " << gatherer.personName(pid)
                          << " and attribute = " << gatherer.attributeName(cid));

                if (   cid >= attributePeerGroups.size()
                    || cid >= attributePriors.size())
                {
                    LOG_ERROR("No peer groups for attribute '"
                              << gatherer.attributeName(cid, core::CStringUtils::typeToString(cid)) << "'");
                    continue;
                }

                TSampleData &peerGroupAttributeData = peerGroupFeatureData[cid];

                const TSizeSizeUMap &personPeerGroups = attributePeerGroups[cid];
                TSizeSizeUMapCItr k = personPeerGroups.find(pid);
                std::size_t group = k != personPeerGroups.end() ?
                                    k->second : attributePriors[cid].size();
                sample[0] = adjustedCount;
                weight[0][0] = this->sampleRateWeight(pid, cid) * this->learnRate(feature);

                TSizeVecDouble1VecVecDouble1Vec4VecVecTr &tuple = peerGroupAttributeData[group];
                tuple.get<0>().push_back(pid);
                tuple.get<1>().push_back(sample);
                tuple.get<2>().push_back(weight);
            }
        }
    }

    double alpha = ::exp(-this->params().s_DecayRate);

    TDouble1VecVec attributeSamples;
    TTimeVec sampleTimes;
    for (TFeatureTimePrSizeSampleDataUMapMapItr i = peerGroupData.begin();
         i != peerGroupData.end();
         ++i)
    {
        model_t::EFeature feature = i->first.first;
        core_t::TTime sampleTime = i->first.second;

        TSizeMomentsUMapVec &attributeMoments = m_FeatureMoments[feature];
        TPriorPtrVecVec &attributePriors = m_FeaturePeerGroupPriors[feature];
        TSizeSampleDataUMap &attributeData = i->second;

        for (TSizeSampleDataUMapItr j = attributeData.begin();
             j != attributeData.end();
             ++j)
        {
            std::size_t cid = j->first;
            attributeSamples.clear();
            for (TSampleDataItr k = j->second.begin(); k != j->second.end(); ++k)
            {
                attributeSamples.insert(attributeSamples.end(),
                                        k->second.get<1>().begin(),
                                        k->second.get<1>().end());
            }
            sampleTimes.assign(attributeSamples.size(), sampleTime);
            this->updateTrend(feature, cid, sampleTimes, attributeSamples);

            TSizeMomentsUMap &moments = attributeMoments[cid];

            for (TSampleDataItr k = j->second.begin(); k != j->second.end(); ++k)
            {
                const TSizeVec &pids = k->second.get<0>();
                TDouble1VecVec &samples = k->second.get<1>();
                TDouble1Vec4VecVec &weights = k->second.get<2>();
                for (std::size_t l = 0u; l < samples.size(); ++l)
                {
                    samples[l] = this->detrend(feature,
                                               cid,
                                               sampleTime,
                                               0.0, // confidence
                                               samples[l]);

                    TMoments &moment = moments[pids[l]];
                    moment.add(samples[l][0], weights[l][0][0]);
                    moment.age(alpha);
                }

                std::size_t group = k->first;
                if (group < attributePriors[cid].size())
                {
                    maths::COrderings::simultaneousSort(samples, weights);
                    TPriorPtr &prior = attributePriors[cid][k->first];
                    for (std::size_t l = 0u; l < samples.size(); ++l)
                    {
                        prior->addSamples(COUNT_WEIGHT, samples[l], weights[l]);
                    }
                    prior->propagateForwardsByTime(this->propagationTime(cid, sampleTime));
                    LOG_TRACE(this->attributeName(cid) << "(" << k->first
                              << ") prior:" << core_t::LINE_ENDING << prior->print());
                }
            }
        }
    }

    if (endTime >= m_LastBatchUpdateTime + BATCH_UPDATE_PERIOD)
    {
        this->updatePeerGroups(endTime);
        m_LastBatchUpdateTime = maths::CIntegerTools::ceil(endTime, BATCH_UPDATE_PERIOD);
    }
}

void CEventRatePeersModel::prune(std::size_t maximumAge)
{
    CDataGatherer &gatherer = this->dataGatherer();

    TSizeVec peopleToRemove;
    TSizeVec attributesToRemove;
    this->peopleAndAttributesToRemove(m_CurrentBucketStats.s_StartTime,
                                      maximumAge,
                                      peopleToRemove,
                                      attributesToRemove);

    if (peopleToRemove.empty() && attributesToRemove.empty())
    {
        return;
    }

    std::sort(peopleToRemove.begin(), peopleToRemove.end());
    std::sort(attributesToRemove.begin(), attributesToRemove.end());

    LOG_DEBUG("Removing people {" << this->printPeople(peopleToRemove, 20) << '}');
    LOG_DEBUG("Removing attributes {" << this->printAttributes(attributesToRemove, 20) << '}');

    // Stop collecting for these people/attributes and add them
    // to the free list.
    gatherer.recyclePeople(peopleToRemove);
    gatherer.recycleAttributes(attributesToRemove);

    TFeatureSizeSizePrFeatureDataPrVecPrVec featureData;
    gatherer.featureData(m_CurrentBucketStats.s_StartTime, gatherer.bucketLength(), featureData);
    for (std::size_t i = 0u; i < featureData.size(); ++i)
    {
        model_t::EFeature feature = featureData[i].first;
        TSizeSizePrFeatureDataPrVec &data = m_CurrentBucketStats.s_FeatureData[feature];
        data.swap(featureData[i].second);
    }

    this->clearPrunedResources(peopleToRemove, attributesToRemove);
    this->removePeople(peopleToRemove);
}

bool CEventRatePeersModel::computeProbability(std::size_t pid,
                                              core_t::TTime startTime,
                                              core_t::TTime endTime,
                                              CPartitioningFields &partitioningFields,
                                              std::size_t numberAttributeProbabilities,
                                              SAnnotatedProbability &result) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (endTime != startTime + bucketLength)
    {
        LOG_ERROR("Can only compute probability for single bucket");
        return false;
    }
    if (pid > gatherer.numberActivePeople())
    {
        LOG_TRACE("No person for pid = " << pid);
        return false;
    }

    CAnnotatedProbabilityBuilder resultBuilder(result,
                                               numberAttributeProbabilities,
                                               function_t::function(gatherer.features()),
                                               gatherer.numberActivePeople());

    LOG_TRACE("computeProbability(" << gatherer.personName(pid) << ")");

    typedef boost::shared_ptr<const std::string> TStrPtr;
    typedef core::CSmallVector<TStrPtr, 1> TStrPtr1Vec;
    typedef boost::unordered_map<std::size_t, CProbabilityAndInfluenceCalculator> TSizeProbabilityAndInfluenceUMap;
    typedef TSizeProbabilityAndInfluenceUMap::const_iterator TSizeProbabilityAndInfluenceUMapCItr;
    typedef std::pair<double, model_t::EFeature> TDoubleFeaturePr;
    typedef maths::CBasicStatistics::COrderStatisticsStack<TDoubleFeaturePr, 1u> TDoubleFeaturePrMinAccumulator;
    typedef boost::unordered_map<std::size_t, TDoubleFeaturePrMinAccumulator> TSizeDoubleFeaturePrMinAccumulatorUMap;
    typedef TSizeDoubleFeaturePrMinAccumulatorUMap::const_iterator TSizeDoubleFeaturePrMinAccumulatorUMapCItr;
    typedef TSizeSizeUMap::const_iterator TSizeSizeUMapCItr;

    numberAttributeProbabilities = std::max(numberAttributeProbabilities, std::size_t(1));

    CProbabilityAndInfluenceCalculator pJoint(this->params().s_InfluenceCutoff);
    pJoint.addAggregator(maths::CJointProbabilityOfLessLikelySamples());

    CProbabilityAndInfluenceCalculator pConditionalTemplate(this->params().s_InfluenceCutoff);
    pConditionalTemplate.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
    pConditionalTemplate.addAggregator(maths::CProbabilityOfExtremeSample());
    TSizeProbabilityAndInfluenceUMap pConditional;

    TSizeDoubleFeaturePrMinAccumulatorUMap features;
    maths::CMultinomialConjugate personAttributeProbabilityPrior(m_NewAttributeProbabilityPrior);

    // Declared outside loop to minimize number of times they are created.
    TDoubleVec category(1);
    static const TStrPtr1Vec NO_CORRELATED_ATTRIBUTES;
    static const TSizeDoublePr1Vec NO_CORRELATES;
    CProbabilityAndInfluenceCalculator::SParams params(SEASONAL_VARIANCE_WEIGHT, partitioningFields);
    params.s_Weights.resize(1, TDouble4Vec(SEASONAL_VARIANCE_WEIGHT.size()));

    for (std::size_t i = 0u; i < gatherer.numberFeatures(); ++i)
    {
        model_t::EFeature feature = gatherer.feature(i);
        LOG_TRACE("feature = " << model_t::print(feature));

        if (feature == model_t::E_PeersAttributeTotalCountByPerson)
        {
            const TSizeSizePrFeatureDataPrVec &data = this->featureData(feature, startTime);
            TSizeSizePr range = CModelTools::personRange(data, pid);
            for (std::size_t j = range.first; j < range.second; ++j)
            {
                category[0] = static_cast<double>(CDataGatherer::extractAttributeId(data[j]));
                params.s_Weights[0][0] = static_cast<double>(CDataGatherer::extractData(data[j]).s_Count);
                personAttributeProbabilityPrior.addSamples(COUNT_WEIGHT, category, params.s_Weights);
            }
            continue;
        }
        else if (model_t::isCategorical(feature))
        {
            continue;
        }

        // TODO Apply detection rules

        const TSizeSizePrFeatureDataPrVec &featureData = this->featureData(feature, startTime);
        TSizeSizePr range = CModelTools::personRange(featureData, pid);
        const TPriorPtrVecVec &peerGroupPriors = this->peerGroupPriors(feature);
        const TSizeSizeUMapVec &personPeerGroups = this->personPeerGroups(feature);
        core_t::TTime sampleTime = model_t::sampleTime(feature, startTime, bucketLength);

        for (std::size_t j = range.first; j < range.second; ++j)
        {
            // 1) Sample the person's feature for the bucket.
            // 2) Compute the probability of the sample for the
            //    population model of the corresponding attribute.
            // 3) Update the attribute probability.
            // 4) Update the attribute influences.

            std::size_t cid = CDataGatherer::extractAttributeId(featureData[j]);
            if (cid >= peerGroupPriors.size() || cid >= personPeerGroups.size())
            {
                LOG_TRACE("No priors for attribute = " << gatherer.attributeName(cid)
                          << " and feature = " << model_t::print(feature));
                continue;
            }
            TSizeSizeUMapCItr priorItr = personPeerGroups[cid].find(pid);
            if (   priorItr == personPeerGroups[cid].end()
                || priorItr->second >= peerGroupPriors[cid].size())
            {
                LOG_TRACE("Peer group not known for = " << gatherer.personName(pid)
                          << " and feature = " << model_t::print(feature));
                continue;
            }
            const maths::CPrior &prior = *peerGroupPriors[cid][priorItr->second];

            const TFeatureData &dj = CDataGatherer::extractData(featureData[j]);

            core_t::TTime elapsedTime = sampleTime - this->attributeFirstBucketTimes()[cid];
            uint64_t count = dj.s_Count;

            params.s_Feature = feature;
            params.s_Trend = this->trend(feature, cid);
            params.s_Prior = &prior;
            params.s_ElapsedTime = elapsedTime;
            params.s_Time = sampleTime;
            params.s_Value.assign(1, model_t::offsetCountToZero(feature, static_cast<double>(count)));
            params.s_Count = 1.0;
            params.s_Sample = this->detrend(feature, cid, sampleTime, SEASONAL_CONFIDENCE_INTERVAL, params.s_Value);
            params.s_Weights[0] = this->seasonalVarianceScale(feature, cid, sampleTime, SEASONAL_CONFIDENCE_INTERVAL).second;
            params.s_BucketEmpty = false;
            params.s_ProbabilityBucketEmpty = 0.0;
            params.s_Confidence = SEASONAL_CONFIDENCE_INTERVAL;
            if (result.isInterim() && model_t::requiresInterimResultAdjustment(feature))
            {
                // TODO Think how corrections should be calculated for peers
                double mode = prior.marginalLikelihoodMode(SEASONAL_VARIANCE_WEIGHT, params.s_Weights[0]);
                TDouble1Vec corrections(1, this->interimValueCorrector().corrections(
                                                         sampleTime,
                                                         this->currentBucketTotalCount(),
                                                         mode, params.s_Sample[0]));
                params.s_Value  += corrections;
                params.s_Sample += corrections;
                this->currentBucketInterimCorrections().emplace(core::make_triple(feature, pid, cid), corrections);
            }

            CProbabilityAndInfluenceCalculator *calculator = 0;
            params.s_Probability = 1.0;
            params.s_Tail = maths_t::E_UndeterminedTail;
            if (   model_t::isAttributeConditional(feature)
                && pConditional.emplace(cid, pConditionalTemplate).first->second.addProbability(feature, prior, elapsedTime,
                                                                                                SEASONAL_VARIANCE_WEIGHT,
                                                                                                params.s_Sample,
                                                                                                params.s_Weights,
                                                                                                params.s_BucketEmpty,
                                                                                                params.s_ProbabilityBucketEmpty,
                                                                                                params.s_Probability,
                                                                                                params.s_Tail))
            {
                LOG_TRACE("P(" << params.describe()
                          << ", attribute = " << gatherer.attributeName(cid)
                          << ", person = " << gatherer.personName(pid) << ") = "
                          << params.s_Probability);
                calculator = &pConditional.emplace(cid, pConditionalTemplate).first->second;
                features[cid].add(TDoubleFeaturePr(params.s_Probability, feature));
            }
            else if (  !model_t::isAttributeConditional(feature)
                     && pJoint.addProbability(feature, prior, elapsedTime,
                                              SEASONAL_VARIANCE_WEIGHT,
                                              params.s_Sample,
                                              params.s_Weights,
                                              params.s_BucketEmpty,
                                              params.s_ProbabilityBucketEmpty,
                                              params.s_Probability,
                                              params.s_Tail))
            {
                LOG_TRACE("P(" << params.describe()
                          << ", person = " << gatherer.personName(pid) << ") = "
                          << params.s_Probability);
                calculator = &pJoint;
                resultBuilder.addAttributeProbability(cid, gatherer.attributeNamePtr(cid),
                                                      1.0, params.s_Probability,
                                                      model_t::CResultType::E_Unconditional,
                                                      feature, NO_CORRELATED_ATTRIBUTES, NO_CORRELATES);
            }
            else
            {
                LOG_ERROR("Unable to compute P(" << params.describe()
                          << ", attribute = " << gatherer.attributeName(cid)
                          << ", person = " << gatherer.personName(pid) << ")");
            }

            if (calculator && !dj.s_InfluenceValues.empty())
            {
                for (std::size_t k = 0u; k < dj.s_InfluenceValues.size(); ++k)
                {
                    if (const CInfluenceCalculator *influenceCalculator = this->influenceCalculator(feature, k))
                    {
                        calculator->plugin(*influenceCalculator);
                        calculator->addInfluences(*(gatherer.beginInfluencers() + k), dj.s_InfluenceValues[k], params);
                    }
                }
            }
        }
    }

    // TODO Need projection clustering for attribute probabilities.
    //const CTools::CLessLikelyProbability &pAttributes = this->attributeProbabilities();
    for (TSizeProbabilityAndInfluenceUMapCItr i = pConditional.begin();
         i != pConditional.end();
         ++i)
    {
        std::size_t cid = i->first;

        double pPersonGivenAttribute;
        if (!i->second.calculate(pPersonGivenAttribute))
        {
            LOG_ERROR("Unable to compute P(" << gatherer.personName(pid)
                      << " | " << gatherer.attributeName(cid) << ")");
            continue;
        }

        CProbabilityAndInfluenceCalculator pPersonAndAttribute(this->params().s_InfluenceCutoff);
        pPersonAndAttribute.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
        pPersonAndAttribute.add(i->second);
        // TODO Need projection clustering for attribute probabilities.
        //double pAttribute;
        //if (pAttributes.lookup(cid, pAttribute))
        //{
        //    pPersonAndAttribute.addProbability(pAttribute);
        //}
        //LOG_TRACE("P(" << gatherer.attributeName(cid) << ") = " << pAttribute);
        LOG_TRACE("P(" << gatherer.personName(pid)
                  << " | " << gatherer.attributeName(cid)
                  << ") = " << pPersonGivenAttribute);

        // The idea is we imagine drawing n samples from the person's total
        // attribute set, where n is the size of the person's attribute set,
        // and we weight each sample according to the probability it occurs
        // assuming the attributes are distributed according to the supplied
        // multinomial distribution.
        double w = 1.0;
        double pAttributeGivenPerson;
        if (personAttributeProbabilityPrior.probability(static_cast<double>(cid), pAttributeGivenPerson))
        {
            w = maths::CCategoricalTools::probabilityOfCategory(pConditional.size(), pAttributeGivenPerson);
        }
        LOG_TRACE("w = " << w);

        pJoint.add(pPersonAndAttribute, w);

        TSizeDoubleFeaturePrMinAccumulatorUMapCItr featureItr = features.find(cid);
        if (featureItr == features.end())
        {
            LOG_ERROR("No feature for " << gatherer.attributeName(cid));
            continue;
        }
        double p;
        pPersonAndAttribute.calculate(p);
        // TODO Need projection clustering for attribute probabilities.
        resultBuilder.addAttributeProbability(cid, gatherer.attributeNamePtr(cid),
                                              1.0, p,
                                              model_t::CResultType::E_Unconditional,
                                              (featureItr->second)[0].second,
                                              NO_CORRELATED_ATTRIBUTES, NO_CORRELATES);
    }

    double p;
    if (!pJoint.calculate(p, result.s_Influences))
    {
        LOG_ERROR("Unable to compute probability of " << gatherer.personName(pid));
        return false;
    }
    resultBuilder.probability(p);
    LOG_TRACE("probability(" << gatherer.personName(pid) << ") = " << p);

    resultBuilder.build();

    return true;
}

bool CEventRatePeersModel::computeTotalProbability(const std::string &/*person*/,
                                                   std::size_t /*numberAttributeProbabilities*/,
                                                   TOptionalDouble &/*probability*/,
                                                   TAttributeProbability1Vec &/*attributeProbabilities*/) const
{
    // TODO
    return true;
}

void CEventRatePeersModel::outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                         const TBucketStatsOutputFunc &outputFunc) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    const std::string &partitionFieldName = gatherer.partitionFieldName();
    const std::string &personFieldName = gatherer.personFieldName();
    const std::string &attributeFieldName = gatherer.attributeFieldName();

    const TFeatureSizeSizePrFeatureDataPrVecMap &featureData = m_CurrentBucketStats.s_FeatureData;
    for (TFeatureSizeSizePrFeatureDataPrVecMapCItr itr = featureData.begin();
         itr != featureData.end();
         ++itr)
    {
        const std::string &funcName = model_t::outputFunctionName(itr->first);
        const TSizeSizePrFeatureDataPrVec &data = itr->second;
        for (std::size_t i = 0u; i < data.size(); ++i)
        {
            outputFunc(
                SOutputStats(
                    m_CurrentBucketStats.s_StartTime,
                    true,
                    false,
                    partitionFieldName,
                    partitionFieldValue,
                    personFieldName,
                    gatherer.personName(CDataGatherer::extractPersonId(data[i]), EMPTY_STRING),
                    attributeFieldName,
                    gatherer.attributeName(CDataGatherer::extractAttributeId(data[i]), EMPTY_STRING),
                    EMPTY_STRING,
                    funcName,
                    static_cast<double>(data[i].second.s_Count),
                    true
                )
            );
        }
    }
}

uint64_t CEventRatePeersModel::checksum(bool includeCurrentBucketStats) const
{
    uint64_t seed = this->CPopulationModel::checksum(includeCurrentBucketStats);
    seed = maths::CChecksum::calculate(seed, m_NewAttributeProbabilityPrior);
    if (includeCurrentBucketStats)
    {
        seed = maths::CChecksum::calculate(seed, m_CurrentBucketStats.s_StartTime);
    }

    TStrCRefStrCRefPrUInt64Map hashes;

    const CDataGatherer &gatherer = this->dataGatherer();

    detail::checksum(gatherer, m_FeatureMoments, hashes);
    detail::checksum(gatherer, m_FeaturePeerGroups, hashes);
    for (TFeaturePriorPtrVecVecMapCItr itr = m_FeaturePeerGroupPriors.begin();
         itr != m_FeaturePeerGroupPriors.end();
         ++itr)
    {
        for (std::size_t cid = 0u; cid < itr->second.size(); ++cid)
        {
            if (gatherer.isAttributeActive(cid))
            {
#define ATTRIBUTE_KEY(cid) TStrCRefStrCRefPr(boost::cref(EMPTY_STRING), \
                                             boost::cref(gatherer.attributeName(cid)))
                uint64_t &hash = hashes[ATTRIBUTE_KEY(cid)];
#undef ATTRIBUTE_KEY
                hash = maths::CChecksum::calculate(hash, itr->second[cid]);
            }
        }
    }
    detail::checksum(gatherer, m_FeatureClusterStability, hashes);

    if (includeCurrentBucketStats)
    {
        this->checksums(this->personCounts(), hashes);

        const TFeatureSizeSizePrFeatureDataPrVecMap &featureData = m_CurrentBucketStats.s_FeatureData;
        for (TFeatureSizeSizePrFeatureDataPrVecMapCItr itr = featureData.begin();
             itr != featureData.end();
             ++itr)
        {
            for (std::size_t i = 0u; i < itr->second.size(); ++i)
            {
#define KEY(pid, cid) TStrCRefStrCRefPr(boost::cref(gatherer.personName(pid)), \
                                        boost::cref(gatherer.attributeName(cid)))
                uint64_t &hash = hashes[KEY(CDataGatherer::extractPersonId(itr->second[i]),
                                            CDataGatherer::extractAttributeId(itr->second[i]))];
#undef KEY
                hash = maths::CChecksum::calculate(
                           hash,
                           CDataGatherer::extractData(itr->second[i]).s_Count);
            }
        }
    }


    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes = " << core::CContainerPrinter::print(hashes));

    return maths::CChecksum::calculate(seed, hashes);
}

void CEventRatePeersModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CEventRatePeersModel");
    this->CPopulationModel::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_PersonCounts",
                                    m_CurrentBucketStats.s_PersonCounts, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_FeatureData",
                                    m_CurrentBucketStats.s_FeatureData, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_InterimCorrections",
                                    m_CurrentBucketStats.s_InterimCorrections, mem);
    core::CMemoryDebug::dynamicSize("m_NewPersonAttributePrior",
                                    m_NewAttributeProbabilityPrior, mem);
    core::CMemoryDebug::dynamicSize("m_FeatureAttributeMoments",
                                    m_FeatureMoments, mem);
    core::CMemoryDebug::dynamicSize("m_FeaturePersonPeerGroups",
                                    m_FeaturePeerGroups, mem);
    core::CMemoryDebug::dynamicSize("m_FeaturePeerGroupModels",
                                    m_FeaturePeerGroupPriors, mem);
    core::CMemoryDebug::dynamicSize("m_FeatureClusterStability",
                                    m_FeatureClusterStability, mem);
}

std::size_t CEventRatePeersModel::memoryUsage(void) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    return this->estimateMemoryUsage(gatherer.numberActivePeople(),
                                     gatherer.numberActiveAttributes(),
                                     0); // # correlations
}

std::size_t CEventRatePeersModel::computeMemoryUsage(void) const
{
    std::size_t mem = this->CPopulationModel::memoryUsage();
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_PersonCounts);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_FeatureData);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_InterimCorrections);
    mem += core::CMemory::dynamicSize(m_NewAttributeProbabilityPrior);
    mem += core::CMemory::dynamicSize(m_FeatureMoments);
    mem += core::CMemory::dynamicSize(m_FeaturePeerGroups);
    mem += core::CMemory::dynamicSize(m_FeaturePeerGroupPriors);
    mem += core::CMemory::dynamicSize(m_FeatureClusterStability);
    return mem;
}

std::size_t CEventRatePeersModel::estimateMemoryUsage(std::size_t numberPeople,
                                                      std::size_t numberAttributes,
                                                      std::size_t numberCorrelations) const
{
    CMemoryUsageEstimator::TSizeArray predictors;
    predictors[CMemoryUsageEstimator::E_People]       = numberPeople;
    predictors[CMemoryUsageEstimator::E_Attributes]   = numberAttributes;
    predictors[CMemoryUsageEstimator::E_Correlations] = numberCorrelations;
    CMemoryUsageEstimator::TOptionalSize guess = m_MemoryEstimator.estimate(predictors);
    if (guess)
    {
        return guess.get();
    }
    else
    {
        std::size_t mem = this->computeMemoryUsage();
        m_MemoryEstimator.addValue(predictors, mem);
        return mem;
    }
}

std::size_t CEventRatePeersModel::staticSize(void) const
{
    return sizeof(*this);
}

CEventRatePeersModel::CModelDetailsViewPtr
    CEventRatePeersModel::details(void) const
{
    return CModelDetailsViewPtr(new CEventRatePeersModelDetailsView(*this));
}

const CEventRatePeersModel::TSizeSizePrFeatureDataPrVec &
    CEventRatePeersModel::featureData(model_t::EFeature feature,
                                      core_t::TTime time) const
{
    static const TSizeSizePrFeatureDataPrVec EMPTY;

    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time
                  << ", current bucket = [" << m_CurrentBucketStats.s_StartTime
                  << "," << m_CurrentBucketStats.s_StartTime + this->bucketLength() << ")");
        return EMPTY;
    }

    const TFeatureSizeSizePrFeatureDataPrVecMap &featureData = m_CurrentBucketStats.s_FeatureData;
    TFeatureSizeSizePrFeatureDataPrVecMapCItr result = featureData.find(feature);
    return result == featureData.end() ? EMPTY : result->second;
}

core_t::TTime CEventRatePeersModel::currentBucketStartTime(void) const
{
    return m_CurrentBucketStats.s_StartTime;
}

void CEventRatePeersModel::currentBucketStartTime(core_t::TTime startTime)
{
    m_CurrentBucketStats.s_StartTime = startTime;
}

uint64_t CEventRatePeersModel::currentBucketTotalCount(void) const
{
    return m_CurrentBucketStats.s_TotalCount;
}

CEventRatePeersModel::TFeatureSizeSizeTripleDouble1VecUMap &
    CEventRatePeersModel::currentBucketInterimCorrections(void) const
{
    return m_CurrentBucketStats.s_InterimCorrections;
}

void CEventRatePeersModel::createNewModels(std::size_t n, std::size_t m)
{
    const model_t::TFeatureVec &features = this->dataGatherer().features();

    for (std::size_t i = 0u; m > 0 && i < features.size(); ++i)
    {
        detail::resize(features[i], m, m_FeatureMoments);
        detail::resize(features[i], m, m_FeaturePeerGroups);
        detail::resize(features[i], m, m_FeaturePeerGroupPriors);
        detail::resize(features[i], m, m_FeatureClusterStability);
    }

    this->CPopulationModel::createNewModels(n, m);
}

void CEventRatePeersModel::updateRecycledModels(void)
{
    CDataGatherer &gatherer = this->dataGatherer();
    const TSizeVec &recycledPeople = gatherer.recycledPersonIds();
    const TSizeVec &recycledAttributes = gatherer.recycledAttributeIds();
    this->clear(recycledPeople, recycledAttributes);
    this->CPopulationModel::updateRecycledModels();
}

void CEventRatePeersModel::clearPrunedResources(const TSizeVec &people,
                                                const TSizeVec &attributes)
{
    this->clear(people, attributes);
    this->CPopulationModel::clearPrunedResources(people, attributes);
}

void CEventRatePeersModel::clear(const TSizeVec &people,
                                 const TSizeVec &attributes)
{
    for (std::size_t i = 0u; i < attributes.size(); ++i)
    {
        std::size_t cid = attributes[i];
        detail::clearAttribute(cid, m_FeatureMoments);
        detail::clearAttribute(cid, m_FeaturePeerGroups);
        detail::clearAttribute(cid, m_FeaturePeerGroupPriors);
        detail::clearAttribute(cid, m_FeatureClusterStability);
    }
    for (std::size_t i = 0u; i < people.size(); ++i)
    {
        std::size_t pid = people[i];
        detail::clearPerson(pid, m_FeatureMoments);
        detail::clearPerson(pid, m_FeaturePeerGroups);
        detail::clearPerson(pid, m_FeatureClusterStability);
    }
}

void CEventRatePeersModel::updatePeerGroups(core_t::TTime time)
{
    const CDataGatherer &gatherer = this->dataGatherer();
    const model_t::TFeatureVec &features = gatherer.features();

    // Declared outside the loop to minimize the number of allocations.
    detail::SUpdatePeersState state(MAX_PEER_GROUPS);

    double alpha = ::exp(  this->params().s_DecayRate
                         * static_cast<double>(m_LastBatchUpdateTime - time)
                         / gatherer.bucketLength());

    for (std::size_t i = 0u; i < features.size(); ++i)
    {
        model_t::EFeature feature = features[i];

        TSizeMomentsUMapVec &moments = m_FeatureMoments[feature];
        TSizeSizeUMapVec &peerGroups = m_FeaturePeerGroups[feature];
        TPriorPtrVecVec &peerGroupPriors = m_FeaturePeerGroupPriors[feature];
        TSizeDoubleUMapVec &clusterStability = m_FeatureClusterStability[feature];

        // Save the current person peer groups.
        TSizeSizeUMapVec lastPeerGroups;
        lastPeerGroups.swap(peerGroups);

        for (std::size_t cid = 0u; cid < moments.size(); ++cid)
        {
            if (!gatherer.isAttributeActive(cid))
            {
                continue;
            }

            TSizeMomentsUMap &attributeMoments = moments[cid];
            TSizeSizeUMap &lastAttributePeerGroups = lastPeerGroups[cid];
            TSizeSizeUMap &attributePeerGroups = peerGroups[cid];
            TPriorPtrVec &attributePeerGroupPriors = peerGroupPriors[cid];
            TSizeDoubleUMap &attributeClusterStability = clusterStability[cid];

            // Extract the moments from which to compute points to cluster.
            detail::extractMoments(state, attributeMoments);

            // Make the points to cluster.
            detail::makePoints(state);

            // Compute the clustering.
            state.clusters.clear();
            maths::bootstrapCluster(state.points,
                                    NUMBER_BOOTSTRAPS,
                                    state.xmeans,
                                    IMPROVE_PARAMS_KMEANS_ITERATIONS,
                                    IMPROVE_STRUCTURE_CLUSTER_SEEDS,
                                    IMPROVE_STRUCTURE_KMEANS_ITERATIONS,
                                    OVERLAP_THRESHOLD,
                                    CHAINING_FACTOR,
                                    state.clusters);

            // Assign people to their new peer groups.
            detail::assignPeerGroups(state, attributePeerGroups);

            // Update the priors.
            detail::updatePeerGroupModels(state,
                                          PERSON_SAMPLES,
                                          PEER_GROUP_SAMPLES,
                                          lastAttributePeerGroups,
                                          attributePeerGroups,
                                          attributeMoments,
                                          attributeClusterStability,
                                          *this->newPrior(feature), // FIXME
                                          attributePeerGroupPriors);

            // Update the cluster stabilities.
            detail::updateClusterStability(state,
                                           lastAttributePeerGroups,
                                           attributePeerGroups,
                                           alpha,
                                           attributeClusterStability);
        }
    }
}

const maths::CPrior *CEventRatePeersModel::prior(model_t::EFeature /*feature*/,
                                                 std::size_t /*cid*/) const
{
    return 0;
}

const maths::CMultivariatePrior *
    CEventRatePeersModel::multivariatePrior(model_t::EFeature /*feature*/,
                                            std::size_t /*cid*/) const
{
    return 0;
}

bool CEventRatePeersModel::resetPrior(model_t::EFeature /*feature*/,
                                      std::size_t /*cid*/)
{
    // TODO.
    return false;
}

const CEventRatePeersModel::TPriorPtrVecVec &
    CEventRatePeersModel::peerGroupPriors(model_t::EFeature feature) const
{
    static const TPriorPtrVecVec EMPTY;
    TFeaturePriorPtrVecVecMapCItr result = m_FeaturePeerGroupPriors.find(feature);
    return result == m_FeaturePeerGroupPriors.end() ? EMPTY : result->second;
}

const CEventRatePeersModel::TSizeSizeUMapVec &
    CEventRatePeersModel::personPeerGroups(model_t::EFeature feature) const
{
    static const TSizeSizeUMapVec EMPTY;
    TFeatureSizeSizeUMapVecMapCItr result = m_FeaturePeerGroups.find(feature);
    return result == m_FeaturePeerGroups.end() ? EMPTY : result->second;
}

const CEventRatePeersModel::TSizeMomentsUMap &
    CEventRatePeersModel::attributeMoments(model_t::EFeature feature,
                                           std::size_t cid) const
{
    static const TSizeMomentsUMap EMPTY;

    TFeatureSizeMomentsUMapVecMapCItr result = m_FeatureMoments.find(feature);
    if (result == m_FeatureMoments.end())
    {
        LOG_ERROR("No moments for " << model_t::print(feature));
        return EMPTY;
    }

    if (cid >= result->second.size())
    {
        LOG_ERROR("No moments for attribute " << this->attributeName(cid));
        return EMPTY;
    }

    return result->second[cid];
}

const CEventRatePeersModel::TSizeUInt64PrVec &
    CEventRatePeersModel::personCounts(void) const
{
    return m_CurrentBucketStats.s_PersonCounts;
}

void CEventRatePeersModel::currentBucketTotalCount(uint64_t totalCount)
{
    m_CurrentBucketStats.s_TotalCount = totalCount;
}

bool CEventRatePeersModel::bucketStatsAvailable(core_t::TTime time) const
{
    return    time >= m_CurrentBucketStats.s_StartTime
           && time <  m_CurrentBucketStats.s_StartTime + this->bucketLength();
}

const core_t::TTime CEventRatePeersModel::BATCH_UPDATE_PERIOD = core::constants::WEEK;
const std::size_t CEventRatePeersModel::NUMBER_BOOTSTRAPS = 5u;
const std::size_t CEventRatePeersModel::IMPROVE_PARAMS_KMEANS_ITERATIONS = 5u;
const std::size_t CEventRatePeersModel::IMPROVE_STRUCTURE_CLUSTER_SEEDS = 3u;
const std::size_t CEventRatePeersModel::IMPROVE_STRUCTURE_KMEANS_ITERATIONS = 4u;
const double CEventRatePeersModel::OVERLAP_THRESHOLD = 0.3;
const double CEventRatePeersModel::CHAINING_FACTOR = 3.0;
const std::size_t CEventRatePeersModel::MAX_PEER_GROUPS = 200u;
const std::size_t CEventRatePeersModel::PERSON_SAMPLES = 5u;
const std::size_t CEventRatePeersModel::PEER_GROUP_SAMPLES = 500u;

////////// CEventRatePeersModel::SBucketStats Implementation //////////

CEventRatePeersModel::SBucketStats::SBucketStats(core_t::TTime startTime) :
        s_StartTime(startTime),
        s_PersonCounts(),
        s_TotalCount(0),
        s_FeatureData(),
        s_InterimCorrections(1)
{
}

}
}
