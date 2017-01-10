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

#ifndef INCLUDED_ml_maths_CMultivariateComponentMixture_h
#define INCLUDED_ml_maths_CMultivariateComponentMixture_h

#include <core/CPolymorphicStackObjectCPtr.h>

#include <maths/CCopulaMargin.h>
//#include <maths/CCopulaConditional.h>
#include <maths/CMultimodalPriorMode.h>
#include <maths/CMultivariateComponent.h>
#include <maths/CMultivariateComponentNormal.h>
#include <maths/ImportExport.h>

#include <vector>

namespace ml
{
namespace maths
{

//! \brief A mixture of multivariate component distributions.
//!
//! DESCRIPTION:\n
//! TODO
class MATHS_EXPORT CMultivariateComponentMixture : public CMultivariateComponent
{
    public:
        // TODO
        typedef core::CPolymorphicStackObjectCPtr<CMultivariateComponent,
                                                  CMultivariateComponentNormal,
                                                  CCopulaMargin/*,
                                                  CCopulaConditional*/> TComponentCPtr;
        typedef std::vector<TComponentCPtr> TComponentCPtrVec;

    public:
        CMultivariateComponentMixture(double numberSamples, const TComponentCPtrVec &modes);

        //! \name Component Contract
        //@{
        //! Get the support for the marginal likelihood function.
        virtual TDoubleDoublePr marginalLikelihoodSupport(void) const;

        //! Get the mean of the marginal likelihood function.
        virtual double marginalLikelihoodMean(void) const;

        //! Get the mode of the marginal likelihood function.
        virtual double marginalLikelihoodMode(const TWeightStyleVec &weightStyles = TWeights::COUNT_VARIANCE,
                                              const TDouble4Vec &weights = TWeights::UNIT) const;

        //! Get the variance of the marginal likelihood.
        virtual double marginalLikelihoodVariance(const TWeightStyleVec &weightStyles = TWeights::COUNT_VARIANCE,
                                                  const TDouble4Vec &weights = TWeights::UNIT) const;

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
                                                 const TDouble4Vec &weights = TWeights::UNIT) const;

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
                                       double &result) const;

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
                                              TDouble1Vec &samples) const;

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
                                      double &upperBound) const;

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
                                                double &upperBound) const;

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
                                                    maths_t::ETail &tail) const;

        //! Check if this is a non-informative prior.
        virtual bool isNonInformative(void) const;

        //! Get the number of samples received to date.
        virtual double numberSamples(void) const;

        //! Get a human readable description of the prior.
        //!
        //! \param[in] indent The indent to use at the start of new lines.
        //! \param[in,out] result Filled in with the description.
        virtual void print(const std::string &indent, std::string &result) const;

        //! Get a checksum for this object.
        virtual uint64_t checksum(uint64_t seed = 0) const;
        //@}

    private:
        typedef SMultimodalPriorMode<TComponentCPtr> TMode;
        typedef std::vector<TMode> TModeVec;

    private:
        //! The number of samples which have been added to the distribution.
        double m_NumberSamples;

        //! The modes of the distribution.
        TModeVec m_Modes;
};

}
}

#endif // INCLUDED_ml_maths_CMultivariateComponentMixture_h
