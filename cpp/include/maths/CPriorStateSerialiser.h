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

#ifndef INCLUDED_prelert_maths_CPriorStateSerialiser_h
#define INCLUDED_prelert_maths_CPriorStateSerialiser_h

#include <core/CNonInstantiatable.h>

#include <maths/ImportExport.h>
#include <maths/MathsTypes.h>

#include <boost/shared_ptr.hpp>

namespace prelert
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace maths
{
class CMultivariatePrior;
class CPrior;
struct SDistributionRestoreParams;

//! \brief Convert CPrior sub-classes to/from text representations.
//!
//! DESCRIPTION:\n
//! Encapsulate the conversion of arbitrary CPrior sub-classes to/from
//! textual state.  In particular, the field name associated with each prior
//! distribution is then in one file.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The serialisation format must be a hierarchical format that supports
//! name/value pairs where the value may be a nested set of name/value
//! pairs.  Text format is used instead of binary because the format
//! needs to be storable in Splunk, Elasticsearch and potentially other
//! data stores that deal with text.  This also makes it easier to
//! provide backwards/forwards compatibility in the future as the classes
//! evolve.
//!
//! The field names given to each prior distribution class are deliberately
//! terse and uninformative to avoid giving away details of our analytics
//! to potential competitors.
//!
class MATHS_EXPORT CPriorStateSerialiser
{
    public:
        typedef boost::shared_ptr<CPrior> TPriorPtr;
        typedef boost::shared_ptr<CMultivariatePrior> TMultivariatePriorPtr;

    public:
        //! Construct the appropriate CPrior sub-class from its state
        //! document representation.  Sets \p ptr to NULL on failure.
        bool operator()(const SDistributionRestoreParams &params,
                        TPriorPtr &ptr,
                        core::CStateRestoreTraverser &traverser) const;

        //! Persist state by passing information to the supplied inserter
        void operator()(const CPrior &prior,
                        core::CStatePersistInserter &inserter) const;

        //! Construct the appropriate CMultivariatePrior sub-class from
        //! its state document representation.  Sets \p ptr to NULL on
        //! failure.
        bool operator()(const SDistributionRestoreParams &params,
                        TMultivariatePriorPtr &ptr,
                        core::CStateRestoreTraverser &traverser) const;

        //! Persist state by passing information to the supplied inserter
        void operator()(const CMultivariatePrior &prior,
                        core::CStatePersistInserter &inserter) const;
};

}
}

#endif // INCLUDED_prelert_maths_CPriorStateSerialiser_h

