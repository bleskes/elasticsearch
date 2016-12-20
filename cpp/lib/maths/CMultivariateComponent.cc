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

#include <maths/CMultivariateComponent.h>

#include <core/CoreTypes.h>

#include <maths/CPriorUtils.h>

namespace prelert
{
namespace maths
{

CMultivariateComponent::~CMultivariateComponent(void)
{
}

std::string CMultivariateComponent::print(void) const
{
    std::string result;
    this->print("", result);
    return result;
}

std::string CMultivariateComponent::printMarginalLikelihoodFunction(double weight) const
{
    // We'll plot the marginal likelihood function over a range
    // where most of the mass is.

    static const unsigned int POINTS = 501;

    CPrior::SPlot plot = this->marginalLikelihoodPlot(POINTS, weight);

    std::ostringstream abscissa;
    std::ostringstream likelihood;

    abscissa << "x = [";
    likelihood << "likelihood = [";
    for (unsigned int i = 0u; i < plot.s_Abscissa.size(); ++i)
    {
        abscissa << plot.s_Abscissa[i] << " ";
        likelihood << plot.s_Ordinates[i] << " ";
    }
    abscissa << "];" << core_t::LINE_ENDING;
    likelihood << "];" << core_t::LINE_ENDING << "plot(x, likelihood);";

    return abscissa.str() + likelihood.str();
}

CPrior::SPlot CMultivariateComponent::marginalLikelihoodPlot(unsigned int numberPoints,
                                                             double weight) const
{
    return CPriorUtils::marginalLikelihoodPlot(*this, numberPoints, weight);
}

}
}
