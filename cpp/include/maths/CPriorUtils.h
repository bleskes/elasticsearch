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

#ifndef INCLUDED_prelert_maths_CPriorUtils_h
#define INCLUDED_prelert_maths_CPriorUtils_h

#include <core/CNonInstantiatable.h>
#include <core/CSmallVector.h>

#include <maths/CPrior.h>
#include <maths/ImportExport.h>
#include <maths/MathsTypes.h>

namespace prelert
{
namespace maths
{

//! \brief Assorted utility functions and objects used by our priors.
class MATHS_EXPORT CPriorUtils : private core::CNonInstantiatable
{
    public:
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef std::vector<double> TDoubleVec;
        typedef maths_t::TWeightStyleVec TWeightStyleVec;
        typedef core::CSmallVector<double, 1> TDouble1Vec;
        typedef core::CSmallVector<double, 4> TDouble4Vec;
        typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;
        typedef CConstantWeights TWeights;

        //! \brief Wrapper around the jointLogMarginalLikelihood function.
        //!
        //! DESCRIPTION:\n
        //! This adapts the jointLogMarginalLikelihood function for use with
        //! CIntegration.
        template<typename PRIOR>
        class CLogMarginalLikelihood
        {
            public:
                typedef double result_type;

            public:
                 CLogMarginalLikelihood(const PRIOR &prior,
                                        const TWeightStyleVec &weightStyles = TWeights::COUNT,
                                        const TDouble4Vec1Vec &weights = TWeights::SINGLE_UNIT) :
                     m_Prior(&prior),
                     m_WeightStyles(&weightStyles),
                     m_Weights(&weights),
                     m_X(1u)
                 {
                 }

                 double operator()(double x) const
                 {
                     double result;
                     if (!this->operator()(x, result))
                     {
                         throw std::runtime_error("Unable to compute likelihood at "
                                                  + core::CStringUtils::typeToString(x));
                     }
                     return result;
                 }

                bool operator()(double x, double &result) const
                {
                    m_X[0] = x;
                    maths_t::EFloatingPointErrorStatus status =
                            m_Prior->jointLogMarginalLikelihood(*m_WeightStyles, m_X, *m_Weights, result);
                    return !(status & maths_t::E_FpFailed);
                }

            private:
                const PRIOR *m_Prior;
                const TWeightStyleVec *m_WeightStyles;
                const TDouble4Vec1Vec *m_Weights;
                //! Avoids creating the vector argument to jointLogMarginalLikelihood
                //! more than once.
                mutable TDouble1Vec m_X;
        };

        //! Return the plot data for the marginal likelihood function.
        template<typename PRIOR>
        static CPrior::SPlot marginalLikelihoodPlot(const PRIOR &prior,
                                                    unsigned int numberPoints,
                                                    double weight)
        {
            if (prior.isNonInformative())
            {
                // The non-informative likelihood is improper 0 everywhere.
                return CPrior::SPlot();
            }

            CPrior::SPlot plot;
            if (numberPoints == 0)
            {
                return plot;
            }

            plot.s_Abscissa.reserve(numberPoints);
            plot.s_Ordinates.reserve(numberPoints);
            prior.sampleMarginalLikelihood(numberPoints, plot.s_Abscissa);
            std::sort(plot.s_Abscissa.begin(), plot.s_Abscissa.end());

            TDouble1Vec x(1u, plot.s_Abscissa[0]);
            for (unsigned int i = 0u; i < numberPoints; ++i, x[0] = plot.s_Abscissa[i])
            {
                double likelihood;
                maths_t::EFloatingPointErrorStatus status =
                        prior.jointLogMarginalLikelihood(TWeights::COUNT, x, TWeights::SINGLE_UNIT, likelihood);
                if (status & maths_t::E_FpFailed)
                {
                    // Ignore point.
                }
                else if (status & maths_t::E_FpOverflowed)
                {
                    plot.s_Ordinates.push_back(0.0);
                }
                else
                {
                    plot.s_Ordinates.push_back(weight * ::exp(likelihood));
                }
            }

            return plot;
        }
};

}
}

#endif // INCLUDED_prelert_maths_CPriorUtils_h
