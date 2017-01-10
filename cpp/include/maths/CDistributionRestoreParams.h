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

#ifndef INCLUDED_ml_maths_CDistributionRestoreParams_h
#define INCLUDED_ml_maths_CDistributionRestoreParams_h

#include <maths/ImportExport.h>
#include <maths/MathsTypes.h>

namespace ml
{
namespace maths
{

//! \brief Gatherers up extra parameters supplied when restoring
//! distribution models.
struct MATHS_EXPORT SDistributionRestoreParams
{
    SDistributionRestoreParams(maths_t::EDataType dataType,
                               double decayRate,
                               double minimumClusterFraction,
                               double minimumClusterCount,
                               double minimumCategoryCount);

    //! The type of data being clustered.
    maths_t::EDataType s_DataType;

    //! The rate at which cluster priors decay to non-informative.
    double s_DecayRate;

    //! The minimum cluster fractional count.
    double s_MinimumClusterFraction;

    //! The minimum cluster count.
    double s_MinimumClusterCount;

    //! The minimum count for a category in the sketch to cluster.
    double s_MinimumCategoryCount;
};

}
}

#endif // INCLUDED_ml_maths_CDistributionRestoreParams_h
