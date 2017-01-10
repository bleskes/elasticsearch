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

#ifndef INCLUDED_ml_maths_CMultivariateComponent_h
#define INCLUDED_ml_maths_CMultivariateComponent_h

#include <core/CSmallVector.h>

#include <maths/Constants.h>
#include <maths/CPrior.h>
#include <maths/ImportExport.h>

#include <cstddef>
#include <string>
#include <utility>

namespace ml
{
namespace maths
{

//! \brief Interface of multivariate component distributions.
//!
//! DESCRIPTION:\n
//! This defines an interface used for dealing with aspects of the
//! multivariate distribution modeling. In particular, we support
//! multivariate Gaussian, multivariate mixture distributions and
//! bivariate copula models for describing data. In order, to compute
//! probabilities, Winsorise outliers, etc, for these distributions
//! we need extract univariate distribution descriptions obtained by
//! marginalizing or conditioning on subsets of the variables. This
//! effectively defines the minimum subset of the CPrior interface
//! needed for these purposes.
class MATHS_EXPORT CMultivariateComponent
{
    public:
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef maths_t::TWeightStyleVec TWeightStyleVec;
        typedef core::CSmallVector<double, 1> TDouble1Vec;
        typedef core::CSmallVector<double, 4> TDouble4Vec;
        typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;
        typedef CConstantWeights TWeights;

    public:
        virtual ~CMultivariateComponent(void);

        //! \name Component Contract
        //@{
        //! Get the support for the marginal likelihood function.
        virtual TDoubleDoublePr marginalLikelihoodSupport(void) const = 0;

        //! Get the mean of the marginal likelihood function.
        virtual double marginalLikelihoodMean(void) const = 0;

        //! Get the mode of the marginal likelihood function.
        virtual double marginalLikelihoodMode(const TWeightStyleVec &weightStyles = TWeights::COUNT_VARIANCE,
                                              const TDouble4Vec &weights = TWeights::UNIT) const = 0;

        //! Get the variance of the marginal likelihood.
        virtual double marginalLikelihoodVariance(const TWeightStyleVec &weightStyles = TWeights::COUNT_VARIANCE,
                                                  const TDouble4Vec &weights = TWeights::UNIT) const = 0;

        //! Get the \p percentage symmetric confidence interval for the marginal
        //! likelihood function, i.e. the values \f$a\f$ and \f$b\f$ such that:
        //! <pre class="fragment">
        //!   \f$P([a,m]) = P([m,b]) = p / 100 / 2\f$
        //! </pre>
        //!
        //! where \f$m\f$ is the median of the distribution and \f$p\f$ is the
        //! the percentage of interest \p percentage.
        //!
        //! \param[in] percentage The percentage of interest.
        //! \param[in] weightStyles Optional variance scale weight styles.
        //! \param[in] weights Optional variance scale weights.
        //! \note \p percentage should be in the range [0.0, 100.0).
        virtual TDoubleDoublePr
            marginalLikelihoodConfidenceInterval(double percentage,
                                                 const TWeightStyleVec &weightStyles = TWeights::COUNT_VARIANCE,
                                                 const TDouble4Vec &weights = TWeights::UNIT) const = 0;

        //! Calculate the log marginal likelihood function integrating over
        //! the prior density function.
        //!
        //! \param[in] weightStyles Controls the interpretation of the weight(s)
        //! that are associated with each sample. See maths_t::ESampleWeightStyle
        //! for more details.
        //! \param[in] samples A collection of samples of the variable.
        //! \param[in] weights The weights of each sample in \p samples.
        //! \param[out] result Filled in with the joint likelihood of \p samples.
        virtual maths_t::EFloatingPointErrorStatus
            jointLogMarginalLikelihood(const TWeightStyleVec &weightStyles,
                                       const TDouble1Vec &samples,
                                       const TDouble4Vec1Vec &weights,
                                       double &result) const = 0;

        //! Sample the marginal likelihood function.
        //!
        //! The marginal likelihood functions are sampled in quantile
        //! intervals. The idea is to capture a set of samples that
        //! accurately and efficiently represent the information in the
        //! prior. Random sampling (although it has nice asymptotic properties)
        //! doesn't fulfill the second requirement: typically requiring
        //! many more samples than sampling in quantile intervals to
        //! capture the same amount of information.
        //!
        //! This is to allow us to transform one prior distribution into
        //! another completely generically and relatively efficiently, by
        //! updating the target prior with these samples. As such the prior
        //! needs to maintain a count of the number of samples to date
        //! so that it isn't over sampled.
        //!
        //! \param[in] numberSamples The number of samples required.
        //! \param[out] samples Filled in with samples from the prior.
        //! \note \p numberSamples is truncated to the number of samples received.
        virtual void sampleMarginalLikelihood(std::size_t numberSamples,
                                              TDouble1Vec &samples) const = 0;

        //! Calculate minus the log of the joint c.d.f. of the marginal likelihood
        //! for a collection of independent samples from the variable.
        //!
        //! \param[in] weightStyles Controls the interpretation of the weights
        //! that are associated with each sample. See maths_t::ESampleWeightStyle
        //! for more details.
        //! \param[in] samples A collection of samples of the process.
        //! \param[in] weights The weights of each sample in \p samples.
        //! \param[out] lowerBound Filled in with a lower bound for
        //! \f$-\log(\prod_i{F(x_i)})\f$ where \f$F(.)\f$ is the c.d.f. and
        //! \f$\{x_i\}\f$ are the samples.
        //! \param[out] upperBound Filled in with a upper bound for
        //! \f$-\log(\prod_i{F(x_i)})\f$ where \f$F(.)\f$ is the c.d.f. and
        //! \f$\{x_i\}\f$ are the samples.
        //! \note The samples are assumed to be independent.
        //! \note In general, \p lowerBound equals \p upperBound. However,
        //! in some cases insufficient data is held to exactly compute the
        //! c.d.f. in which case the we use sharp upper and lower bounds.
        //! \warning The variance scales must be in the range \f$(0,\infty)\f$,
        //! i.e. a value of zero is not well defined and a value of infinity
        //! is not well handled. (Very large values are handled though.)
        virtual bool minusLogJointCdf(const TWeightStyleVec &weightStyles,
                                      const TDouble1Vec &samples,
                                      const TDouble4Vec1Vec &weights,
                                      double &lowerBound,
                                      double &upperBound) const = 0;

        //! Compute minus the log of the one minus the joint c.d.f. of the
        //! marginal likelihood at \p samples without losing precision due to
        //! cancellation errors at one, i.e. the smallest non-zero value this
        //! can return is the minimum double rather than epsilon.
        //!
        //! \see minusLogJointCdf for more details.
        virtual bool minusLogJointCdfComplement(const TWeightStyleVec &weightStyles,
                                                const TDouble1Vec &samples,
                                                const TDouble4Vec1Vec &weights,
                                                double &lowerBound,
                                                double &upperBound) const = 0;

        //! Calculate the joint probability of seeing a lower likelihood collection
        //! of independent samples from the variable integrating over the prior
        //! density function.
        //!
        //! \param[in] calculation The style of the probability calculation
        //! (see model_t::EProbabilityCalculation for details).
        //! \param[in] weightStyles Controls the interpretation of the weights
        //! that are associated with each sample. See maths_t::ESampleWeightStyle
        //! for more details.
        //! \param[in] samples A collection of samples of the process.
        //! \param[in] weights The weights of each sample in \p samples.
        //! \param[out] lowerBound Filled in with a lower bound for the probability
        //! of the set for which the joint marginal likelihood is less than
        //! that of \p samples (subject to the measure \p calculation).
        //! \param[out] upperBound Filled in with an upper bound for the
        //! probability of the set for which the joint marginal likelihood is
        //! less than that of \p samples (subject to the measure \p calculation).
        //! \param[out] tail The tail that (left or right) that all the
        //! samples are in or neither.
        //! \note The samples are assumed to be independent.
        //! \note In general, \p lowerBound equals \p upperBound. However,
        //! in some cases insufficient data is held to exactly compute the
        //! c.d.f. in which case the we use sharp upper and lower bounds.
        //! \warning The variance scales must be in the range \f$(0,\infty)\f$,
        //! i.e. a value of zero is not well defined and a value of infinity
        //! is not well handled. (Very large values are handled though.)
        virtual bool probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
                                                    const TWeightStyleVec &weightStyles,
                                                    const TDouble1Vec &samples,
                                                    const TDouble4Vec1Vec &weights,
                                                    double &lowerBound,
                                                    double &upperBound,
                                                    maths_t::ETail &tail) const = 0;

        //! Check if this is a non-informative prior.
        virtual bool isNonInformative(void) const = 0;

        //! Get the number of samples received to date.
        virtual double numberSamples(void) const = 0;

        //! Get a human readable description of the prior.
        std::string print(void) const;

        //! Get a human readable description of the prior.
        //!
        //! \param[in] indent The indent to use at the start of new lines.
        //! \param[in,out] result Filled in with the description.
        virtual void print(const std::string &indent, std::string &result) const = 0;

        //! Print the marginal likelihood function.
        //!
        //! The format is as follows:\n
        //! \code{cpp}
        //!   x = [x1 x2 .... xn ];
        //!   likelihood = [L(x1) L(x2) ... L(xn) ];
        //! \endcode
        //!
        //! i.e. domain values are space separated on the first line and the likelihood
        //! evaluated at those values are space separated on the next line.
        std::string printMarginalLikelihoodFunction(double weight = 1.0) const;

        //! Return the plot data for the marginal likelihood function.
        //!
        //! \param[in] numberPoints Number of points to use in the returned plot.
        //! \param[in] weight A scale which is applied to all likelihoods.
        CPrior::SPlot marginalLikelihoodPlot(unsigned int numberPoints,
                                             double weight = 1.0) const;

        //! Get a checksum for this object.
        virtual uint64_t checksum(uint64_t seed = 0) const = 0;
        //@}
};

}
}

#endif // INCLUDED_ml_maths_CMultivariateComponent_h
