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

#ifndef INCLUDED_prelert_maths_Constants_h
#define INCLUDED_prelert_maths_Constants_h

#include <core/CSmallVector.h>

#include <maths/ImportExport.h>
#include <maths/MathsTypes.h>

namespace prelert
{
namespace maths
{

//! The minimum coefficient of variation supported by the models.
//! In general, if the coefficient of variation for the data becomes
//! too small we run into numerical problems in the analytics. So,
//! in addSamples we effectively add on variation in the data on the
//! order of this value. This is scale invariant since it includes
//! the sample mean. However, it means we are insensitive anomalous
//! deviations in data whose variation is significantly smaller than
//! this minimum value.
const double MINIMUM_COEFFICIENT_OF_VARIATION = 1e-4;

//! The largest probability for which an event is considered anomalous
//! enough to be worthwhile showing a user.
const double LARGEST_SIGNIFICANT_PROBABILITY = 0.05;

//! The largest probability that it is deemed significantly anomalous.
const double SMALL_PROBABILITY = 1e-4;

//! The largest probability that it is deemed extremely anomalous.
//! Probabilities smaller than this are only weakly discriminated
//! in the sense that they are given the correct order, but fairly
//! similar score.
const double MINUSCULE_PROBABILITY = 1e-50;

//! The minimum variance scale for which the likelihood function
//! can be accurately adjusted. For smaller scales there errors
//! are introduced for some priors.
const double MINIMUM_ACCURATE_VARIANCE_SCALE = 0.5;

//! The maximum variance scale for which the likelihood function
//! can be accurately adjusted. For larger scales there errors
//! are introduced for some priors.
const double MAXIMUM_ACCURATE_VARIANCE_SCALE = 2.0;

//! \brief A collection of weight styles and weights.
class MATHS_EXPORT CConstantWeights
{
    public:
        typedef core::CSmallVector<double, 4> TDouble4Vec;
        typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;

    public:
        //! A single count weight style.
        static const maths_t::TWeightStyleVec COUNT;
        //! A single count variance weight style.
        static const maths_t::TWeightStyleVec COUNT_VARIANCE;
        //! A single seasonal variance weight style.
        static const maths_t::TWeightStyleVec SEASONAL_VARIANCE;
        //! A unit weight.
        static const TDouble4Vec UNIT;
        //! An empty weight collection.
        static const TDouble4Vec1Vec EMPTY;
        //! A single unit weight.
        static const TDouble4Vec1Vec SINGLE_UNIT;
};

//! The minimum fractional count of points in a cluster.
const double MINIMUM_CLUSTER_SPLIT_FRACTION = 0.0;

//! The default minimum count of points in a cluster.
const double MINIMUM_CLUSTER_SPLIT_COUNT = 24.0;

//! The minimum count of a category in the sketch to cluster.
const double MINIMUM_CATEGORY_COUNT = 0.5;

//! Get the maximum amount we'll penalize a model in addSamples.
MATHS_EXPORT double maxModelPenalty(double numberSamples);

}
}

#endif // INCLUDED_prelert_maths_Constants_h
