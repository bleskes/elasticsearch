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

#include <maths/COneOfNPrior.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStringUtils.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/CDistributionRestoreParams.h>
#include <maths/CMathsFuncs.h>
#include <maths/CPriorStateSerialiser.h>
#include <maths/CSampling.h>
#include <maths/CTools.h>

#include <boost/bind.hpp>
#include <boost/numeric/conversion/bounds.hpp>
#include <boost/ref.hpp>

#include <algorithm>
#include <iterator>
#include <limits>
#include <utility>

#include <math.h>

namespace ml
{
namespace maths
{

namespace
{

typedef core::CSmallVectorBool<5> TBool5Vec;
typedef core::CSmallVector<double, 5> TDouble5Vec;
typedef std::pair<double, std::size_t> TDoubleSizePr;
typedef core::CSmallVector<TDoubleSizePr, 5> TDoubleSizePr5Vec;
typedef CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

//! Compute the log of \p n.
double logn(std::size_t n)
{
    static const double LOG_N[] = { 0.0, ::log(2.0), ::log(3.0), ::log(4.0), ::log(5.0) };
    return n < boost::size(LOG_N) ? LOG_N[n - 1] : ::log(static_cast<double>(n));
}

const double MINUS_INF = boost::numeric::bounds<double>::lowest();
const double INF = boost::numeric::bounds<double>::highest();
const double LOG_INITIAL_WEIGHT = ::log(1e-6);
const double MINIMUM_SIGNIFICANT_WEIGHT = 0.01;
const double MAXIMUM_RELATIVE_ERROR = 1e-3;
const double LOG_MAXIMUM_RELATIVE_ERROR = ::log(MAXIMUM_RELATIVE_ERROR);

// We obfuscate the XML element names to avoid giving away too much
// information about our model.
const std::string MODEL_TAG("a");
const std::string NUMBER_SAMPLES_TAG("b");
const std::string MINIMUM_TAG("c");
const std::string MAXIMUM_TAG("d");
const std::string DECAY_RATE_TAG("e");

// Nested tags
const std::string WEIGHT_TAG("a");
const std::string PRIOR_TAG("b");

const std::string EMPTY_STRING;

//! Persist state for a models by passing information to \p inserter.
void modelAcceptPersistInserter(const CModelWeight &weight,
                                const CPrior &prior,
                                core::CStatePersistInserter &inserter)
{
    inserter.insertLevel(WEIGHT_TAG, boost::bind(&CModelWeight::acceptPersistInserter, &weight, _1));
    inserter.insertLevel(PRIOR_TAG, boost::bind<void>(CPriorStateSerialiser(), boost::cref(prior), _1));
}

}

//////// COneOfNPrior Implementation ////////

COneOfNPrior::COneOfNPrior(const TPriorPtrVec &models,
                           maths_t::EDataType dataType,
                           double decayRate) :
        CPrior(dataType, decayRate)
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

COneOfNPrior::COneOfNPrior(const TDoublePriorPtrPrVec &models,
                           maths_t::EDataType dataType,
                           double decayRate/*= 0.0*/) :
       CPrior(dataType, decayRate)
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

COneOfNPrior::COneOfNPrior(const SDistributionRestoreParams &params,
                           core::CStateRestoreTraverser &traverser) :
        CPrior(params.s_DataType, params.s_DecayRate)
{
    traverser.traverseSubLevel(boost::bind(&COneOfNPrior::acceptRestoreTraverser,
                                           this, boost::cref(params), _1));
}

bool COneOfNPrior::acceptRestoreTraverser(const SDistributionRestoreParams &params,
                                          core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_SETUP_TEARDOWN(DECAY_RATE_TAG,
                               double decayRate,
                               core::CStringUtils::stringToType(traverser.value(), decayRate),
                               this->decayRate(decayRate))
        RESTORE(MODEL_TAG, traverser.traverseSubLevel(boost::bind(&COneOfNPrior::modelAcceptRestoreTraverser,
                                                                  this, boost::cref(params), _1)))
        RESTORE_SETUP_TEARDOWN(NUMBER_SAMPLES_TAG,
                               double numberSamples,
                               core::CStringUtils::stringToType(traverser.value(), numberSamples),
                               this->numberSamples(numberSamples))
        RESTORE(MINIMUM_TAG, this->minimum().restore(traverser))
        RESTORE(MAXIMUM_TAG, this->maximum().restore(traverser))
    }
    while (traverser.next());

    if (CScopeDisableNormalizeOnRestore::normalizeOnRestore())
    {
        CScopeNormalizeWeights<TPriorPtr> normalizer(m_Models);
    }

    return true;
}

COneOfNPrior::COneOfNPrior(const COneOfNPrior &other) :
        CPrior(other.dataType(), other.decayRate())
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

    this->CPrior::addSamples(other.numberSamples());
    this->minimum() = other.minimum();
    this->maximum() = other.maximum();
}

COneOfNPrior &COneOfNPrior::operator=(const COneOfNPrior &rhs)
{
    if (this != &rhs)
    {
        COneOfNPrior tmp(rhs);
        this->swap(tmp);
    }
    return *this;
}

void COneOfNPrior::swap(COneOfNPrior &other)
{
    this->CPrior::swap(other);
    m_Models.swap(other.m_Models);
}

COneOfNPrior::EPrior COneOfNPrior::type(void) const
{
    return E_OneOfN;
}

COneOfNPrior *COneOfNPrior::clone(void) const
{
    return new COneOfNPrior(*this);
}

void COneOfNPrior::dataType(maths_t::EDataType value)
{
    this->CPrior::dataType(value);
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        m_Models[i].second->dataType(value);
    }
}

void COneOfNPrior::decayRate(double value)
{
    this->CPrior::decayRate(value);
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        m_Models[i].second->decayRate(this->decayRate());
    }
}

void COneOfNPrior::setToNonInformative(double offset, double decayRate)
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        m_Models[i].first.age(0.0);
        m_Models[i].second->setToNonInformative(offset, decayRate);
    }
    this->decayRate(decayRate);
    this->numberSamples(0.0);
}

void COneOfNPrior::removeModels(CModelFilter &filter)
{
    CScopeNormalizeWeights<TPriorPtr> normalizer(m_Models);

    std::size_t last = 0u;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        if (last != i)
        {
            std::swap(m_Models[last], m_Models[i]);
        }
        if (!filter(m_Models[last].second->type()))
        {
            ++last;
        }
    }
    m_Models.erase(m_Models.begin() + last, m_Models.end());
}

bool COneOfNPrior::needsOffset(void) const
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        if (m_Models[i].second->needsOffset())
        {
            return true;
        }
    }
    return false;
}

void COneOfNPrior::adjustOffset(const TWeightStyleVec &weightStyles,
                                const TDouble1Vec &samples,
                                const TDouble4Vec1Vec &weights)
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        m_Models[i].second->adjustOffset(weightStyles, samples, weights);
    }
}

double COneOfNPrior::offset(void) const
{
    double offset = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        offset = std::max(offset, m_Models[i].second->offset());
    }
    return offset;
}

void COneOfNPrior::addSamples(const TWeightStyleVec &weightStyles,
                              const TDouble1Vec &samples,
                              const TDouble4Vec1Vec &weights)
{
    if (samples.empty())
    {
        return;
    }

    if (samples.size() != weights.size())
    {
        LOG_ERROR("Mismatch in samples '"
                  << core::CContainerPrinter::print(samples)
                  << "' and weights '"
                  << core::CContainerPrinter::print(weights) << "'");
        return;
    }

    this->adjustOffset(weightStyles, samples, weights);

    double penalty = ::log(this->numberSamples());
    this->CPrior::addSamples(weightStyles, samples, weights);
    penalty = (penalty - ::log(this->numberSamples())) / 2.0;

    // For this 1-of-n model we assume that all the data come from one
    // the distributions which comprise the model. The model can be
    // treated exactly like a discrete parameter and assigned a posterior
    // probability in a Bayesian sense. In particular, we have:
    //   f({p(m), m} | x) = L(x | p(m), m) * f(p(m)) * P(m) / N      (1)
    //
    // where,
    //   x = {x(1), x(2), ... , x(n)} is the sample vector,
    //   f({p(m), m} | x) is the posterior distribution for p(m) and m,
    //   p(m) are the parameters of the model,
    //   m is the model,
    //   L(x | p(m), m) is the likelihood of the data given the model m'th,
    //   f(p(m)) is the prior for the m'th model parameters,
    //   P(m) is the prior probability the data are from the m'th model,
    //   N is a normalization factor.
    //
    // There is one such relation for each model and N is computed over
    // all models:
    //   N = Sum_m( Integral_dp(m)( f({p(m), m}) ) )
    //
    // Note that we can write the r.h.s. of (1) as:
    //   ((L(x | p(m), m) / N'(m)) * f(p(m))) * (N'(m) / N * P(m))   (2)
    //
    // where,
    //   N'(m) = Integral_dp(m)( L(x | {p(m), m}) ),
    //   N = Sum_m( N'(m | x) ) by definition.
    //
    // This means that the joint posterior distribution factorises into the
    // posterior distribution for the model parameters given the data and
    // the posterior weights for each model, i.e.
    //   f({p(m), m} | x) = f'(p(m) | x) * P'(m | x)
    //
    // where f' and P' come from (2). Finally, note that N'(m) is really
    // a function of the data, say h_m(x), and satisfies the relation:
    //   h_m({x(1), x(2), ... , x(k+1)})
    //     = L(x(k+1) | {p(m), m, x(1), x(2), ... , x(k)})
    //       * h_m({x(1), x(2), ... , x(k)})
    //
    // Here, L(x(k+1) | {p(m), m, x(1), x(2), ... , x(k)}) is the likelihood
    // of the (k+1)'th datum given model m and all previous data. Really, the
    // data just enter into this via the updated model parameters p(m). This
    // is the form we use below.
    //
    // Note that the weight of the sample x(i) is interpreted as its count,
    // i.e. n(i), so for example updating with {(x, 2)} is equivalent to
    // updating with {x, x}.

    typedef CBasicStatistics::COrderStatisticsStack<double, 1> TMinAccumulator;

    CScopeNormalizeWeights<TPriorPtr> normalizer(m_Models);

    // We need to check *before* adding samples to the constituent models.
    bool isNonInformative = this->isNonInformative();

    // Compute the unnormalized posterior weights and update the component
    // priors. These weights are computed on the side since they are only
    // updated if all marginal likelihoods can be computed.
    TDouble5Vec logLikelihoods;
    TBool5Vec reinstateModel;
    TMinAccumulator minLogLikelihood;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        CPrior &model = *m_Models[i].second;
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

    double n = 0.0;
    try
    {
        for (std::size_t i = 0u; i < weights.size(); ++i)
        {
            n += maths_t::count(weightStyles, weights[i]);
        }
    }
    catch (const std::exception &e)
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
        double minLogFactor = std::max(n * maxModelPenalty(this->numberSamples()),
                                       minLogLikelihood[0] - n * 100);

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

void COneOfNPrior::propagateForwardsByTime(double time)
{
    if (!CMathsFuncs::isFinite(time) || time < 0.0)
    {
        LOG_ERROR("Bad propagation time " << time);
        return;
    }

    CScopeNormalizeWeights<TPriorPtr> normalizer(m_Models);

    double alpha = ::exp(-this->decayRate() * time);

    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        TWeightPriorPtrPr &model = m_Models[i];
        model.first.age(alpha);
        model.second->propagateForwardsByTime(time);
    }

    this->numberSamples(this->numberSamples() * alpha);

    LOG_TRACE("numberSamples = " << this->numberSamples());
}

COneOfNPrior::TDoubleDoublePr COneOfNPrior::marginalLikelihoodSupport(void) const
{
    TDoubleDoublePr result(MINUS_INF, INF);

    // We define this is as the intersection of the component model supports.
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const CPrior &model = *m_Models[i].second;
        if (model.participatesInModelSelection())
        {
            TDoubleDoublePr modelSupport = model.marginalLikelihoodSupport();
            result.first  = std::max(result.first, modelSupport.first);
            result.second = std::min(result.second, modelSupport.second);
        }
    }

    return result;
}

double COneOfNPrior::marginalLikelihoodMean(void) const
{
    if (this->isNonInformative())
    {
        TDoubleVec means;
        means.reserve(m_Models.size());
        for (std::size_t i = 0u; i < m_Models.size(); ++i)
        {
            const CPrior &model = *m_Models[i].second;
            if (model.participatesInModelSelection())
            {
                means.push_back(model.marginalLikelihoodMean());
            }
        }
        return CBasicStatistics::median(means);
    }

    // This is E_{P(i)}[ E[X | P(i)] ] and the conditional expectation
    // is just the individual model expectation. Note we exclude models
    // with low weight because typically the means are similar between
    // models and if they are very different we don't want to include
    // the model if there is strong evidence against it.

    double result = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        double wi = model.first;
        if (wi > MINIMUM_SIGNIFICANT_WEIGHT)
        {
            result += wi * model.second->marginalLikelihoodMean();
        }
    }
    return result;
}

double COneOfNPrior::nearestMarginalLikelihoodMean(double value) const
{
    // See marginalLikelihoodMean for discussion.

    double result = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        double wi = model.first;
        if (wi > MINIMUM_SIGNIFICANT_WEIGHT)
        {
            result += wi * model.second->nearestMarginalLikelihoodMean(value);
        }
    }
    return result;
}

double COneOfNPrior::marginalLikelihoodMode(const TWeightStyleVec &weightStyles,
                                            const TDouble4Vec &weights) const
{
    // We approximate this as the weighted average of the component
    // model modes.

    // Declared outside the loop to minimize the number of times
    // they are created.
    TDouble1Vec sample(1);
    TDouble4Vec1Vec weight(1, weights);

    TMeanAccumulator mode;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        if (model.second->participatesInModelSelection())
        {
            double wi = model.first;
            double modelMode = model.second->marginalLikelihoodMode(weightStyles, weights);
            double logLikelihood;
            sample[0] = modelMode;
            model.second->jointLogMarginalLikelihood(weightStyles, sample, weight, logLikelihood);
            double w = wi * ::exp(logLikelihood);
            LOG_TRACE("modelMode = " << modelMode << ", weight = " << w);
            mode.add(modelMode, w);
        }
    }

    double result = CBasicStatistics::mean(mode);
    TDoubleDoublePr support = this->marginalLikelihoodSupport();
    return CTools::truncate(result, support.first, support.second);
}

double COneOfNPrior::marginalLikelihoodVariance(const TWeightStyleVec &weightStyles,
                                                const TDouble4Vec &weights) const
{
    if (this->isNonInformative())
    {
        return INF;
    }

    // This is E_{P(i)}[ Var[X | i] ] and the conditional expectation
    // is just the individual model expectation. Note we exclude models
    // with low weight because typically the means are similar between
    // models and if they are very different we don't want to include
    // the model if there is strong evidence against it.

    double result = 0.0;
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        double wi = model.first;
        if (wi > MINIMUM_SIGNIFICANT_WEIGHT)
        {
            result += wi * model.second->marginalLikelihoodVariance(weightStyles, weights);
        }
    }
    return result;
}

COneOfNPrior::TDoubleDoublePr
COneOfNPrior::marginalLikelihoodConfidenceInterval(double percentage,
                                                   const TWeightStyleVec &weightStyles,
                                                   const TDouble4Vec &weights) const
{
    // We approximate this as the weighted sum of the component model
    // intervals. To compute the weights we expand all component model
    // marginal likelihoods about a reasonable estimate for the true
    // interval end points, i.e.
    //   [a_0, b_0] = [Sum_m( a(m) * w(m) ), Sum_m( b(m) * w(m) )]
    //
    // Here, m ranges over the component models, w(m) are the model
    // weights and P([a(m), b(m)]) = p where p is the desired percentage
    // expressed as a probability. Note that this will be accurate in
    // the limit that one model dominates.
    //
    // Note P([a, b]) = F(b) - F(a) where F(.) is the c.d.f. of the
    // marginal likelihood f(.) so it possible to compute a first order
    // correction to [a_0, b_0] as follows:
    //   a_1 = a_0 + ((1 - p) / 2 - F(a_0)) / f(a_0)
    //   b_1 = b_0 + ((1 - p) / 2 - F(b_0)) / f(b_0)             (1)
    //
    // For the time being we just compute a_0 and b_0. We can revisit
    // this calculation if the accuracy proves to be a problem.

    TMeanAccumulator x1;
    TMeanAccumulator x2;

    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const TWeightPriorPtrPr &model = m_Models[i];
        double weight = model.first;
        if (weight >= MAXIMUM_RELATIVE_ERROR)
        {
            TDoubleDoublePr modelInterval =
                    model.second->marginalLikelihoodConfidenceInterval(percentage,
                                                                       weightStyles,
                                                                       weights);
            x1.add(modelInterval.first,  weight);
            x2.add(modelInterval.second, weight);
            LOG_TRACE("interval = " << core::CContainerPrinter::print(modelInterval));
        }
    }
    LOG_TRACE("x1 = " << x1 << ", x2 = " << x2);

    return std::make_pair(CBasicStatistics::mean(x1), CBasicStatistics::mean(x2));
}

maths_t::EFloatingPointErrorStatus
COneOfNPrior::jointLogMarginalLikelihood(const TWeightStyleVec &weightStyles,
                                         const TDouble1Vec &samples,
                                         const TDouble4Vec1Vec &weights,
                                         double &result) const
{
    result = 0.0;

    if (samples.empty())
    {
        LOG_ERROR("Can't compute likelihood for empty sample set");
        return maths_t::E_FpFailed;
    }

    if (samples.size() != weights.size())
    {
        LOG_ERROR("Mismatch in samples '"
                  << core::CContainerPrinter::print(samples)
                  << "' and weights '"
                  << core::CContainerPrinter::print(weights) << "'");
        return maths_t::E_FpFailed;
    }

    // We get that:
    //   marginal_likelihood(x) = Sum_m( L(x | m) * P(m) ).
    //
    // where,
    //   x = {x(1), x(2), ... , x(n)} the sample vector,
    //   L(x | m) = Integral_du(m)( L(x | {m, p(m)}) ),
    //   p(m) are the parameters of the component and
    //   P(m) is the prior probability the data are from the m'th model.

    // We re-normalize the data so that the maximum likelihood is one
    // to avoid underflow.
    TDouble5Vec logLikelihoods;
    logLikelihoods.reserve(m_Models.size());
    TMaxAccumulator maxLogLikelihood;

    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const CPrior &model = *m_Models[i].second;
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
        // Technically, the marginal likelihood is zero here so the
        // log would be infinite. We use minus max double because
        // log(0) = HUGE_VALUE, which causes problems for Windows.
        // Calling code is notified when the calculation overflows
        // and should avoid taking the exponential since this will
        // underflow and pollute the floating point environment.
        // This may cause issues for some library function
        // implementations (see fe*exceptflag for more details).
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

void COneOfNPrior::sampleMarginalLikelihood(std::size_t numberSamples,
                                            TDouble1Vec &samples) const
{
    samples.clear();

    if (numberSamples == 0 || this->isNonInformative())
    {
        return;
    }

    TDouble5Vec weights;
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

    TDoubleDoublePr support = this->marginalLikelihoodSupport();
    support.first  = CTools::shiftRight(support.first);
    support.second = CTools::shiftLeft(support.second);

    samples.reserve(numberSamples);
    TDouble1Vec modelSamples;
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

bool COneOfNPrior::minusLogJointCdfImpl(bool complement,
                                        const TWeightStyleVec &weightStyles,
                                        const TDouble1Vec &samples,
                                        const TDouble4Vec1Vec &weights,
                                        double &lowerBound,
                                        double &upperBound) const
{
    lowerBound = upperBound = 0.0;

    if (samples.empty())
    {
        LOG_ERROR("Can't compute c.d.f. "
                  << (complement ? "complement " : "") << "for empty sample set");
        return false;
    }

    if (this->isNonInformative())
    {
        lowerBound = upperBound = -::log(complement ? 1.0 - CTools::IMPROPER_CDF :
                                                            CTools::IMPROPER_CDF);
        return true;
    }

    // We get that:
    //   cdf(x) = Integral_dx( Sum_m( L(x | m) * P(m) )
    //
    // where,
    //   x = {x(1), x(2), ... , x(n)} the sample vector,
    //   L(x | m) = Integral_du(m)( L(x | {m, p(m)}) ),
    //   p(m) are the parameters of the component,
    //   P(m) is the prior probability the data are from the m'th model and
    //   Integral_dx is over [-inf, x(1)] o [-inf, x(2)] o ... o [-inf, x(n)]
    //   and o denotes the outer product.

    TDoubleSizePr5Vec logWeights;
    logWeights.reserve(m_Models.size());
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        if (m_Models[i].second->participatesInModelSelection())
        {
            double logWeight = m_Models[i].first.logWeight();
            logWeights.push_back(std::make_pair(logWeight, i));
        }
    }
    std::sort(logWeights.begin(), logWeights.end(), std::greater<TDoubleSizePr>());
    LOG_TRACE("logWeights = " << core::CContainerPrinter::print(logWeights));

    TDouble5Vec logLowerBounds;
    TDouble5Vec logUpperBounds;
    TMaxAccumulator maxLogLowerBound;
    TMaxAccumulator maxLogUpperBound;
    double logMaximumRemainder = MINUS_INF;
    for (std::size_t i = 0u, n = logWeights.size(); i < n; ++i)
    {
        double wi = logWeights[i].first;
        const CPrior &model = *m_Models[logWeights[i].second].second;

        double li = 0.0;
        double ui = 0.0;
        if (complement && !model.minusLogJointCdfComplement(weightStyles, samples, weights, li, ui))
        {
            LOG_ERROR("Failed computing c.d.f. complement for " << core::CContainerPrinter::print(samples));
            return false;
        }
        else if (!complement && !model.minusLogJointCdf(weightStyles, samples, weights, li, ui))
        {
            LOG_ERROR("Failed computing c.d.f. for " << core::CContainerPrinter::print(samples));
            return false;
        }
        li = wi - li;
        ui = wi - ui;

        logLowerBounds.push_back(li);
        logUpperBounds.push_back(ui);
        maxLogLowerBound.add(li);
        maxLogUpperBound.add(ui);

        // Check if we can exit early with reasonable precision.
        if (i+1 < n)
        {
            logMaximumRemainder = logn(n-i-1) + logWeights[i+1].first;
            if (   logMaximumRemainder < maxLogLowerBound[0] + LOG_MAXIMUM_RELATIVE_ERROR
                && logMaximumRemainder < maxLogUpperBound[0] + LOG_MAXIMUM_RELATIVE_ERROR)
            {
                break;
            }
        }
    }
    if (!CTools::logWillUnderflow<double>(maxLogLowerBound[0]))
    {
        maxLogLowerBound[0] = 0.0;
    }
    if (!CTools::logWillUnderflow<double>(maxLogUpperBound[0]))
    {
        maxLogUpperBound[0] = 0.0;
    }
    for (std::size_t i = 0u; i < logLowerBounds.size(); ++i)
    {
        lowerBound += ::exp(logLowerBounds[i] - maxLogLowerBound[0]);
        upperBound += ::exp(logUpperBounds[i] - maxLogUpperBound[0]);
    }
    lowerBound = -::log(lowerBound) - maxLogLowerBound[0];
    upperBound = -::log(upperBound) - maxLogUpperBound[0];
    if (logLowerBounds.size() < logWeights.size())
    {
        upperBound += -::log(1.0 + ::exp(logMaximumRemainder + upperBound));
    }
    lowerBound = std::max(lowerBound, 0.0);
    upperBound = std::max(upperBound, 0.0);

    LOG_TRACE("Joint -log(c.d.f." << (complement ? " complement" : "") << ") = ["
              << lowerBound << "," << upperBound << "]");

    return true;
}

bool COneOfNPrior::minusLogJointCdf(const TWeightStyleVec &weightStyles,
                                    const TDouble1Vec &samples,
                                    const TDouble4Vec1Vec &weights,
                                    double &lowerBound,
                                    double &upperBound) const
{
    return this->minusLogJointCdfImpl(false, // complement
                                      weightStyles, samples, weights,
                                      lowerBound, upperBound);
}

bool COneOfNPrior::minusLogJointCdfComplement(const TWeightStyleVec &weightStyles,
                                              const TDouble1Vec &samples,
                                              const TDouble4Vec1Vec &weights,
                                              double &lowerBound,
                                              double &upperBound) const
{
    return this->minusLogJointCdfImpl(true, // complement
                                      weightStyles, samples, weights,
                                      lowerBound, upperBound);
}

bool COneOfNPrior::probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
                                                  const TWeightStyleVec &weightStyles,
                                                  const TDouble1Vec &samples,
                                                  const TDouble4Vec1Vec &weights,
                                                  double &lowerBound,
                                                  double &upperBound,
                                                  maths_t::ETail &tail) const
{
    lowerBound = upperBound = 0.0;
    tail = maths_t::E_UndeterminedTail;

    if (samples.empty())
    {
        LOG_ERROR("Can't compute distribution for empty sample set");
        return false;
    }

    if (this->isNonInformative())
    {
        lowerBound = upperBound = 1.0;
        return true;
    }

    // The joint probability of less likely collection of samples can be
    // computed from the conditional probabilities of a less likely collection
    // of samples from each component model:
    //   P(R) = Sum_i( P(R | m) * P(m) )
    //
    // where,
    //   P(R | m) is the probability of a less likely collection of samples
    //   from the m'th model and
    //   P(m) is the prior probability the data are from the m'th model.

    typedef std::pair<double, maths_t::ETail> TDoubleTailPr;
    typedef CBasicStatistics::COrderStatisticsStack<TDoubleTailPr, 1, COrderings::SFirstGreater> TMaxAccumulator;

    TDoubleSizePr5Vec modelWeights;
    modelWeights.reserve(m_Models.size());
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        if (m_Models[i].second->participatesInModelSelection())
        {
            modelWeights.push_back(std::make_pair(m_Models[i].first, i));
        }
    }
    std::sort(modelWeights.begin(), modelWeights.end(), std::greater<TDoubleSizePr>());

    TMaxAccumulator tail_;
    for (std::size_t i = 0u; i < modelWeights.size(); ++i)
    {
        double weight = modelWeights[i].first;
        const CPrior &model = *m_Models[modelWeights[i].second].second;

        if (lowerBound > static_cast<double>(m_Models.size() - i) * weight
                         / MAXIMUM_RELATIVE_ERROR)
        {
            // The probability calculation is relatively expensive so don't
            // evaluate the probabilities that aren't needed to get good
            // accuracy.
            break;
        }

        double modelLowerBound, modelUpperBound;
        maths_t::ETail modelTail;
        if (!model.probabilityOfLessLikelySamples(calculation,
                                                  weightStyles, samples, weights,
                                                  modelLowerBound, modelUpperBound, modelTail))
        {
            // Logging handled at a lower level.
            return false;
        }

        LOG_TRACE("weight = " << weight
                  << ", modelLowerBound = " << modelLowerBound
                  << ", modelUpperBound = " << modelLowerBound);

        lowerBound += weight * modelLowerBound;
        upperBound += weight * modelUpperBound;
        tail_.add(TDoubleTailPr(weight * (modelLowerBound + modelUpperBound), modelTail));
    }

    if (   !(lowerBound >= 0.0 && lowerBound <= 1.001)
        || !(upperBound >= 0.0 && upperBound <= 1.001))
    {
        LOG_ERROR("Bad probability bounds = ["
                  << lowerBound << ", " << upperBound << "]"
                  << ", " << core::CContainerPrinter::print(modelWeights));
    }

    if (CMathsFuncs::isNan(lowerBound))
    {
        lowerBound = 0.0;
    }
    if (CMathsFuncs::isNan(upperBound))
    {
        upperBound = 1.0;
    }
    lowerBound = CTools::truncate(lowerBound, 0.0, 1.0);
    upperBound = CTools::truncate(upperBound, 0.0, 1.0);
    tail = tail_[0].second;

    LOG_TRACE("Probability = [" << lowerBound << "," << upperBound << "]");

    return true;
}

bool COneOfNPrior::isNonInformative(void) const
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const CPrior &model = *m_Models[i].second;
        if (model.participatesInModelSelection() && model.isNonInformative())
        {
            return true;
        }
    }
    return false;
}

void COneOfNPrior::print(const std::string &indent, std::string &result) const
{
    result += core_t::LINE_ENDING + indent + "one-of-n";
    if (this->isNonInformative())
    {
        result += " non-informative";
    }

    static const double MINIMUM_SIGNIFICANT_WEIGHT = 0.05;

    result += ':';
    result += core_t::LINE_ENDING + indent
                    + " # samples "
                    + core::CStringUtils::typeToStringPretty(this->numberSamples());
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        double weight = m_Models[i].first;
        if (weight >= MINIMUM_SIGNIFICANT_WEIGHT)
        {
            std::string indent_ =  indent
                                 + " weight "
                                 + core::CStringUtils::typeToStringPretty(weight) + "  ";
            m_Models[i].second->print(indent_, result);
        }
    }
}

std::string COneOfNPrior::printJointDensityFunction(void) const
{
    return "Not supported";
}

uint64_t COneOfNPrior::checksum(uint64_t seed) const
{
    seed = this->CPrior::checksum(seed);
    return CChecksum::calculate(seed, m_Models);
}

void COneOfNPrior::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("COneOfNPrior");
    core::CMemoryDebug::dynamicSize("m_Models", m_Models, mem);
}

std::size_t COneOfNPrior::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_Models);
    return mem;
}

std::size_t COneOfNPrior::staticSize(void) const
{
    return sizeof(*this);
}

void COneOfNPrior::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        const CPrior *priorPtr(m_Models[i].second.get());
        if (priorPtr == 0)
        {
            LOG_ERROR("Unexpected NULL pointer");
            continue;
        }

        inserter.insertLevel(MODEL_TAG, boost::bind(&modelAcceptPersistInserter,
                                                    boost::cref(m_Models[i].first),
                                                    boost::cref(*priorPtr), _1));
    }
    inserter.insertValue(DECAY_RATE_TAG, this->decayRate(), core::CIEEE754::E_SinglePrecision);
    inserter.insertValue(NUMBER_SAMPLES_TAG, this->numberSamples(), core::CIEEE754::E_SinglePrecision);
    this->minimum().persist(MINIMUM_TAG, inserter);
    this->maximum().persist(MAXIMUM_TAG, inserter);
}

COneOfNPrior::TDoubleVec COneOfNPrior::weights(void) const
{
    TDoubleVec result;
    result.reserve(m_Models.size());

    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        result.push_back(m_Models[i].first);
    }

    return result;
}

COneOfNPrior::TDoubleVec COneOfNPrior::logWeights(void) const
{
    TDoubleVec result;
    result.reserve(m_Models.size());

    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        result.push_back(m_Models[i].first.logWeight());
    }

    return result;
}

COneOfNPrior::TPriorCPtrVec COneOfNPrior::models(void) const
{
    TPriorCPtrVec result;
    result.reserve(m_Models.size());
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        result.push_back(m_Models[i].second.get());
    }
    return result;
}

bool COneOfNPrior::modelAcceptRestoreTraverser(const SDistributionRestoreParams &params,
                                               core::CStateRestoreTraverser &traverser)
{
    CModelWeight weight(1.0);
    bool gotWeight = false;
    TPriorPtr model;

    do
    {
        const std::string &name = traverser.name();
        RESTORE_SETUP_TEARDOWN(WEIGHT_TAG,
                               /*no-op*/,
                               traverser.traverseSubLevel(boost::bind(&CModelWeight::acceptRestoreTraverser,
                                                                      &weight, _1)),
                               gotWeight = true)
        RESTORE(PRIOR_TAG, traverser.traverseSubLevel(boost::bind<bool>(CPriorStateSerialiser(),
                                                                        boost::cref(params), boost::ref(model), _1)))
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

    m_Models.push_back(TWeightPriorPtrPr(weight, model));

    return true;
}

bool COneOfNPrior::badWeights(void) const
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

std::string COneOfNPrior::debugWeights(void) const
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

}
}
