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

#include <maths/CTimeSeriesDecompositionStateSerialiser.h>

#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>

#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTimeSeriesDecompositionStub.h>

#include <boost/bind.hpp>

#include <string>
#include <typeinfo>


namespace prelert
{
namespace maths
{

namespace
{

// We obfuscate the field names to avoid giving away too much
// information about our model. There needs to be one constant
// here per sub-class of CTimeSeriesDecompositionInterface.
// DO NOT change the existing tags if new sub-classes are added.
const std::string TIME_SERIES_DECOMPOSITION_TAG("a");
const std::string TIME_SERIES_DECOMPOSITION_STUB_TAG("b");

const std::string EMPTY_STRING;

}

bool CTimeSeriesDecompositionStateSerialiser::acceptRestoreTraverser(double decayRate,
                                                                     core_t::TTime minimumBucketLength,
                                                                     std::size_t componentSize,
                                                                     TDecompositionPtr &result,
                                                                     core::CStateRestoreTraverser &traverser)
{
    std::size_t numResults = 0;

    do
    {
        const std::string &name = traverser.name();
        if (name == TIME_SERIES_DECOMPOSITION_TAG)
        {
            result.reset(new CTimeSeriesDecomposition(decayRate,
                                                      minimumBucketLength,
                                                      componentSize,
                                                      traverser));
            ++numResults;
        }
        else if (name == TIME_SERIES_DECOMPOSITION_STUB_TAG)
        {
            result.reset(new CTimeSeriesDecompositionStub());
            ++numResults;
        }
        else
        {
            // Due to the way we divide large state into multiple
            // Splunk events this is not necessarily a problem -
            // the unexpected element may be marking the start of
            // a new chunk.
            LOG_WARN("No decomposition corresponds to name " << traverser.name());
        }
    }
    while (traverser.next());

    if (numResults != 1)
    {
        LOG_ERROR("Expected 1 (got " << numResults << ") decomposition tags");
        result.reset();
        return false;
    }

    return true;
}

void CTimeSeriesDecompositionStateSerialiser::acceptPersistInserter(const CTimeSeriesDecompositionInterface &decomposition,
                                                                    core::CStatePersistInserter &inserter)
{
    if (dynamic_cast<const CTimeSeriesDecomposition*>(&decomposition) != 0)
    {
        inserter.insertLevel(TIME_SERIES_DECOMPOSITION_TAG,
                             boost::bind(&CTimeSeriesDecomposition::acceptPersistInserter,
                                         dynamic_cast<const CTimeSeriesDecomposition*>(&decomposition),
                                         _1));
    }
    else if (dynamic_cast<const CTimeSeriesDecompositionStub*>(&decomposition) != 0)
    {
        inserter.insertValue(TIME_SERIES_DECOMPOSITION_STUB_TAG, "");
    }
    else
    {
        LOG_ERROR("Decomposition with type " << typeid(decomposition).name()
                  << " has no defined name");
    }

}

}
}

