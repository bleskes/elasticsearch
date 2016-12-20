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

#include <maths/CMultivariateOneOfNPrior.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CStringUtils.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/CDistributionRestoreParams.h>
#include <maths/CMathsFuncs.h>
#include <maths/COneOfNPrior.h>
#include <maths/Constants.h>
#include <maths/CPriorStateSerialiser.h>
#include <maths/CSampling.h>
#include <maths/CTools.h>

#include <boost/bind.hpp>
#include <boost/numeric/conversion/bounds.hpp>
#include <boost/ref.hpp>

#include <algorithm>
#include <iterator>

#include <math.h>

namespace prelert
{
namespace maths
{
namespace
{

typedef core::CSmallVectorBool<3> TBool3Vec;
typedef CMultivariateOneOfNPrior::TDouble3Vec TDouble3Vec;
typedef CMultivariateOneOfNPrior::TDouble10Vec TDouble10Vec;
typedef CMultivariateOneOfNPrior::TDouble10VecDouble10VecPr TDouble10VecDouble10VecPr;
typedef CMultivariateOneOfNPrior::TDouble10Vec1Vec TDouble10Vec1Vec;
typedef CMultivariateOneOfNPrior::TDouble10Vec10Vec TDouble10Vec10Vec;
typedef CMultivariateOneOfNPrior::TDouble10Vec4Vec1Vec TDouble10Vec4Vec1Vec;
typedef CMultivariateOneOfNPrior::TPriorPtr TPriorPtr;
typedef CMultivariateOneOfNPrior::TWeightPriorPtrPr TWeightPriorPtrPr;
typedef CMultivariateOneOfNPrior::TWeightPriorPtrPrVec TWeightPriorPtrPrVec;

// We obfuscate the XML element names to avoid giving away too much
// information about our model.
const std::string MODEL_TAG("a");
const std::string NUMBER_SAMPLES_TAG("b");
const std::string WEIGHT_TAG("c");
const std::string PRIOR_TAG("d");
const std::string DECAY_RATE_TAG("e");

//! Add elements of \p x to \p y.
void add(const TDouble10Vec &x, TDouble10Vec &y)
{
    for (std::size_t i = 0u; i < x.size(); ++i)
    {
        y[i] += x[i];
    }
}

//! Get the min of \p x and \p y.
TDouble10Vec min(const TDouble10Vec &x, const TDouble10Vec &y)
{
    TDouble10Vec result(x);
    for (std::size_t i = 0u; i < x.size(); ++i)
    {
        result[i] = std::min(result[i], y[i]);
    }
    return result;
}

//! Get the max of \p x and \p y.
TDouble10Vec max(const TDouble10Vec &x, const TDouble10Vec &y)
{
    TDouble10Vec result(x);
    for (std::size_t i = 0u; i < x.size(); ++i)
    {
        result[i] = std::max(result[i], y[i]);
    }
    return result;
}

//! Update the arithmetic mean \p mean with \p x and weight \p nx.
void updateMean(const TDouble10Vec &x, double nx, TDouble10Vec &mean, double &n)
{
    if (nx <= 0.0)
    {
        return;
    }
    for (std::size_t i = 0u; i < x.size(); ++i)
    {
        mean[i] = (n * mean[i] + nx * x[i]) / (n + nx);
    }
    n += nx;
}

//! Update the arithmetic mean \p mean with \p x and weight \p nx.
void updateMean(const TDouble10Vec10Vec &x, double nx, TDouble10Vec10Vec &mean, double &n)
{
    if (nx <= 0.0)
    {
        return;
    }
    for (std::size_t i = 0u; i < x.size(); ++i)
    {
        for (std::size_t j = 0u; j < x[i].size(); ++j)
        {
            mean[i][j] = (n * mean[i][j] + nx * x[i][j]) / (n + nx);
        }
    }
    n += nx;
}

//! Get the largest element of \p x.
double largest(const TDouble10Vec &x)
{
    return *std::max_element(x.begin(), x.end());
}

//! Add a model vector entry reading parameters from \p traverser.
bool modelAcceptRestoreTraverser(const SDistributionRestoreParams &params,
                                 TWeightPriorPtrPrVec &models,
                                 core::CStateRestoreTraverser &traverser)
{
    CModelWeight weight(1.0);
    bool gotWeight = false;
    TPriorPtr model;

    do
    {
        const std::string &name = traverser.name();
        RESTORE_SETUP_TEARDOWN(WEIGHT_TAG,
                               /**/,
                               traverser.traverseSubLevel(boost::bind(&CModelWeight::acceptRestoreTraverser,
                                                                      &weight, _1)),
                               gotWeight = true)
        RESTORE(PRIOR_TAG,
                traverser.traverseSubLevel(boost::bind<bool>(CPriorStateSerialiser(),
                                                             boost::cref(params),
                                                             boost::ref(model), _1)))
    }
    while (traverser.next());

    if (!gotWeight)
    {
        LOG_ERROR("No weight found");
        return false;
    }
    if (model == 0)
    {
        LOG_ERROR("No model found");
        return false;
    }

    models.push_back(TWeightPriorPtrPr(weight, model));

    return true;
}

//! Read the models and number of samples from the supplied traverser.
bool acceptRestoreTraverser(const SDistributionRestoreParams &params,
                            TWeightPriorPtrPrVec &models,
                            double &decayRate,
                            double &numberSamples,
                            core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_BUILT_IN(DECAY_RATE_TAG, decayRate)
        RESTORE(MODEL_TAG,
                traverser.traverseSubLevel(boost::bind(&modelAcceptRestoreTraverser,
                                                       boost::cref(params),
                                                       boost::ref(models), _1)))
        RESTORE_BUILT_IN(NUMBER_SAMPLES_TAG, numberSamples)
    }
    while (traverser.next());

    if (CScopeDisableNormalizeOnRestore::normalizeOnRestore())
    {
        CScopeNormalizeWeights<TPriorPtr> normalizer(models);
    }

    return true;
}

//! Persist state for one of the models by passing information
//! to the supplied inserter.
void modelAcceptPersistInserter(const CModelWeight &weight,
                                const CMultivariatePrior &prior,
                                core::CStatePersistInserter &inserter)
{
    inserter.insertLevel(WEIGHT_TAG, boost::bind(&CModelWeight::acceptPersistInserter, &weight, _1));
    inserter.insertLevel(PRIOR_TAG, boost::bind<void>(CPriorStateSerialiser(), boost::cref(prior), _1));
}

const double MINUS_INF = boost::numeric::bounds<double>::lowest();
const double INF = boost::numeric::bounds<double>::highest();
const double LOG_INITIAL_WEIGHT = ::log(1e-6);
const double MINIMUM_SIGNIFICANT_WEIGHT = 0.01;

}

CMultivariateOneOfNPrior::CMultivariateOneOfNPrior(std::size_t dimension,
                                                   const TPriorPtrVec &models,
                                                   maths_t::EDataType dataType,
                                                   double decayRate) :
        CMultivariatePrior(dataType, decayRate),
        m_Dimension(dimension)
{
    if (models.empty())
    {
        LOG_ERROR("Can't initialize one-of-n with no models!");
        return;
    }

    std::size_t numberModels = models.size();

    // Non-informative prior for the weights uses equal probability for all models.
    CModelWeight weight(1.0 / static_cast<double>(numberModels));

    // Create a new model vector using uniform weights.
    m_Models.reserve(numberModels);
    for (std::size_t i = 0u; i < models.size(); ++i)
    {
        m_Models.push_back(TWeightPriorPtrPr(weight, models[i]));
    }
}

CMultivariateOneOfNPrior::CMultivariateOneOfNPrior(std::size_t dimension,
                                                   const TDoublePriorPtrPrVec &models,
                                                   maths_t::EDataType dataType,
                                                   double decayRate) :
        CMultivariatePrior(dataType, decayRate),
        m_Dimension(dimension)
{
    if (models.empty())
    {
        LOG_ERROR("Can't initialize mixed model with no models!");
        return;
    }

    CScopeNormalizeWeights<TPriorPtr> normalizer(m_Models);

    // Create a new model vector using the specified models and their associated weights.
    m_Models.reserve(models.size());
    for (std::size_t i = 0u; i < models.size(); ++i)
    {
        m_Models.push_back(TWeightPriorPtrPr(CModelWeight(models[i].first), models[i].second));
    }
}

CMultivariateOneOfNPrior::CMultivariateOneOfNPrior(std::size_t dimension,
                                                   const SDistributionRestoreParams &params,
                                                   core::CStateRestoreTraverser &traverser) :
        CMultivariatePrior(params.s_DataType, params.s_DecayRate),
        m_Dimension(dimension)
{
    double decayRate;
    double numberSamples;
    if (traverser.traverseSubLevel(boost::bind(&acceptRestoreTraverser,
                                               boost::cref(params),
                                               boost::ref(m_Models),
                                               boost::ref(decayRate),
                                               boost::ref(numberSamples), _1)) == false)
    {
        return;
    }
    this->decayRate(decayRate);
    this->numberSamples(numberSamples);
}

CMultivariateOneOfNPrior::CMultivariateOneOfNPrior(const CMultivariateOneOfNPrior &other) :
        CMultivariatePrior(other.dataType(), other.decayRate()),
        m_Dimension(other.m_Dimension)
{
    // Clone all the models up front so we can implement strong exception safety.
    TWeightPriorPtrPrVec tmp;
    tmp.reserve(other.m_Models.size());
    for (std::size_t i = 0u; i < other.m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = other.m_Models[i];
        tmp.push_back(TWeightPriorPtrPr(model.first, TPriorPtr(model.second->clone())));
    }
    m_Models.swap(tmp);

    this->CMultivariatePrior::addSamples(other.numberSamples());
}

CMultivariateOneOfNPrior &CMultivariateOneOfNPrior::operator=(const CMultivariateOneOfNPrior &rhs)
{
    if (this != &rhs)
    {
        CMultivariateOneOfNPrior tmp(rhs);
        this->swap(tmp);
    }
    return *this;
}

void CMultivariateOneOfNPrior::swap(CMultivariateOneOfNPrior &other)
{
    this->CMultivariatePrior::swap(other);
    m_Models.swap(other.m_Models);
}

CMultivariateOneOfNPrior *CMultivariateOneOfNPrior::clone(void) const
{
    return new CMultivariateOneOfNPrior(*this);
}

std::size_t CMultivariateOneOfNPrior::dimension(void) const
{
    return m_Dimension;
}

void CMultivariateOneOfNPrior::dataType(maths_t::EDataType value)
{
    this->CMultivariatePrior::dataType(value);
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        m_Models[i].second->dataType(value);
    }
}

void CMultivariateOneOfNPrior::decayRate(double value)
{
    this->CMultivariatePrior::decayRate(value);
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        m_Models[i].second->decayRate(this->decayRate());
    }
}

void CMultivariateOneOfNPrior::setToNonInformative(double offset, double decayRate)
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        m_Models[i].first.age(0.0);
        m_Models[i].second->setToNonInformative(offset, decayRate);
    }
    this->decayRate(decayRate);
    this->numberSamples(0.0);
}

void CMultivariateOneOfNPrior::adjustOffset(const TWeightStyleVec &weightStyles,
                                            const TDouble10Vec1Vec &samples,
                                            const TDouble10Vec4Vec1Vec &weights)
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        m_Models[i].second->adjustOffset(weightStyles, samples, weights);
    }
}

void CMultivariateOneOfNPrior::addSamples(const TWeightStyleVec &weightStyles,
                                          const TDouble10Vec1Vec &samples,
                                          const TDouble10Vec4Vec1Vec &weights)
{
    if (samples.empty())
    {
        return;
    }
    if (!this->check(samples, weights))
    {
        return;
    }

    this->adjustOffset(weightStyles, samples, weights);

    double penalty = ::log(this->numberSamples());
    this->CMultivariatePrior::addSamples(weightStyles, samples, weights);
    penalty = (penalty - ::log(this->numberSamples())) / 2.0;

    // See COneOfNPrior::addSamples for a discussion.

    CScopeNormalizeWeights<TPriorPtr> normalizer(m_Models);

    // We need to check *before* adding samples to the constituent models.
    bool isNonInformative = this->isNonInformative();

    // Compute the unnormalized posterior weights and update the component
    // priors. These weights are computed on the side since they are only
    // updated if all marginal likelihoods can be computed.
    TDouble3Vec logLikelihoods;
    TBool3Vec reinstateModel;
    TMinAccumulator minLogLikelihood;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        CMultivariatePrior &model = *m_Models[i].second;
        bool participates = model.participatesInModelSelection();

        // Update the weights with the marginal likelihoods.
        double logLikelihood = 0.0;
        maths_t::EFloatingPointErrorStatus status = participates ?
                model.jointLogMarginalLikelihood(weightStyles, samples, weights, logLikelihood) :
                maths_t::E_FpOverflowed;
        if (status & maths_t::E_FpFailed)
        {
            LOG_ERROR("Failed to compute log-likelihood");
            LOG_ERROR("samples = " << core::CContainerPrinter::print(samples));
        }
        else
        {
            if (!(status & maths_t::E_FpOverflowed))
            {
                logLikelihood += model.unmarginalizedParameters() * penalty;
                logLikelihoods.push_back(logLikelihood);
                minLogLikelihood.add(logLikelihood);
            }
            else
            {
                logLikelihoods.push_back(MINUS_INF);
            }

            // Update the component prior distribution.
            model.addSamples(weightStyles, samples, weights);
            reinstateModel.push_back(participates ^ model.participatesInModelSelection());
        }
    }

    TDouble10Vec n(m_Dimension, 0.0);
    try
    {
        for (std::size_t i = 0u; i < weights.size(); ++i)
        {
            add(maths_t::count(m_Dimension, weightStyles, weights[i]), n);
        }
    }
    catch (std::exception &e)
    {
        LOG_ERROR("Failed to add samples: " << e.what());
        return;
    }

    if (!isNonInformative && minLogLikelihood.count() > 0)
    {
        LOG_TRACE("logLikelihoods = " << core::CContainerPrinter::print(logLikelihoods));

        // The idea here is to limit the amount which extreme samples
        // affect model selection, particularly early on in the model
        // life-cycle.
        double minLogFactor = std::max(largest(n) * maxModelPenalty(this->numberSamples()),
                                       minLogLikelihood[0] - largest(n) * 100);

        TMaxAccumulator maxLogWeight;
        for (std::size_t i = 0; i < logLikelihoods.size(); ++i)
        {
            CModelWeight &weight = m_Models[i].first;
            weight.addLogFactor(std::max(logLikelihoods[i],
                                         m_Models[i].second->participatesInModelSelection() ?
                                             minLogFactor : MINUS_INF / 2.0 - weight.logWeight()));
            maxLogWeight.add(weight.logWeight());
        }
        for (std::size_t i = 0u; i < reinstateModel.size(); ++i)
        {
            if (reinstateModel[i])
            {
                m_Models[i].first.logWeight(maxLogWeight[0] + LOG_INITIAL_WEIGHT);
            }
        }
    }

    if (this->badWeights())
    {
        LOG_ERROR("Update failed (" << this->debugWeights() << ")");
        LOG_ERROR("samples = " << core::CContainerPrinter::print(samples));
        LOG_ERROR("weights = " << core::CContainerPrinter::print(weights));
        this->setToNonInformative(this->offsetMargin(), this->decayRate());
    }
}

void CMultivariateOneOfNPrior::propagateForwardsByTime(double time)
{
    if (!CMathsFuncs::isFinite(time) || time < 0.0)
    {
        LOG_ERROR("Bad propagation time " << time);
        return;
    }

    CScopeNormalizeWeights<TPriorPtr> normalizer(m_Models);

    double alpha = ::exp(-this->scaledDecayRate() * time);

    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        TWeightPriorPtrPr &model = m_Models[i];
        model.first.age(alpha);
        model.second->propagateForwardsByTime(time);
    }

    this->numberSamples(this->numberSamples() * alpha);

    LOG_TRACE("numberSamples = " << this->numberSamples());
}

CMultivariateOneOfNPrior::TUnivariatePriorPtrDoublePr
CMultivariateOneOfNPrior::univariate(const TSize10Vec &marginalize,
                                     const TSizeDoublePr10Vec &condition) const
{
    std::size_t n = m_Models.size();

    COneOfNPrior::TDoublePriorPtrPrVec models;
    TDouble3Vec weights;
    TMaxAccumulator maxWeight;
    models.reserve(n);
    weights.reserve(n);

    for (std::size_t i = 0u; i < n; ++i)
    {
        if (m_Models[i].second->participatesInModelSelection())
        {
            TUnivariatePriorPtrDoublePr prior(m_Models[i].second->univariate(marginalize, condition));
            if (prior.first == 0)
            {
                return TUnivariatePriorPtrDoublePr();
            }
            models.push_back(std::make_pair(1.0, prior.first));
            weights.push_back(prior.second + m_Models[i].first.logWeight());
            maxWeight.add(weights.back());
        }
    }

    for (std::size_t i = 0u; i < weights.size(); ++i)
    {
        models[i].first *= ::exp(weights[i] - maxWeight[0]);
    }

    return std::make_pair(TUnivariatePriorPtr(new COneOfNPrior(models, this->dataType(), this->decayRate())),
                          maxWeight.count() > 0 ? maxWeight[0] : 0.0);
}

CMultivariateOneOfNPrior::TPriorPtrDoublePr
CMultivariateOneOfNPrior::bivariate(const TSize10Vec &marginalize,
                                    const TSizeDoublePr10Vec &condition) const
{
    if (m_Dimension == 2)
    {
        return TPriorPtrDoublePr(TPriorPtr(this->clone()), 0.0);
    }

    std::size_t n = m_Models.size();

    TDoublePriorPtrPrVec models;
    TDouble3Vec weights;
    TMaxAccumulator maxWeight;
    models.reserve(n);
    weights.reserve(n);

    for (std::size_t i = 0u; i < n; ++i)
    {
        if (m_Models[i].second->participatesInModelSelection())
        {
            TPriorPtrDoublePr prior(m_Models[i].second->bivariate(marginalize, condition));
            if (prior.first == 0)
            {
                return TPriorPtrDoublePr();
            }
            models.push_back(std::make_pair(1.0, prior.first));
            weights.push_back(prior.second + m_Models[i].first.logWeight());
            maxWeight.add(weights.back());
        }
    }

    for (std::size_t i = 0u; i < weights.size(); ++i)
    {
        models[i].first *= ::exp(weights[i] - maxWeight[0]);
    }

    return std::make_pair(TPriorPtr(new CMultivariateOneOfNPrior(2, models, this->dataType(), this->decayRate())),
                          maxWeight.count() > 0 ? maxWeight[0] : 0.0);
}

TDouble10VecDouble10VecPr
CMultivariateOneOfNPrior::marginalLikelihoodSupport(void) const
{
    // We define this is as the intersection of the component model
    // supports.

    TDouble10VecDouble10VecPr result(TDouble10Vec(m_Dimension, MINUS_INF),
                                     TDouble10Vec(m_Dimension, INF));
    TDouble10VecDouble10VecPr modelSupport;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const CMultivariatePrior &model = *m_Models[i].second;
        if (model.participatesInModelSelection())
        {
            modelSupport  = model.marginalLikelihoodSupport();
            result.first  = max(result.first, modelSupport.first);
            result.second = min(result.second, modelSupport.second);
        }
    }

    return result;
}

TDouble10Vec CMultivariateOneOfNPrior::marginalLikelihoodMean(void) const
{
    // This is E_{P(i)}[ E[X | P(i)] ] and the conditional expectation
    // is just the individual model expectation. Note we exclude models
    // with low weight because typically the means are similar between
    // models and if they are very different we don't want to include
    // the model if there is strong evidence against it.

    TDouble10Vec result(m_Dimension, 0.0);
    double w = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        double wi = model.first;
        if (wi > MINIMUM_SIGNIFICANT_WEIGHT)
        {
            updateMean(model.second->marginalLikelihoodMean(), wi, result, w);
        }
    }
    return result;
}

TDouble10Vec CMultivariateOneOfNPrior::nearestMarginalLikelihoodMean(const TDouble10Vec &value) const
{
    // See marginalLikelihoodMean for discussion.

    TDouble10Vec result(m_Dimension, 0.0);
    double w = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        double wi = model.first;
        if (wi > MINIMUM_SIGNIFICANT_WEIGHT)
        {
            updateMean(model.second->nearestMarginalLikelihoodMean(value), wi, result, w);
        }
    }
    return result;
}

TDouble10Vec10Vec CMultivariateOneOfNPrior::marginalLikelihoodCovariance(void) const
{
    TDouble10Vec10Vec result(m_Dimension, TDouble10Vec(m_Dimension, 0.0));
    if (this->isNonInformative())
    {
        for (std::size_t i = 0u; i < m_Dimension; ++i)
        {
            result[i][i] = INF;
        }
        return result;
    }

    // This is E_{P(i)}[ Cov[X | i] ] and the conditional expectation
    // is just the individual model expectation. Note we exclude models
    // with low weight because typically the variance are similar between
    // models and if they are very different we don't want to include
    // the model if there is strong evidence against it.

    double w = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        double wi = model.first;
        if (wi > MINIMUM_SIGNIFICANT_WEIGHT)
        {
            updateMean(model.second->marginalLikelihoodCovariance(), wi, result, w);
        }
    }
    return result;
}

TDouble10Vec CMultivariateOneOfNPrior::marginalLikelihoodVariances(void) const
{
    if (this->isNonInformative())
    {
        return TDouble10Vec(m_Dimension, INF);
    }

    TDouble10Vec result(m_Dimension, 0.0);
    double w = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        double wi = model.first;
        if (wi > MINIMUM_SIGNIFICANT_WEIGHT)
        {
            updateMean(model.second->marginalLikelihoodVariances(), wi, result, w);
        }
    }
    return result;
}

TDouble10Vec CMultivariateOneOfNPrior::marginalLikelihoodMode(const TWeightStyleVec &weightStyles,
                                                              const TDouble10Vec4Vec &weights) const
{
    // We approximate this as the weighted average of the component
    // model modes.

    // Declared outside the loop to minimize the number of times
    // it is created.
    TDouble10Vec1Vec sample(1);
    TDouble10Vec4Vec1Vec sampleWeights(1, weights);

    TDouble10Vec result(m_Dimension, 0.0);
    double n = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const CMultivariatePrior &model = *m_Models[i].second;
        if (model.participatesInModelSelection())
        {
            sample[0] = model.marginalLikelihoodMode(weightStyles, weights);
            double logLikelihood;
            model.jointLogMarginalLikelihood(weightStyles, sample, sampleWeights, logLikelihood);
            updateMean(sample[0], m_Models[i].first * ::exp(logLikelihood), result, n);
        }
    }

    TDouble10VecDouble10VecPr support = this->marginalLikelihoodSupport();
    return CTools::truncate(result, support.first, support.second);
}

maths_t::EFloatingPointErrorStatus
CMultivariateOneOfNPrior::jointLogMarginalLikelihood(const TWeightStyleVec &weightStyles,
                                                     const TDouble10Vec1Vec &samples,
                                                     const TDouble10Vec4Vec1Vec &weights,
                                                     double &result) const
{
    result = 0.0;

    if (samples.empty())
    {
        LOG_ERROR("Can't compute likelihood for empty sample set");
        return maths_t::E_FpFailed;
    }
    if (!this->check(samples, weights))
    {
        return maths_t::E_FpFailed;
    }

    // See COneOfNPrior::jointLogMarginalLikelihood for a discussion.

    // We re-normalize the data so that the maximum likelihood is one
    // to avoid underflow.
    TDouble3Vec logLikelihoods;
    logLikelihoods.reserve(m_Models.size());
    TMaxAccumulator maxLogLikelihood;

    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const CMultivariatePrior &model = *m_Models[i].second;
        if (model.participatesInModelSelection())
        {
            double logLikelihood;
            maths_t::EFloatingPointErrorStatus status =
                    model.jointLogMarginalLikelihood(weightStyles, samples, weights, logLikelihood);
            if (status & maths_t::E_FpFailed)
            {
                return status;
            }
            if (!(status & maths_t::E_FpOverflowed))
            {
                logLikelihood += m_Models[i].first.logWeight();
                logLikelihoods.push_back(logLikelihood);
                maxLogLikelihood.add(logLikelihood);
            }
        }
    }

    if (maxLogLikelihood.count() == 0)
    {
        result = MINUS_INF;
        return maths_t::E_FpOverflowed;
    }

    for (std::size_t i = 0u; i < logLikelihoods.size(); ++i)
    {
        result += ::exp(logLikelihoods[i] - maxLogLikelihood[0]);
    }
    result = maxLogLikelihood[0] + ::log(result);

    maths_t::EFloatingPointErrorStatus status = CMathsFuncs::fpStatus(result);
    if (status & maths_t::E_FpFailed)
    {
        LOG_ERROR("Failed to compute log likelihood (" << this->debugWeights() << ")");
        LOG_ERROR("samples = " << core::CContainerPrinter::print(samples));
        LOG_ERROR("weights = " << core::CContainerPrinter::print(weights));
        LOG_ERROR("logLikelihoods = " << core::CContainerPrinter::print(logLikelihoods));
        LOG_ERROR("maxLogLikelihood = " << maxLogLikelihood[0]);
    }
    else if (status & maths_t::E_FpOverflowed)
    {
        LOG_ERROR("Log likelihood overflowed for (" << this->debugWeights() << ")");
        LOG_TRACE("likelihoods = " << core::CContainerPrinter::print(logLikelihoods));
        LOG_TRACE("samples = " << core::CContainerPrinter::print(samples));
        LOG_TRACE("weights = " << core::CContainerPrinter::print(weights));
    }
    return status;
}

void CMultivariateOneOfNPrior::sampleMarginalLikelihood(std::size_t numberSamples,
                                                        TDouble10Vec1Vec &samples) const
{
    samples.clear();

    if (numberSamples == 0 || this->isNonInformative())
    {
        return;
    }

    TDouble3Vec weights;
    weights.reserve(m_Models.size());
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        weights.push_back(m_Models[i].first);
    }

    CSampling::TSizeVec sampling;
    CSampling::weightedSample(numberSamples, weights, sampling);
    LOG_TRACE("weights = " << core::CContainerPrinter::print(weights)
              << ", sampling = " << core::CContainerPrinter::print(sampling));

    if (sampling.size() != m_Models.size())
    {
        LOG_ERROR("Failed to sample marginal likelihood");
        return;
    }

    TDouble10VecDouble10VecPr support = this->marginalLikelihoodSupport();
    for (std::size_t i = 0u; i < m_Dimension; ++i)
    {
        support.first[i]  = CTools::shiftRight(support.first[i]);
        support.second[i] = CTools::shiftLeft(support.second[i]);
    }

    samples.reserve(numberSamples);
    TDouble10Vec1Vec modelSamples;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        modelSamples.clear();
        m_Models[i].second->sampleMarginalLikelihood(sampling[i], modelSamples);
        LOG_TRACE("modelSamples = " << core::CContainerPrinter::print(modelSamples));

        for (std::size_t j = 0u; j < modelSamples.size(); ++j)
        {
            samples.push_back(CTools::truncate(modelSamples[j], support.first, support.second));
        }
    }
    LOG_TRACE("samples = "<< core::CContainerPrinter::print(samples));
}

bool CMultivariateOneOfNPrior::isNonInformative(void) const
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        if (m_Models[i].second->isNonInformative())
        {
            return true;
        }
    }
    return false;
}

void CMultivariateOneOfNPrior::print(const std::string &separator,
                                     std::string &result) const
{
    result += core_t::LINE_ENDING + separator + " one-of-n";
    if (this->isNonInformative())
    {
        result += " non-informative";
    }

    result += ':';
    result += core_t::LINE_ENDING + separator
             + " # samples " + core::CStringUtils::typeToStringPretty(this->numberSamples());

    std::string separator_ = separator + separator;

    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        double weight = m_Models[i].first;
        if (weight >= MINIMUM_SIGNIFICANT_WEIGHT)
        {
            result += core_t::LINE_ENDING + separator_ + " weight " + core::CStringUtils::typeToStringPretty(weight);
            m_Models[i].second->print(separator_, result);
        }
    }
}

uint64_t CMultivariateOneOfNPrior::checksum(uint64_t seed) const
{
    seed = this->CMultivariatePrior::checksum(seed);
    return CChecksum::calculate(seed, m_Models);
}

void CMultivariateOneOfNPrior::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CMultivariateOneOfNPrior");
    core::CMemoryDebug::dynamicSize("m_Models", m_Models, mem);
}

std::size_t CMultivariateOneOfNPrior::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_Models);
    return mem;
}

std::size_t CMultivariateOneOfNPrior::staticSize(void) const
{
    return sizeof(*this);
}

std::string CMultivariateOneOfNPrior::persistenceTag(void) const
{
    return ONE_OF_N_TAG + core::CStringUtils::typeToString(m_Dimension);
}

void CMultivariateOneOfNPrior::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const CMultivariatePrior *prior(m_Models[i].second.get());
        if (prior == 0)
        {
            LOG_ERROR("Unexpected NULL pointer");
            continue;
        }
        inserter.insertLevel(MODEL_TAG,
                             boost::bind(&modelAcceptPersistInserter,
                                         boost::cref(m_Models[i].first),
                                         boost::cref(*prior), _1));
    }
    inserter.insertValue(DECAY_RATE_TAG, this->decayRate(), core::CIEEE754::E_SinglePrecision);
    inserter.insertValue(NUMBER_SAMPLES_TAG,
                         this->numberSamples(),
                         core::CIEEE754::E_SinglePrecision);
}

CMultivariateOneOfNPrior::TDouble3Vec CMultivariateOneOfNPrior::weights(void) const
{
    TDouble3Vec result;
    result.reserve(m_Models.size());
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        result.push_back(m_Models[i].first);
    }
    return result;
}

CMultivariateOneOfNPrior::TDouble3Vec CMultivariateOneOfNPrior::logWeights(void) const
{
    TDouble3Vec result;
    result.reserve(m_Models.size());
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        result.push_back(m_Models[i].first.logWeight());
    }
    return result;
}

CMultivariateOneOfNPrior::TPriorCPtr3Vec CMultivariateOneOfNPrior::models(void) const
{
    TPriorCPtr3Vec result;
    result.reserve(m_Models.size());
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        result.push_back(m_Models[i].second.get());
    }
    return result;
}

bool CMultivariateOneOfNPrior::badWeights(void) const
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        if (!CMathsFuncs::isFinite(m_Models[i].first.logWeight()))
        {
            return true;
        }
    }
    return false;
}

std::string CMultivariateOneOfNPrior::debugWeights(void) const
{
    if (m_Models.empty())
    {
        return std::string();
    }
    std::ostringstream result;
    result << std::scientific << std::setprecision(15)
           << m_Models[0].first.logWeight();
    for (std::size_t i = 1u; i < m_Models.size(); ++i)
    {
        result << " " << m_Models[i].first.logWeight();
    }
    return result.str();
}

const double CMultivariateOneOfNPrior::MAXIMUM_RELATIVE_ERROR = 1e-3;
const double CMultivariateOneOfNPrior::LOG_MAXIMUM_RELATIVE_ERROR = ::log(MAXIMUM_RELATIVE_ERROR);

}
}
