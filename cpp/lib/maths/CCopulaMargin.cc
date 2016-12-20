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

#include <maths/CCopulaMargin.h>

#include <maths/CPrior.h>

namespace prelert
{
namespace maths
{

CCopulaMargin::CCopulaMargin(const TPriorPtr &margin) :
        m_Margin(margin)
{
}

CCopulaMargin::TDoubleDoublePr CCopulaMargin::marginalLikelihoodSupport(void) const
{
    return m_Margin->marginalLikelihoodSupport();
}

double CCopulaMargin::marginalLikelihoodMean(void) const
{
    return m_Margin->marginalLikelihoodMean();
}

double CCopulaMargin::marginalLikelihoodMode(const TWeightStyleVec &weightStyles,
                                             const TDouble4Vec &weights) const
{
    return m_Margin->marginalLikelihoodMode(weightStyles, weights);
}

double CCopulaMargin::marginalLikelihoodVariance(const TWeightStyleVec &weightStyles,
                                                 const TDouble4Vec &weights) const
{
    return m_Margin->marginalLikelihoodVariance(weightStyles, weights);
}

CCopulaMargin::TDoubleDoublePr
CCopulaMargin::marginalLikelihoodConfidenceInterval(double percentage,
                                                    const TWeightStyleVec &weightStyles,
                                                    const TDouble4Vec &weights) const
{
    return m_Margin->marginalLikelihoodConfidenceInterval(percentage, weightStyles, weights);
}

maths_t::EFloatingPointErrorStatus
CCopulaMargin::jointLogMarginalLikelihood(const TWeightStyleVec &weightStyles,
                                          const TDouble1Vec &samples,
                                          const TDouble4Vec1Vec &weights,
                                          double &result) const
{
    return m_Margin->jointLogMarginalLikelihood(weightStyles, samples, weights, result);
}

void CCopulaMargin::sampleMarginalLikelihood(std::size_t numberSamples,
                                             TDouble1Vec &samples) const
{
    return m_Margin->sampleMarginalLikelihood(numberSamples, samples);
}

bool CCopulaMargin::minusLogJointCdf(const TWeightStyleVec &weightStyles,
                                     const TDouble1Vec &samples,
                                     const TDouble4Vec1Vec &weights,
                                     double &lowerBound,
                                     double &upperBound) const
{
    return m_Margin->minusLogJointCdf(weightStyles, samples, weights, lowerBound, upperBound);
}

bool CCopulaMargin::minusLogJointCdfComplement(const TWeightStyleVec &weightStyles,
                                               const TDouble1Vec &samples,
                                               const TDouble4Vec1Vec &weights,
                                               double &lowerBound,
                                               double &upperBound) const
{
    return m_Margin->minusLogJointCdfComplement(weightStyles, samples, weights, lowerBound, upperBound);
}

bool CCopulaMargin::probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
                                                   const TWeightStyleVec &weightStyles,
                                                   const TDouble1Vec &samples,
                                                   const TDouble4Vec1Vec &weights,
                                                   double &lowerBound,
                                                   double &upperBound,
                                                   maths_t::ETail &tail) const
{
    return m_Margin->probabilityOfLessLikelySamples(calculation,
                                                    weightStyles,
                                                    samples,
                                                    weights,
                                                    lowerBound, upperBound, tail);
}

double CCopulaMargin::numberSamples(void) const
{
    return m_Margin->numberSamples();
}

bool CCopulaMargin::isNonInformative(void) const
{
    return m_Margin->isNonInformative();
}

void CCopulaMargin::print(const std::string &indent, std::string &result) const
{
    m_Margin->print(indent, result);
}

uint64_t CCopulaMargin::checksum(uint64_t seed) const
{
    return m_Margin->checksum(seed);
}

}
}
