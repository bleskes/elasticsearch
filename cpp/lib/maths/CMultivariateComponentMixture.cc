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

#include <maths/CMultivariateComponentMixture.h>

#include <maths/CMultimodalPriorUtils.h>

namespace ml
{
namespace maths
{

CMultivariateComponentMixture::CMultivariateComponentMixture(double numberSamples,
                                                             const TComponentCPtrVec &modes) :
        m_NumberSamples(numberSamples),
        m_Modes(modes.size())
{
    for (std::size_t i = 0u; i < modes.size(); ++i)
    {
        m_Modes[i].s_Index = i;
        m_Modes[i].s_Prior = modes[i];
    }
}

CMultivariateComponentMixture::TDoubleDoublePr
CMultivariateComponentMixture::marginalLikelihoodSupport(void) const
{
    return CMultimodalPriorUtils::marginalLikelihoodSupport(m_Modes);
}

double CMultivariateComponentMixture::marginalLikelihoodMean(void) const
{
    return CMultimodalPriorUtils::marginalLikelihoodMean(m_Modes);
}

double CMultivariateComponentMixture::marginalLikelihoodMode(const TWeightStyleVec &weightStyles,
                                                             const TDouble4Vec &weights) const
{
    return CMultimodalPriorUtils::marginalLikelihoodMode(m_Modes, weightStyles, weights);
}

double CMultivariateComponentMixture::marginalLikelihoodVariance(const TWeightStyleVec &weightStyles,
                                                                 const TDouble4Vec &weights) const
{
    return CMultimodalPriorUtils::marginalLikelihoodVariance(m_Modes, weightStyles, weights);
}

CMultivariateComponentMixture::TDoubleDoublePr
CMultivariateComponentMixture::marginalLikelihoodConfidenceInterval(double percentage,
                                                                    const TWeightStyleVec &weightStyles,
                                                                    const TDouble4Vec &weights) const
{
    return CMultimodalPriorUtils::marginalLikelihoodConfidenceInterval(*this, m_Modes, percentage, weightStyles, weights);
}

maths_t::EFloatingPointErrorStatus
CMultivariateComponentMixture::jointLogMarginalLikelihood(const TWeightStyleVec &weightStyles,
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

    if (m_Modes.size() == 1)
    {
        return m_Modes[0].s_Prior->jointLogMarginalLikelihood(weightStyles, samples, weights, result);
    }

    return CMultimodalPriorUtils::jointLogMarginalLikelihood(m_Modes, weightStyles, samples, weights, result);
}

void CMultivariateComponentMixture::sampleMarginalLikelihood(std::size_t numberSamples,
                                                             TDouble1Vec &samples) const
{
    samples.clear();

    if (numberSamples == 0 || m_NumberSamples == 0.0)
    {
        return;
    }

    CMultimodalPriorUtils::sampleMarginalLikelihood(m_Modes, numberSamples, samples);
}

bool CMultivariateComponentMixture::minusLogJointCdf(const TWeightStyleVec &weightStyles,
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

bool CMultivariateComponentMixture::minusLogJointCdfComplement(const TWeightStyleVec &weightStyles,
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

bool CMultivariateComponentMixture::probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
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

bool CMultivariateComponentMixture::isNonInformative(void) const
{
    return CMultimodalPriorUtils::isNonInformative(m_Modes);
}

double CMultivariateComponentMixture::numberSamples(void) const
{
    return m_NumberSamples;
}

void CMultivariateComponentMixture::print(const std::string &indent, std::string &result) const
{
    CMultimodalPriorUtils::print(m_Modes, indent, result);
}

uint64_t CMultivariateComponentMixture::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_NumberSamples);
    return CChecksum::calculate(seed, m_Modes);
}

}
}
