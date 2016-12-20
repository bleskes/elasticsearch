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

#include <maths/CMultivariateComponentNormal.h>

namespace prelert
{
namespace maths
{

CMultivariateComponentNormal::CMultivariateComponentNormal(const CNormalMeanPrecConjugate &normal) :
        m_Normal(normal)
{
}

CMultivariateComponentNormal::TDoubleDoublePr
CMultivariateComponentNormal::marginalLikelihoodSupport(void) const
{
    return m_Normal.marginalLikelihoodSupport();
}

double CMultivariateComponentNormal::marginalLikelihoodMean(void) const
{
    return m_Normal.marginalLikelihoodMean();
}

double CMultivariateComponentNormal::marginalLikelihoodMode(const TWeightStyleVec &weightStyles,
                                                            const TDouble4Vec &weights) const
{
    return m_Normal.marginalLikelihoodMode(weightStyles, weights);
}

double CMultivariateComponentNormal::marginalLikelihoodVariance(const TWeightStyleVec &weightStyles,
                                                                const TDouble4Vec &weights) const
{
    return m_Normal.marginalLikelihoodVariance(weightStyles, weights);
}

CMultivariateComponentNormal::TDoubleDoublePr
CMultivariateComponentNormal::marginalLikelihoodConfidenceInterval(double percentage,
                                                                   const TWeightStyleVec &weightStyles,
                                                                   const TDouble4Vec &weights) const
{
    return m_Normal.marginalLikelihoodConfidenceInterval(percentage, weightStyles, weights);
}

maths_t::EFloatingPointErrorStatus
CMultivariateComponentNormal::jointLogMarginalLikelihood(const TWeightStyleVec &weightStyles,
                                                         const TDouble1Vec &samples,
                                                         const TDouble4Vec1Vec &weights,
                                                         double &result) const
{
    return m_Normal.jointLogMarginalLikelihood(weightStyles, samples, weights, result);
}

void CMultivariateComponentNormal::sampleMarginalLikelihood(std::size_t numberSamples,
                                                            TDouble1Vec &samples) const
{
    return m_Normal.sampleMarginalLikelihood(numberSamples, samples);
}

bool CMultivariateComponentNormal::minusLogJointCdf(const TWeightStyleVec &weightStyles,
                                                    const TDouble1Vec &samples,
                                                    const TDouble4Vec1Vec &weights,
                                                    double &lowerBound,
                                                    double &upperBound) const
{
    return m_Normal.minusLogJointCdf(weightStyles, samples, weights, lowerBound, upperBound);
}

bool CMultivariateComponentNormal::minusLogJointCdfComplement(const TWeightStyleVec &weightStyles,
                                                              const TDouble1Vec &samples,
                                                              const TDouble4Vec1Vec &weights,
                                                              double &lowerBound,
                                                              double &upperBound) const
{
    return m_Normal.minusLogJointCdfComplement(weightStyles, samples, weights, lowerBound, upperBound);
}

bool CMultivariateComponentNormal::probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
                                                                  const TWeightStyleVec &weightStyles,
                                                                  const TDouble1Vec &samples,
                                                                  const TDouble4Vec1Vec &weights,
                                                                  double &lowerBound,
                                                                  double &upperBound,
                                                                  maths_t::ETail &tail) const
{
    return m_Normal.probabilityOfLessLikelySamples(calculation,
                                                   weightStyles,
                                                   samples,
                                                   weights,
                                                   lowerBound, upperBound, tail);
}

double CMultivariateComponentNormal::numberSamples(void) const
{
    return m_Normal.numberSamples();
}

bool CMultivariateComponentNormal::isNonInformative(void) const
{
    return m_Normal.isNonInformative();
}

void CMultivariateComponentNormal::print(const std::string &indent, std::string &result) const
{
    m_Normal.print(indent, result);
}

uint64_t CMultivariateComponentNormal::checksum(uint64_t seed) const
{
    return m_Normal.checksum(seed);
}

}
}
