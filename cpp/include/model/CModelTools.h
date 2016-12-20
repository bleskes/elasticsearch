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

#ifndef INCLUDED_prelert_model_CModelTools_h
#define INCLUDED_prelert_model_CModelTools_h

#include <core/CAllocationStrategy.h>
#include <core/CLogger.h>
#include <core/CSmallVector.h>

#include <maths/CMultivariatePrior.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/ProbabilityAggregators.h>

#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <boost/optional.hpp>
#include <boost/unordered_map.hpp>
#include <boost/variant.hpp>

#include <cstddef>
#include <utility>
#include <vector>

namespace prelert
{
namespace maths
{
class CMultinomialConjugate;
}
namespace model
{
struct SModelParams;

//! \brief A collection of utility functionality for the CModel hierarchy.
//!
//! DESCRIPTION:\n
//! A collection of utility functions primarily intended for use by the
//! CModel class hierarchy.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This class is really just a proxy for a namespace, but a class has
//! been intentionally used to provide a single point for the declaration
//! and definition of utility functions within the model library. As such
//! all member functions should be static and it should be state-less.
//! If your functionality doesn't fit this pattern just make it a nested
//! class.
class MODEL_EXPORT CModelTools
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
        typedef boost::shared_ptr<const std::string> TStrPtr;
        typedef std::pair<TStrPtr, TStrPtr> TStrPtrStrPtrPr;

        //! \brief Hashes a string pointer pair.
        struct MODEL_EXPORT SStrPtrStrPtrPrHash
        {
            std::size_t operator()(const TStrPtrStrPtrPr &target) const
            {
                return static_cast<std::size_t>(
                           core::CHashing::hashCombine(static_cast<uint64_t>(s_Hasher(*target.first)),
                                                       static_cast<uint64_t>(s_Hasher(*target.second))));
            }
            core::CHashing::CMurmurHash2String s_Hasher;
        };

        //! \brief Compares two string pointer pairs.
        struct MODEL_EXPORT SStrPtrStrPtrPrEqual
        {
            std::size_t operator()(const TStrPtrStrPtrPr &lhs,
                                   const TStrPtrStrPtrPr &rhs) const
            {
                return *lhs.first == *rhs.first && *lhs.second == *rhs.second;
            }
        };

        //! \brief Manages the aggregation of probabilities.
        //!
        //! DESCRIPTION:\n
        //! This allows one to register either one of or both the joint
        //! probability and extreme aggregation styles. The resulting
        //! aggregate probability is the minimum of the aggregates of
        //! the probabilities added so far for any of the registered
        //! aggregation styles.
        class MODEL_EXPORT CProbabilityAggregator
        {
            public:
                typedef boost::variant<maths::CJointProbabilityOfLessLikelySamples,
                                       maths::CProbabilityOfExtremeSample> TAggregator;
                typedef std::pair<TAggregator, double> TAggregatorDoublePr;
                typedef std::vector<TAggregatorDoublePr> TAggregatorDoublePrVec;

                enum EStyle
                {
                    E_Sum,
                    E_Min
                };

            public:
                CProbabilityAggregator(EStyle style);

                //! Check if any probabilities have been added.
                bool empty(void) const;

                //! Add an aggregation style \p aggregator with weight \p weight.
                void add(const TAggregator &aggregator, double weight = 1.0);

                //! Add \p probability.
                void add(double probability, double weight = 1.0);

                //! Calculate the probability if possible.
                bool calculate(double &result) const;

            private:
                //! The style of aggregation to use.
                EStyle m_Style;

                //! The total weight of all samples.
                double m_TotalWeight;

                //! The collection of objects for computing "joint" probabilities.
                TAggregatorDoublePrVec m_Aggregators;
        };

        typedef boost::unordered_map<TStrPtrStrPtrPr,
                                     CProbabilityAggregator,
                                     SStrPtrStrPtrPrHash,
                                     SStrPtrStrPtrPrEqual> TStrPtrStrPtrPrProbabilityAggregatorUMap;
        typedef TStrPtrStrPtrPrProbabilityAggregatorUMap::iterator TStrPtrStrPtrPrProbabilityAggregatorUMapItr;
        typedef TStrPtrStrPtrPrProbabilityAggregatorUMap::const_iterator TStrPtrStrPtrPrProbabilityAggregatorUMapCItr;

    public:
        //! Copy the new priors \p priors cloning those priors which will
        //! need updating with empty buckets.
        template<typename T>
        static void copyAllNewPriors(const T &source, T &target)
        {
            typedef typename T::value_type PAIR;

            target.reserve(source.size());
            for (std::size_t i = 0u; i < source.size(); ++i)
            {
                if (model_t::newPriorFixed(source[i].first))
                {
                    target.push_back(source[i]);
                }
                else
                {
                    target.push_back(PAIR(source[i].first,
                                          typename PAIR::second_type(source[i].second->clone())));
                }
            }
            std::sort(target.begin(), target.end(), maths::COrderings::SFirstLess());
        }

        //! Shallow copy the priors which don't need updating with empty
        //! buckets.
        template<typename T>
        static void shallowCopyConstantNewPriors(const T &source, T &target)
        {
            for (std::size_t i = 0u; i < source.size(); ++i)
            {
                if (model_t::newPriorFixed(source[i].first))
                {
                    target.push_back(source[i]);
                }
            }
            std::sort(target.begin(), target.end(), maths::COrderings::SFirstLess());
        }

        //! Copy the subset of new priors \p source which need cloning.
        template<typename T>
        static void cloneVaryingNewPriors(const T &source, T &target)
        {
            typedef typename T::value_type TPr;

            target.reserve(source.size());
            for (std::size_t i = 0u; i < source.size(); ++i)
            {
                if (!model_t::newPriorFixed(source[i].first))
                {
                    target.push_back(TPr(source[i].first,
                                         typename TPr::second_type(source[i].second->clone())));
                }
            }
        }

        //! Update the new priors \p priors with an empty bucket.
        template<typename T>
        static void updateNewPriorsWithEmptyBucket(const SModelParams &params, T &priors)
        {
            for (std::size_t i = 0u; i < priors.size(); ++i)
            {
                model_t::EFeature feature = priors[i].first;
                if (!model_t::newPriorFixed(feature))
                {
                    model_t::updateNewPriorWithEmptyBuckets(feature, 1,
                                                            model_t::learnRate(feature, params),
                                                            *priors[i].second);
                    priors[i].second->propagateForwardsByTime(1.0);
                }
            }
        }

        //! Debug the memory used by the new priors \p priors.
        template<typename T>
        static void debugNewPriorsMemoryUsage(const std::string &name,
                                              const T &priors,
                                              core::CMemoryUsage::TMemoryUsagePtr mem)
        {
            core::CMemoryUsage::SMemoryUsage usage(name + "::" + typeid(priors).name(),
                                                   sizeof(typename T::value_type) *  priors.capacity(),
                                                   sizeof(typename T::value_type) * (priors.capacity() - priors.size()));
            mem->addChild()->setName(usage);
            std::string itemName = name + "_item";
            for (std::size_t i = 0u; i < priors.size(); ++i)
            {
                if (!model_t::newPriorFixed(priors[i].first))
                {
                    core::CMemoryDebug::dynamicSize(itemName.c_str(), priors[i].second, mem);
                }
            }
        }

        //! Get the memory used by the new priors \p priors.
        template<typename T>
        static std::size_t newPriorsMemoryUsage(const T &priors)
        {
            std::size_t mem = sizeof(typename T::value_type) * priors.capacity();
            for (std::size_t i = 0u; i < priors.size(); ++i)
            {
                if (!model_t::newPriorFixed(priors[i].first))
                {
                    mem += core::CMemory::dynamicSize(priors[i].second);
                }
            }
            return mem;
        }

        //! Clone the priors from \p source to \p target.
        template<typename T>
        static void clonePriors(const T &source, T &target)
        {
            typedef typename T::mapped_type TMappedType;
            for (typename T::const_iterator i = source.begin(); i != source.end(); ++i)
            {
                TMappedType &priors = target[i->first];
                const TMappedType &otherPriors = i->second;
                priors.reserve(otherPriors.size());
                for (std::size_t j = 0u; j < otherPriors.size(); ++j)
                {
                    priors.push_back(typename TMappedType::value_type(otherPriors[j]->clone()));
                }
            }
        }

        //! Update \p prior with \p samples.
        template<typename U, typename V, typename PRIOR>
        static void updatePrior(const maths_t::TWeightStyleVec &weightStyles,
                                const U &samples,
                                const V &weights,
                                double interval,
                                PRIOR &prior)
        {
            prior.addSamples(weightStyles, samples, weights);
            prior.propagateForwardsByTime(interval);
            LOG_TRACE(prior.print());
        }

        //! Resize \p priors to accommodate \p n new identifiers.
        template<typename U, typename V>
        static void createPriors(std::size_t n, const U &newPriors, V &priors)
        {
            std::size_t i = 0u;
            for (typename V::iterator j = priors.begin(); j != priors.end(); ++i, ++j)
            {
                std::size_t id = j->second.size();
                std::size_t n_ = model_t::isAttributeConditional(j->first) ? id + n : 1;
                if (n_ >= j->second.size())
                {
                    core::CAllocationStrategy::resize(j->second, n_);
                    for (/**/; id < j->second.size(); ++id)
                    {
                        j->second[id].reset(newPriors[i].second->clone());
                    }
                }
            }
        }

        //! Reset the priors of identified by \p id.
        template<typename U, typename V>
        static void resetRecycledPriors(std::size_t id, const U &newPriors, V &priors)
        {
            std::size_t i = 0u;
            for (typename V::iterator j = priors.begin(); j != priors.end(); ++i, ++j)
            {
                if (i > newPriors.size() || j->first != newPriors[i].first)
                {
                    LOG_ERROR("Unexpected feature: " << j->first);
                    continue;
                }
                if (id >= j->second.size())
                {
                    LOG_ERROR("Unexpected id: " << id);
                    continue;
                }
                j->second[id].reset(newPriors[i].second->clone());
            }
        }

        //! Get the index range [begin, end) of the person corresponding to
        //! \p pid in the vector \p data. This relies on the fact that \p data
        //! is sort lexicographically by person then attribute identifier.
        //! This will return an empty range if the person is not present.
        template<typename T>
        static TSizeSizePr personRange(const T &data, std::size_t pid)
        {
            const std::size_t minCid = 0u;
            const std::size_t maxCid = std::numeric_limits<std::size_t>::max();
            typename T::const_iterator begin = std::lower_bound(data.begin(), data.end(),
                                                                std::make_pair(pid, minCid),
                                                                maths::COrderings::SFirstLess());
            typename T::const_iterator end = std::upper_bound(begin, data.end(),
                                                              std::make_pair(pid, maxCid),
                                                              maths::COrderings::SFirstLess());
            return TSizeSizePr(static_cast<std::size_t>(begin - data.begin()),
                               static_cast<std::size_t>(end - data.begin()));
        }

        //! Get the nearest mean of \p prior to \p detrended.
        template<typename VECTOR>
        static VECTOR nearestMarginalLikelihoodMean(const maths::CPrior &prior,
                                                    const VECTOR &detrended)
        {
            return VECTOR(1, prior.nearestMarginalLikelihoodMean(detrended[0]));
        }

        //! Get the nearest mean of \p prior to \p detrended.
        template<typename VECTOR>
        static VECTOR nearestMarginalLikelihoodMean(const maths::CMultivariatePrior &prior,
                                                    const VECTOR &detrended)
        {
            return prior.nearestMarginalLikelihoodMean(detrended);
        }

        //! Get the error in the trend prediction for \p sample.
        template<typename TREND, typename VECTOR>
        static boost::optional<VECTOR> predictionResidual(const TREND &trend,
                                                          const VECTOR &sample)
        {
            boost::optional<VECTOR> result;

            std::size_t dimension = trend.size();
            for (std::size_t i = 0u; i < dimension; ++i)
            {
                if (trend[i]->initialized())
                {
                    result.reset(VECTOR(dimension, 0.0));
                    for (/**/; i < dimension; ++i)
                    {
                        if (trend[i]->initialized())
                        {
                            (*result)[i] = sample[i] - trend[i]->level();
                        }
                    }
                }
            }

            return result;
        }

        //! Get the error in the prior prediction for \p sample.
        template<typename PRIOR, typename VECTOR>
        static boost::optional<VECTOR> predictionResidual(double propagationInterval,
                                                          const PRIOR &prior,
                                                          const VECTOR &sample)
        {
            boost::optional<VECTOR> result;
            if (prior.numberSamples() > 50.0 / propagationInterval)
            {
                result.reset(sample);
                *result -= nearestMarginalLikelihoodMean(prior, sample);
            }
            return result;
        }

        //! Wraps up the calculation of less likely probabilities for a
        //! multinomial distribution.
        //!
        //! DESCRIPTION:\n
        //! This caches the probabilities for each category, in the multinomial
        //! distribution, since they can't be computed independently and for
        //! a large number of categories it is very wasteful to repeatedly
        //! compute them all.
        class MODEL_EXPORT CLessLikelyProbability
        {
            public:
                CLessLikelyProbability(void);
                CLessLikelyProbability(const maths::CMultinomialConjugate &prior);

                //! Calculate the probability of less likely categories than
                //! \p attribute.
                bool lookup(std::size_t category, double &result) const;

                //! Get the memory usage of the component
                void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

                //! Get the memory usage of the component
                std::size_t memoryUsage(void) const;

            private:
                //! The prior.
                const maths::CMultinomialConjugate *m_Prior;
                //! The cached probabilities.
                mutable TDoubleVec m_Cache;
        };
};

}
}

#endif // INCLUDED_prelert_model_CModelTools_h
