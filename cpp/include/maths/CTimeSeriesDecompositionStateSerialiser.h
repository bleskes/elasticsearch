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

#ifndef INCLUDED_ml_maths_CTimeSeriesDecompositionStateSerialiser_h
#define INCLUDED_ml_maths_CTimeSeriesDecompositionStateSerialiser_h

#include <core/CNonInstantiatable.h>
#include <core/CoreTypes.h>

#include <maths/ImportExport.h>
#include <maths/MathsTypes.h>

#include <boost/shared_ptr.hpp>


namespace ml
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace maths
{
class CTimeSeriesDecompositionInterface;

//! \brief Convert CTimeSeriesDecompositionInterface sub-classes to/from
//! text representations.
//!
//! DESCRIPTION:\n
//! Encapsulate the conversion of arbitrary CTimeSeriesDecompositionInterface
//! sub-classes to/from textual state. In particular, the field name associated
//! with each type of decomposition is then in one file.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The serialisation format must be a hierarchical format that supports
//! name/value pairs where the value may be a nested set of name/value
//! pairs. Text format is used instead of binary because the format needs
//! to be storable in Splunk, Elasticsearch and potentially other data stores
//! that deal with text. This also makes it easier to provide backwards/
//! forwards compatibility in the future as the classes evolve.
//!
//! The field names given to each decomposition class are deliberately
//! terse and uninformative to avoid giving away details of our analytics
//! to potential competitors.
class MATHS_EXPORT CTimeSeriesDecompositionStateSerialiser : private core::CNonInstantiatable
{
    public:
        //! Shared pointer to the CTimeSeriesDecompositionInterface abstract
        //! base class.
        typedef boost::shared_ptr<CTimeSeriesDecompositionInterface> TDecompositionPtr;

    public:
        //! Construct the appropriate CTimeSeriesDecompositionInterface
        //! sub-class from its state document representation. Sets \p result
        //! to NULL on failure.
        static bool acceptRestoreTraverser(double decayRate,
                                           core_t::TTime minimumBucketLength,
                                           std::size_t componentSize,
                                           TDecompositionPtr &result,
                                           core::CStateRestoreTraverser &traverser);

        //! Persist state by passing information to the supplied inserter.
        static void acceptPersistInserter(const CTimeSeriesDecompositionInterface &decomposition,
                                          core::CStatePersistInserter &inserter);
};


}
}

#endif // INCLUDED_ml_maths_CTimeSeriesDecompositionStateSerialiser_h

