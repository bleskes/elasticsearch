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

#ifndef INCLUDED_prelert_model_CFeatureData_h
#define INCLUDED_prelert_model_CFeatureData_h

#include <core/CMemoryUsage.h>
#include <core/CoreTypes.h>
#include <core/CSmallVector.h>

#include <model/CSample.h>
#include <model/ImportExport.h>

#include <boost/optional.hpp>
#include <boost/ref.hpp>

#include <cstddef>
#include <string>
#include <utility>
#include <vector>

namespace prelert
{
namespace model
{

//! \brief Manages the indexing for the feature values in the statistics
//! vectors passed from data gatherers to the model classes.
class MODEL_EXPORT CFeatureDataIndexing
{
    public:
        typedef std::vector<std::size_t> TSizeVec;

    public:
        //! Get the indices of the actual feature value(s) in the feature
        //! data vector.
        static const TSizeVec &valueIndices(std::size_t dimension);
};

//! \brief The data for an event rate series feature.
struct MODEL_EXPORT SEventRateFeatureData
{
    typedef core::CSmallVector<double, 1> TDouble1Vec;
    typedef boost::reference_wrapper<const std::string> TStrCRef;
    typedef std::pair<TDouble1Vec, double> TDouble1VecDoublePr;
    typedef std::pair<TStrCRef, TDouble1VecDoublePr> TStrCRefDouble1VecDoublePrPr;
    typedef std::vector<TStrCRefDouble1VecDoublePrPr> TStrCRefDouble1VecDoublePrPrVec;
    typedef std::vector<TStrCRefDouble1VecDoublePrPrVec> TStrCRefDouble1VecDoublePrPrVecVec;

    SEventRateFeatureData(uint64_t count);

    //! Efficiently swap the contents of this and \p other.
    void swap(SEventRateFeatureData &other);

    //! Print the data for debug.
    std::string print(void) const;

    //! Get the memory usage of this component.
    std::size_t memoryUsage(void) const;

    //! Get the memory usage of this component in a tree structure.
    void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

    uint64_t s_Count;
    TStrCRefDouble1VecDoublePrPrVecVec s_InfluenceValues;
};

//! \brief The data for a metric series feature.
struct MODEL_EXPORT SMetricFeatureData
{
    typedef core::CSmallVector<double, 1> TDouble1Vec;
    typedef boost::optional<CSample> TOptionalSample;
    typedef std::vector<CSample> TSampleVec;
    typedef boost::reference_wrapper<const std::string> TStrCRef;
    typedef std::pair<TDouble1Vec, double> TDouble1VecDoublePr;
    typedef std::pair<TStrCRef, TDouble1VecDoublePr> TStrCRefDouble1VecDoublePrPr;
    typedef std::vector<TStrCRefDouble1VecDoublePrPr> TStrCRefDouble1VecDoublePrPrVec;
    typedef std::vector<TStrCRefDouble1VecDoublePrPrVec> TStrCRefDouble1VecDoublePrPrVecVec;

    SMetricFeatureData(core_t::TTime bucketTime,
                       const TDouble1Vec &bucketValue,
                       double bucketVarianceScale,
                       double bucketCount,
                       TStrCRefDouble1VecDoublePrPrVecVec &influenceValues,
                       bool isInteger,
                       const TSampleVec &samples);

    SMetricFeatureData(bool isInteger,
                       const TSampleVec &samples);

    //! Print the data for debug.
    std::string print(void) const;

    //! Get the memory usage of this component.
    std::size_t memoryUsage(void) const;

    //! Get the memory usage of this component in a tree structure.
    void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

    //! The bucket value.
    TOptionalSample s_BucketValue;
    //! The influencing values and counts.
    TStrCRefDouble1VecDoublePrPrVecVec s_InfluenceValues;
    //! True if all the samples are integer.
    bool s_IsInteger;
    //! The samples.
    TSampleVec s_Samples;
};

}
}

#endif // INCLUDED_prelert_model_CFeatureData_h
