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

#include <maths/CMultimodalPrior.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/Constants.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/CClusterer.h>
#include <maths/CClustererStateSerialiser.h>
#include <maths/CDistributionRestoreParams.h>
#include <maths/CIntegration.h>
#include <maths/CKMeansOnline1d.h>
#include <maths/CMathsFuncs.h>
#include <maths/CMultimodalPriorUtils.h>
#include <maths/CNormalMeanPrecConjugate.h>
#include <maths/COrderings.h>
#include <maths/CPriorStateSerialiser.h>
#include <maths/CSampling.h>
#include <maths/CSetTools.h>
#include <maths/CSolvers.h>
#include <maths/CTools.h>
#include <maths/ProbabilityAggregators.h>

#include <boost/bind.hpp>
#include <boost/optional.hpp>
#include <boost/numeric/conversion/bounds.hpp>
#include <boost/ref.hpp>

#include <set>

#include <math.h>

namespace ml
{
namespace maths
{

namespace
{

typedef std::pair<std::size_t, double> TSizeDoublePr;
typedef std::pair<double, double> TDoubleDoublePr;
typedef std::set<std::size_t> TSizeSet;

const std::size_t MODE_SPLIT_NUMBER_SAMPLES(50u);
const std::size_t MODE_MERGE_NUMBER_SAMPLES(25u);

// We obfuscate the XML element names to avoid giving away too much
// information about our model.
const std::string CLUSTERER_TAG("a");
const std::string SEED_PRIOR_TAG("b");
const std::string MODE_TAG("c");
const std::string NUMBER_SAMPLES_TAG("d");
const std::string MINIMUM_TAG("e");
const std::string MAXIMUM_TAG("f");
const std::string DECAY_RATE_TAG("g");
const std::string EMPTY_STRING;

}

//////// CMultimodalPrior Implementation ////////

CMultimodalPrior::CMultimodalPrior(maths_t::EDataType dataType,
                                   const CClusterer1d &clusterer,
                                   const CPrior &seedPrior,
                                   double decayRate/*= 0.0*/) :
        CPrior(dataType, decayRate),
        m_Clusterer(clusterer.clone()),
        m_SeedPrior(seedPrior.clone())
{
    // Register the split and merge callbacks.
    m_Clusterer->splitFunc(CModeSplitCallback(*this));
    m_Clusterer->mergeFunc(CModeMergeCallback(*this));
}

CMultimodalPrior::CMultimodalPrior(maths_t::EDataType dataType,
                                   const TMeanVarAccumulatorVec &moments,
                                   double decayRate/*= 0.0*/) :
        CPrior(dataType, decayRate),
        m_SeedPrior(CNormalMeanPrecConjugate::nonInformativePrior(dataType, decayRate).clone())
{
    typedef std::vector<CNormalMeanPrecConjugate> TNormalVec;

    TNormalVec normals;
    normals.reserve(moments.size());

    for (std::size_t i = 0u; i < moments.size(); ++i)
    {
        normals.push_back(CNormalMeanPrecConjugate(dataType, moments[i], decayRate));
    }

    m_Clusterer.reset(new CKMeansOnline1d(normals));

    m_Modes.reserve(normals.size());
    for (std::size_t i = 0u; i < normals.size(); ++i)
    {
        m_Modes.push_back(TMode(i, TPriorPtr(normals.back().clone())));
    }
}

CMultimodalPrior::CMultimodalPrior(maths_t::EDataType dataType, TPriorPtrVec &priors):
        CPrior(dataType, 0.0)
{
    m_Modes.reserve(priors.size());
    for (std::size_t i = 0u; i < priors.size(); ++i)
    {
        m_Modes.push_back(TMode(i, priors[i]));
    }
}

CMultimodalPrior::CMultimodalPrior(const SDistributionRestoreParams &params,
                                   core::CStateRestoreTraverser &traverser) :
        CPrior(params.s_DataType, params.s_DecayRate)
{
    traverser.traverseSubLevel(boost::bind(&CMultimodalPrior::acceptRestoreTraverser,
                                           this, boost::cref(params), _1));
}

bool CMultimodalPrior::acceptRestoreTraverser(const SDistributionRestoreParams &params,
                                              core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_SETUP_TEARDOWN(DECAY_RATE_TAG,
                               double decayRate,
                               core::CStringUtils::stringToType(traverser.value(), decayRate),
                               this->decayRate(decayRate))
        RESTORE(CLUSTERER_TAG, traverser.traverseSubLevel(boost::bind<bool>(CClustererStateSerialiser(),
                                                                            boost::cref(params),
                                                                            boost::ref(m_Clusterer), _1)))
        RESTORE(SEED_PRIOR_TAG, traverser.traverseSubLevel(boost::bind<bool>(CPriorStateSerialiser(),
                                                                             boost::cref(params),
                                                                             boost::ref(m_SeedPrior), _1)))
        RESTORE_SETUP_TEARDOWN(MODE_TAG,
                               TMode mode,
                               traverser.traverseSubLevel(boost::bind(&TMode::acceptRestoreTraverser,
                                                                      &mode, boost::cref(params), _1)),
                               m_Modes.push_back(mode))
        RESTORE_SETUP_TEARDOWN(NUMBER_SAMPLES_TAG,
                               double numberSamples,
                               core::CStringUtils::stringToType(traverser.value(), numberSamples),
                               this->numberSamples(numberSamples))
        RESTORE(MINIMUM_TAG, this->minimum().restore(traverser))
        RESTORE(MAXIMUM_TAG, this->maximum().restore(traverser))
    }
    while (traverser.next());

    if (m_Clusterer)
    {
        // Register the split and merge callbacks.
        m_Clusterer->splitFunc(CModeSplitCallback(*this));
        m_Clusterer->mergeFunc(CModeMergeCallback(*this));
    }

    return true;
}

CMultimodalPrior::CMultimodalPrior(const CMultimodalPrior &other) :
        CPrior(other.dataType(), other.decayRate()),
        m_Clusterer(other.m_Clusterer->clone()),
        m_SeedPrior(other.m_SeedPrior->clone())
{
    // Register the split and merge callbacks.
    m_Clusterer->splitFunc(CModeSplitCallback(*this));
    m_Clusterer->mergeFunc(CModeMergeCallback(*this));

    // Clone all the modes up front so we can implement strong exception safety.
    TModeVec modes;
    modes.reserve(other.m_Modes.size());
    for (std::size_t i = 0u; i < other.m_Modes.size(); ++i)
    {
        const TMode &mode = other.m_Modes[i];
        modes.push_back(TMode(mode.s_Index, TPriorPtr(mode.s_Prior->clone())));
    }
    m_Modes.swap(modes);

    this->addSamples(other.numberSamples());
    this->minimum() = other.minimum();
    this->maximum() = other.maximum();
}

CMultimodalPrior &CMultimodalPrior::operator=(const CMultimodalPrior &rhs)
{
    if (this != &rhs)
    {
        CMultimodalPrior copy(rhs);
        this->swap(copy);
    }
    return *this;
}

void CMultimodalPrior::swap(CMultimodalPrior &other)
{
    this->CPrior::swap(other);

    std::swap(m_Clusterer, other.m_Clusterer);
    // The call backs for split and merge should point to the
    // appropriate priors (we don't swap the "this" pointers
    // after all). So we need to refresh them after swapping.
    m_Clusterer->splitFunc(CModeSplitCallback(*this));
    m_Clusterer->mergeFunc(CModeMergeCallback(*this));
    other.m_Clusterer->splitFunc(CModeSplitCallback(other));
    other.m_Clusterer->mergeFunc(CModeMergeCallback(other));

    std::swap(m_SeedPrior, other.m_SeedPrior);
    m_Modes.swap(other.m_Modes);
}

CMultimodalPrior::EPrior CMultimodalPrior::type(void) const
{
    return E_Multimodal;
}

CMultimodalPrior *CMultimodalPrior::clone(void) const
{
    return new CMultimodalPrior(*this);
}

void CMultimodalPrior::dataType(maths_t::EDataType value)
{
    this->CPrior::dataType(value);
    m_Clusterer->dataType(value);
    for (std::size_t i = 0u; i < m_Modes.size(); ++i)
    {
        m_Modes[i].s_Prior->dataType(value);
    }
}

void CMultimodalPrior::decayRate(double value)
{
    this->CPrior::decayRate(value);
    m_Clusterer->decayRate(value);
    for (std::size_t i = 0u; i < m_Modes.size(); ++i)
    {
        m_Modes[i].s_Prior->decayRate(value);
    }
    m_SeedPrior->decayRate(value);
}

void CMultimodalPrior::setToNonInformative(double /*offset*/, double decayRate)
{
    m_Clusterer->clear();
    m_Modes.clear();
    this->decayRate(decayRate);
    this->numberSamples(0.0);
}

bool CMultimodalPrior::needsOffset(void) const
{
    for (std::size_t i = 0u; i < m_Modes.size(); ++i)
    {
        if (m_Modes[i].s_Prior->needsOffset())
        {
            return true;
        }
    }
    return false;
}

void CMultimodalPrior::adjustOffset(const TWeightStyleVec &weightStyles,
                                    const TDouble1Vec &samples,
                                    const TDouble4Vec1Vec &weights)
{
    if (this->needsOffset())
    {
        CMultimodalOffsetCost cost(*this);
        CMultimodalApplyOffset apply(*this);
        this->adjustOffsetWithCost(weightStyles, samples, weights, cost, apply);
    }
}

double CMultimodalPrior::offset(void) const
{
    double offset = 0.0;
    for (std::size_t i = 0u; i < m_Modes.size(); ++i)
    {
        offset = std::max(offset, m_Modes[i].s_Prior->offset());
    }
    return offset;
}

void CMultimodalPrior::addSamples(const TWeightStyleVec &weightStyles_,
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

    this->adjustOffset(weightStyles_, samples, weights);

    // This uses a clustering methodology (defined by m_Clusterer)
    // to assign each sample to a cluster. Each cluster has its own
    // mode in the distribution, which comprises a weight and prior
    // which are updated based on the assignment.
    //
    // The idea in separating clustering from modeling is to enable
    // different clustering methodologies to be used as appropriate
    // to the particular data set under consideration and based on
    // the runtime available.
    //
    // It is intended that approximate Bayesian schemes for multimode
    // distributions, such as the variational treatment of a mixture
    // of Gaussians, are implemented as separate priors.
    //
    // Flow through this function is as follows:
    //   1) Assign the samples to clusters.
    //   2) Find or create corresponding modes as necessary.
    //   3) Update the mode's weight and prior with the samples
    //      assigned to it.

    typedef core::CSmallVector<TSizeDoublePr, 2> TSizeDoublePr2Vec;

    // Declared outside the loop to minimize the number of times it
    // is initialized.
    TWeightStyleVec weightStyles(weightStyles_);
    TDouble1Vec sample(1);
    TDouble4Vec1Vec weight(1);
    TSizeDoublePr2Vec clusters;

    std::size_t svi = static_cast<std::size_t>(
                              std::find(weightStyles.begin(),
                                        weightStyles.end(),
                                        maths_t::E_SampleSeasonalVarianceScaleWeight)
                            - weightStyles.begin());
    std::size_t ci  = static_cast<std::size_t>(
                              std::find(weightStyles.begin(),
                                        weightStyles.end(),
                                        maths_t::E_SampleCountWeight)
                            - weightStyles.begin());
    if (ci == weightStyles.size())
    {
        weightStyles.push_back(maths_t::E_SampleCountWeight);
    }

    try
    {
        double mean = (  !this->isNonInformative()
                       && maths_t::hasSeasonalVarianceScale(weightStyles_, weights)) ?
                      this->marginalLikelihoodMean() : 0.0;

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            double seasonalScale = !this->isNonInformative() && svi < weights[i].size() ?
                                   ::sqrt(weights[i][svi]) : 1.0;

            double x = samples[i];
            if (!CMathsFuncs::isFinite(x))
            {
                LOG_ERROR("Discarding " << x);
                continue;
            }
            x = mean + (x - mean) / seasonalScale;

            this->minimum().add(x);
            this->maximum().add(x);

            clusters.clear();
            m_Clusterer->add(x, clusters, maths_t::count(weightStyles_, weights[i]));

            sample[0] = x;
            weight[0] = weights[i];
            if (svi < weight[0].size())
            {
                weight[0][svi] = 1.0;
            }
            if (ci == weight[0].size())
            {
                weight[0].push_back(1.0);
            }

            // This has to happen after adding the sample to the clusterer.
            this->addSamples(maths_t::countForUpdate(weightStyles_, weights[i]));

            for (std::size_t j = 0u; j < clusters.size(); ++j)
            {
                TModeVecItr k = std::find_if(m_Modes.begin(), m_Modes.end(),
                                             CSetTools::CIndexInSet(clusters[j].first));
                if (k == m_Modes.end())
                {
                    LOG_TRACE("Creating mode with index " << clusters[j].first);
                    m_Modes.push_back(TMode(clusters[j].first, m_SeedPrior));
                    k = m_Modes.end();
                    --k;
                }
                if (!CMathsFuncs::isFinite(clusters[j].second))
                {
                    LOG_ERROR("Bad cluster weight " << clusters[j].second);
                    continue;
                }
                weight[0][ci] = clusters[j].second;
                k->s_Prior->addSamples(weightStyles, sample, weight);
            }
        }
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to update likelihood: " << e.what());
    }
}

void CMultimodalPrior::propagateForwardsByTime(double time)
{
    if (!CMathsFuncs::isFinite(time) || time < 0.0)
    {
        LOG_ERROR("Bad propagation time " << time);
        return;
    }

    if (this->isNonInformative())
    {
        // Nothing to be done.
        return;
    }

    // We want to hold the probabilities constant. Since the i'th
    // probability:
    //   p(i) = w(i) / Sum_j{ w(j) }
    //
    // where w(i) is its weight we can achieve this by multiplying
    // all weights by some factor f in the range [0, 1].

    m_Clusterer->propagateForwardsByTime(time);

    for (std::size_t i = 0u; i < m_Modes.size(); ++i)
    {
        m_Modes[i].s_Prior->propagateForwardsByTime(time);
    }

    double alpha = ::exp(-this->decayRate() * time);
    this->numberSamples(this->numberSamples() * alpha);
    LOG_TRACE("numberSamples = " << this->numberSamples());
}

TDoubleDoublePr CMultimodalPrior::marginalLikelihoodSupport(void) const
{
    return CMultimodalPriorUtils::marginalLikelihoodSupport(m_Modes);
}

double CMultimodalPrior::marginalLikelihoodMean(void) const
{
    return CMultimodalPriorUtils::marginalLikelihoodMean(m_Modes);
}

double CMultimodalPrior::nearestMarginalLikelihoodMean(double value) const
{
    if (m_Modes.empty())
    {
        return 0.0;
    }

    double mean = m_Modes[0].s_Prior->marginalLikelihoodMean();
    double distance = ::fabs(value - mean);
    double result = mean;
    for (std::size_t i = 1u; i < m_Modes.size(); ++i)
    {
        mean = m_Modes[i].s_Prior->marginalLikelihoodMean();
        if (::fabs(value - mean) < distance)
        {
            distance = ::fabs(value - mean);
            result = mean;
        }
    }
    return result;
}

double CMultimodalPrior::marginalLikelihoodMode(const TWeightStyleVec &weightStyles,
                                                const TDouble4Vec &weights) const
{
    return CMultimodalPriorUtils::marginalLikelihoodMode(m_Modes, weightStyles, weights);
}

double CMultimodalPrior::marginalLikelihoodVariance(const TWeightStyleVec &weightStyles,
                                                    const TDouble4Vec &weights) const
{
    return CMultimodalPriorUtils::marginalLikelihoodVariance(m_Modes, weightStyles, weights);
}

TDoubleDoublePr CMultimodalPrior::marginalLikelihoodConfidenceInterval(double percentage,
                                                                       const TWeightStyleVec &weightStyles,
                                                                       const TDouble4Vec &weights) const
{
    return CMultimodalPriorUtils::marginalLikelihoodConfidenceInterval(*this, m_Modes, percentage, weightStyles, weights);
}

maths_t::EFloatingPointErrorStatus
CMultimodalPrior::jointLogMarginalLikelihood(const TWeightStyleVec &weightStyles,
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

    if (this->isNonInformative())
    {
        // The non-informative likelihood is improper and effectively
        // zero everywhere. We use minus max double because
        // log(0) = HUGE_VALUE, which causes problems for Windows.
        // Calling code is notified when the calculation overflows
        // and should avoid taking the exponential since this will
        // underflow and pollute the floating point environment. This
        // may cause issues for some library function implementations
        // (see fe*exceptflag for more details).
        result = boost::numeric::bounds<double>::lowest();
        return maths_t::E_FpOverflowed;
    }

    return m_Modes.size() == 1 ?
           m_Modes[0].s_Prior->jointLogMarginalLikelihood(weightStyles, samples, weights, result) :
           CMultimodalPriorUtils::jointLogMarginalLikelihood(m_Modes, weightStyles, samples, weights, result);
}

void CMultimodalPrior::sampleMarginalLikelihood(std::size_t numberSamples,
                                                TDouble1Vec &samples) const
{
    samples.clear();

    if (numberSamples == 0 || this->numberSamples() == 0.0)
    {
        return;
    }

    CMultimodalPriorUtils::sampleMarginalLikelihood(m_Modes, numberSamples, samples);
}

bool CMultimodalPrior::minusLogJointCdf(const TWeightStyleVec &weightStyles,
                                        const TDouble1Vec &samples,
                                        const TDouble4Vec1Vec &weights,
                                        double &lowerBound,
                                        double &upperBound) const
{
    return CMultimodalPriorUtils::minusLogJointCdf(m_Modes,
                                                   weightStyles,
                                                   samples,
                                                   weights,
                                                   lowerBound, upperBound);
}

bool CMultimodalPrior::minusLogJointCdfComplement(const TWeightStyleVec &weightStyles,
                                                  const TDouble1Vec &samples,
                                                  const TDouble4Vec1Vec &weights,
                                                  double &lowerBound,
                                                  double &upperBound) const
{
    return CMultimodalPriorUtils::minusLogJointCdfComplement(m_Modes,
                                                             weightStyles,
                                                             samples,
                                                             weights,
                                                             lowerBound, upperBound);
}

bool CMultimodalPrior::probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
                                                      const TWeightStyleVec &weightStyles,
                                                      const TDouble1Vec &samples,
                                                      const TDouble4Vec1Vec &weights,
                                                      double &lowerBound,
                                                      double &upperBound,
                                                      maths_t::ETail &tail) const
{
    return CMultimodalPriorUtils::probabilityOfLessLikelySamples(*this, m_Modes,
                                                                 calculation,
                                                                 weightStyles,
                                                                 samples,
                                                                 weights,
                                                                 lowerBound, upperBound, tail);
}

bool CMultimodalPrior::isNonInformative(void) const
{
    return CMultimodalPriorUtils::isNonInformative(m_Modes);
}

void CMultimodalPrior::print(const std::string &indent, std::string &result) const
{
    CMultimodalPriorUtils::print(m_Modes, indent, result);
}

std::string CMultimodalPrior::printJointDensityFunction(void) const
{
    return "Not supported";
}

uint64_t CMultimodalPrior::checksum(uint64_t seed) const
{
    seed = this->CPrior::checksum(seed);
    seed = CChecksum::calculate(seed, m_Clusterer);
    seed = CChecksum::calculate(seed, m_SeedPrior);
    return CChecksum::calculate(seed, m_Modes);
}

void CMultimodalPrior::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CMultimodalPrior");
    core::CMemoryDebug::dynamicSize("m_Clusterer", m_Clusterer, mem);
    core::CMemoryDebug::dynamicSize("m_SeedPrior", m_SeedPrior, mem);
    core::CMemoryDebug::dynamicSize("m_Modes", m_Modes, mem);
}

std::size_t CMultimodalPrior::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_Clusterer);
    mem += core::CMemory::dynamicSize(m_SeedPrior);
    mem += core::CMemory::dynamicSize(m_Modes);
    return mem;
}

std::size_t CMultimodalPrior::staticSize(void) const
{
    return sizeof(*this);
}

void CMultimodalPrior::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(CLUSTERER_TAG,
                         boost::bind<void>(CClustererStateSerialiser(),
                                           boost::cref(*m_Clusterer),
                                           _1));
    inserter.insertLevel(SEED_PRIOR_TAG,
                         boost::bind<void>(CPriorStateSerialiser(), boost::cref(*m_SeedPrior), _1));
    for (std::size_t i = 0u; i < m_Modes.size(); ++i)
    {
        inserter.insertLevel(MODE_TAG,
                             boost::bind(&TMode::acceptPersistInserter, &m_Modes[i], _1));
    }
    inserter.insertValue(DECAY_RATE_TAG, this->decayRate(), core::CIEEE754::E_SinglePrecision);
    inserter.insertValue(NUMBER_SAMPLES_TAG, this->numberSamples(), core::CIEEE754::E_SinglePrecision);
    this->minimum().persist(MINIMUM_TAG, inserter);
    this->maximum().persist(MAXIMUM_TAG, inserter);
}

std::size_t CMultimodalPrior::numberModes(void) const
{
    return m_Modes.size();
}

bool CMultimodalPrior::checkInvariants(const std::string &tag) const
{
    bool result = true;

    if (m_Modes.size() != m_Clusterer->numberClusters())
    {
        LOG_ERROR(tag << "# modes = " << m_Modes.size()
                      << ", # clusters = " << m_Clusterer->numberClusters());
        result = false;
    }

    double numberSamples = this->numberSamples();
    double modeSamples = 0.0;
    for (std::size_t i = 0u; i < m_Modes.size(); ++i)
    {
        if (!m_Clusterer->hasCluster(m_Modes[i].s_Index))
        {
            LOG_ERROR(tag << "Expected cluster for = " << m_Modes[i].s_Index);
            result = false;
        }
        modeSamples += m_Modes[i].s_Prior->numberSamples();
    }

    CEqualWithTolerance<double> equal(  CToleranceTypes::E_AbsoluteTolerance
                                      | CToleranceTypes::E_RelativeTolerance,
                                      1e-3);
    if (!equal(modeSamples, numberSamples))
    {
        LOG_ERROR(tag << "Sum mode samples = " << modeSamples
                      << ", total samples = " << numberSamples);
        result = false;
    }

    return result;
}

bool CMultimodalPrior::participatesInModelSelection(void) const
{
    return m_Modes.size() > 1;
}

double CMultimodalPrior::unmarginalizedParameters(void) const
{
    return std::max(static_cast<double>(m_Modes.size()), 1.0) - 1.0;
}

std::string CMultimodalPrior::debugWeights(void) const
{
    return CMultimodalPriorUtils::debugWeights(m_Modes);
}


////////// CMultimodalPrior::CModeSplitCallback Implementation //////////

CMultimodalPrior::CModeSplitCallback::CModeSplitCallback(CMultimodalPrior &prior) :
        m_Prior(&prior)
{
}

void CMultimodalPrior::CModeSplitCallback::operator()(std::size_t sourceIndex,
                                                      std::size_t leftSplitIndex,
                                                      std::size_t rightSplitIndex) const
{
    LOG_TRACE("Splitting mode with index " << sourceIndex);

    TModeVec &modes = m_Prior->m_Modes;

    // Remove the split mode.
    TModeVecItr mode = std::find_if(modes.begin(),
                                    modes.end(),
                                    CSetTools::CIndexInSet(sourceIndex));
    double numberSamples = mode != modes.end() ? mode->weight() : 0.0;
    modes.erase(mode);

    double pLeft  = m_Prior->m_Clusterer->probability(leftSplitIndex);
    double pRight = m_Prior->m_Clusterer->probability(rightSplitIndex);
    double Z = (pLeft + pRight);
    if (Z > 0.0)
    {
        pLeft  /= Z;
        pRight /= Z;
    }
    LOG_TRACE("# samples = " << numberSamples
              << ", pLeft = " << pLeft
              << ", pRight = " << pRight);

    // Create the child modes.

    LOG_TRACE("Creating mode with index " << leftSplitIndex);
    modes.push_back(TMode(leftSplitIndex, m_Prior->m_SeedPrior));
    {
        TDoubleVec samples;
        if (!m_Prior->m_Clusterer->sample(leftSplitIndex, MODE_SPLIT_NUMBER_SAMPLES, samples))
        {
            LOG_ERROR("Couldn't find cluster for " << leftSplitIndex);
        }
        LOG_TRACE("samples = " << core::CContainerPrinter::print(samples));

        double nl = pLeft * numberSamples;
        double ns = std::min(nl, 4.0);
        double n  = static_cast<double>(samples.size());
        LOG_TRACE("# left = " << nl);

        double seedWeight = ns / n;
        TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, seedWeight));
        modes.back().s_Prior->addSamples(TWeights::COUNT, samples, weights);

        double weight = (nl - ns) / n;
        if (weight > 0.0)
        {
            weights.assign(weights.size(), TDouble4Vec(1, weight));
            modes.back().s_Prior->addSamples(TWeights::COUNT, samples, weights);
            LOG_TRACE(modes.back().s_Prior->print());
        }
    }

    LOG_TRACE("Creating mode with index " << rightSplitIndex);
    modes.push_back(TMode(rightSplitIndex, m_Prior->m_SeedPrior));
    {
        TDoubleVec samples;
        if (!m_Prior->m_Clusterer->sample(rightSplitIndex, MODE_SPLIT_NUMBER_SAMPLES, samples))
        {
            LOG_ERROR("Couldn't find cluster for " << rightSplitIndex)
        }
        LOG_TRACE("samples = " << core::CContainerPrinter::print(samples));

        double nr = pRight * numberSamples;
        double ns = std::min(nr, 4.0);
        double n  = static_cast<double>(samples.size());
        LOG_TRACE("# right = " << nr);

        double seedWeight = ns / n;
        TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, seedWeight));
        modes.back().s_Prior->addSamples(TWeights::COUNT, samples, weights);

        double weight = (nr - ns) / n;
        if (weight > 0.0)
        {
            weights.assign(weights.size(), TDouble4Vec(1, weight));
            modes.back().s_Prior->addSamples(TWeights::COUNT, samples, weights);
            LOG_TRACE(modes.back().s_Prior->print());
        }
    }

    if (!m_Prior->checkInvariants("SPLIT: "))
    {
        LOG_ERROR("# samples = " << numberSamples
                  << ", # modes = " << modes.size()
                  << ", pLeft = " << pLeft
                  << ", pRight = " << pRight);
    }

    LOG_TRACE("Split mode");
}


////////// CMultimodalPrior::CModeMergeCallback Implementation //////////

CMultimodalPrior::CModeMergeCallback::CModeMergeCallback(CMultimodalPrior &prior) :
        m_Prior(&prior)
{
}

void CMultimodalPrior::CModeMergeCallback::operator()(std::size_t leftMergeIndex,
                                                      std::size_t rightMergeIndex,
                                                      std::size_t targetIndex) const
{
    LOG_TRACE("Merging modes with indices "
              << leftMergeIndex << " " << rightMergeIndex);

    TModeVec &modes = m_Prior->m_Modes;

    // Create the new mode.
    TMode newMode(targetIndex, m_Prior->m_SeedPrior);

    double wl = 0.0;
    double wr = 0.0;
    double n  = 0.0;
    std::size_t nl = 0;
    std::size_t nr = 0;
    TDouble1Vec samples;

    TModeVecCItr leftMode = std::find_if(modes.begin(), modes.end(),
                                         CSetTools::CIndexInSet(leftMergeIndex));
    if (leftMode != modes.end())
    {
        wl = leftMode->s_Prior->numberSamples();
        n += wl;
        TDouble1Vec leftSamples;
        leftMode->s_Prior->sampleMarginalLikelihood(MODE_MERGE_NUMBER_SAMPLES, leftSamples);
        nl = leftSamples.size();
        samples.insert(samples.end(), leftSamples.begin(), leftSamples.end());
    }
    else
    {
        LOG_ERROR("Couldn't find mode for " << leftMergeIndex);
    }

    TModeVecCItr rightMode = std::find_if(modes.begin(), modes.end(),
                                          CSetTools::CIndexInSet(rightMergeIndex));
    if (rightMode != modes.end())
    {
        wr = rightMode->s_Prior->numberSamples();
        n += wr;
        TDouble1Vec rightSamples;
        rightMode->s_Prior->sampleMarginalLikelihood(MODE_MERGE_NUMBER_SAMPLES, rightSamples);
        nr = rightSamples.size();
        samples.insert(samples.end(), rightSamples.begin(), rightSamples.end());
    }
    else
    {
        LOG_ERROR("Couldn't find mode for " << rightMergeIndex);
    }

    if (n > 0.0)
    {
        double nl_ = static_cast<double>(nl);
        double nr_ = static_cast<double>(nr);
        double Z = (nl_ * wl + nr_ * wr) / (nl_ + nr_);
        wl /= Z;
        wr /= Z;
    }

    LOG_TRACE("samples = " << core::CContainerPrinter::print(samples));
    LOG_TRACE("n = " << n << ", wl = " << wl << ", wr = " << wr);

    double ns = std::min(n, 4.0);
    double s  = static_cast<double>(samples.size());

    double seedWeight = ns / s;
    TDouble4Vec1Vec weights;
    weights.reserve(samples.size());
    weights.resize(nl,      TDouble1Vec(1, wl * seedWeight));
    weights.resize(nl + nr, TDouble1Vec(1, wr * seedWeight));
    newMode.s_Prior->addSamples(TWeights::COUNT, samples, weights);

    double weight = (n - ns) / s;
    if (weight > 0.0)
    {
        for (std::size_t i = 0u; i < weights.size(); ++i)
        {
            weights[i][0] *= weight / seedWeight;
        }
        newMode.s_Prior->addSamples(TWeights::COUNT, samples, weights);
    }

    // Remove the merged modes.
    TSizeSet mergedIndices;
    mergedIndices.insert(leftMergeIndex);
    mergedIndices.insert(rightMergeIndex);
    modes.erase(std::remove_if(modes.begin(), modes.end(),
                               CSetTools::CIndexInSet(mergedIndices)), modes.end());

    // Add the new mode.
    LOG_TRACE("Creating mode with index " << targetIndex);
    modes.push_back(newMode);

    m_Prior->checkInvariants("MERGE: ");

    LOG_TRACE("Merged modes");
}


////////// CMultimodalPrior::CMultimodalOffsetReward Implementation //////////

CMultimodalPrior::CMultimodalOffsetCost::CMultimodalOffsetCost(CMultimodalPrior &prior) :
        COffsetCost(prior),
        m_Prior(&prior)
{}

void CMultimodalPrior::CMultimodalOffsetCost::resample(double minimumSample)
{
    TModeVec &modes = m_Prior->m_Modes;
    m_Resamples.resize(modes.size());
    m_ResamplesWeights.resize(modes.size());
    for (std::size_t i = 0u; i < modes.size(); ++i)
    {
        modes[i].s_Prior->adjustOffsetResamples(minimumSample, m_Resamples[i], m_ResamplesWeights[i]);
    }
}

void CMultimodalPrior::CMultimodalOffsetCost::resetPriors(double offset) const
{
    typedef std::pair<double, std::size_t> TDoubleSizePr;
    typedef CBasicStatistics::COrderStatisticsStack<TDoubleSizePr, 1, COrderings::SFirstLess> TMaxAccumulator;

    TModeVec &modes = m_Prior->m_Modes;
    for (std::size_t i = 0u; i < modes.size(); ++i)
    {
        modes[i].s_Prior->setToNonInformative(offset, modes[i].s_Prior->decayRate());
        modes[i].s_Prior->addSamples(TWeights::COUNT, m_Resamples[i], m_ResamplesWeights[i]);
    }
    for (std::size_t i = 0u; i < this->samples().size(); ++i)
    {
        // Assign samples to maximize the likelihood.
        TDouble1Vec si(1, this->samples()[i]);
        TDouble4Vec1Vec wi(1, this->weights()[i]);
        TMaxAccumulator mi;
        for (std::size_t j = 0u; j < modes.size(); ++j)
        {
            double likelihood;
            if (modes[j].s_Prior->jointLogMarginalLikelihood(this->weightStyles(), si, wi,
                                                             likelihood) != maths_t::E_FpFailed)
            {
                mi.add(std::make_pair(likelihood, j));
            }
        }
        if (mi.count() > 0)
        {
            modes[mi[0].second].s_Prior->addSamples(this->weightStyles(), si, wi);
        }
    }
}


////////// CMultimodalPrior::CMultimodalApplyOffset Implementation //////////

CMultimodalPrior::CMultimodalApplyOffset::CMultimodalApplyOffset(CMultimodalPrior &prior) :
        CApplyOffset(prior),
        m_Prior(&prior)
{}

void CMultimodalPrior::CMultimodalApplyOffset::operator()(double offset) const
{
    TModeVec &modes = m_Prior->m_Modes;
    double n = 0.0;
    for (std::size_t i = 0u; i < modes.size(); ++i)
    {
        modes[i].s_Prior->setToNonInformative(offset, modes[i].s_Prior->decayRate());
        modes[i].s_Prior->addSamples(TWeights::COUNT, m_Resamples[i], m_ResamplesWeights[i]);
        n += modes[i].s_Prior->numberSamples();
    }
    m_Prior->numberSamples(n);
}

void CMultimodalPrior::CMultimodalApplyOffset::resample(double minimumSample)
{
    TModeVec &modes = m_Prior->m_Modes;
    m_Resamples.resize(modes.size());
    m_ResamplesWeights.resize(modes.size());
    for (std::size_t i = 0u; i < modes.size(); ++i)
    {
        modes[i].s_Prior->adjustOffsetResamples(minimumSample, m_Resamples[i], m_ResamplesWeights[i]);
    }
}

}
}
