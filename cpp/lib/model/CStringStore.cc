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

#include <model/CStringStore.h>

#include <core/CLogger.h>
#include <core/CScopedFastLock.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CContainerPrinter.h>

#include <boost/bind.hpp>
#include <boost/make_shared.hpp>

namespace ml
{
namespace model
{

namespace
{

//! \brief Helper class to hash a std::string.
struct SStrHash
{
    std::size_t operator()(const std::string &key) const
    {
        boost::hash<std::string> hasher;
        return hasher(key);
    }
} STR_HASH;

//! \brief Helper class to compare a std::string and a TStrPtr.
struct SStrStrCPtrEqual
{
    typedef CStringStore::TStrPtr TStrPtr;

    bool operator()(const std::string &lhs, const TStrPtr &rhs) const
    {
        return lhs == *rhs;
    }
} STR_EQUAL;

// To ensure the singletons are constructed before multiple threads may
// require them call instance() during the static initialisation phase
// of the program.  Of course, the instance may already be constructed
// before this if another static object has used it.
const CStringStore &DO_NOT_USE_THIS_VARIABLE = CStringStore::names();
const CStringStore &DO_NOT_USE_THIS_VARIABLE_EITHER = CStringStore::influencers();

}

void CStringStore::tidyUpNotThreadSafe(void)
{
    names().pruneRemovedNotThreadSafe();
    influencers().pruneNotThreadSafe();
}

CStringStore &CStringStore::names(void)
{
    static CStringStore namesInstance;
    return namesInstance;
}

CStringStore &CStringStore::influencers(void)
{
    static CStringStore influencersInstance;
    return influencersInstance;
}

const CStringStore::TStrPtr &CStringStore::getEmpty(void) const
{
    return m_EmptyString;
}

CStringStore::TStrPtr CStringStore::get(const std::string &value)
{
    // This section is expected to be performed frequently.
    //
    // We ensure either:
    //   1) Some threads may perform an insert and no thread will perform
    //      a find until no threads can still perform an insert.
    //   2) Some threads may perform a find and no thread will perform
    //      an insert until no thread can still perform a find.
    //
    // We "leak" strings if there is contention between reading and writing,
    // which is expected to be rare because inserts are expected to be rare.

    if (value.empty())
    {
        return m_EmptyString;
    }

    TStrPtr result;

    m_Reading.fetch_add(1, atomic_t::memory_order_release);
    // Using fetch_add(0) rather than load() due to Solaris Studio 12.5 bug.
    // (Timings on other platforms show this doesn't have a major detrimental
    // effect on performance.)
    if (m_Writing.fetch_add(0, atomic_t::memory_order_consume) == 0)
    {
        TStrPtrUSetCItr i = m_Strings.find(value, STR_HASH, STR_EQUAL);
        if (i != m_Strings.end())
        {
            result = *i;
            m_Reading.fetch_sub(1, atomic_t::memory_order_release);
        }
        else
        {
            m_Writing.fetch_add(1, atomic_t::memory_order_acq_rel);
            // NB: fetch_sub() returns the OLD value, and we know we added 1 in
            // this thread, hence the test for 1 rather than 0
            if (m_Reading.fetch_sub(1, atomic_t::memory_order_release) == 1)
            {
                // This section is expected to occur infrequently so inserts
                // are synchronized with a mutex.
                core::CScopedFastLock lock(m_Mutex);
                result = *m_Strings.insert(boost::make_shared<const std::string>(value)).first;
                m_Writing.fetch_sub(1, atomic_t::memory_order_release);
            }
            else
            {
                m_Writing.fetch_sub(1, atomic_t::memory_order_relaxed);
                result = boost::make_shared<const std::string>(value);
            }
        }
    }
    else
    {
        m_Reading.fetch_sub(1, atomic_t::memory_order_relaxed);
        result = boost::make_shared<const std::string>(value);
    }

    return result;
}

void CStringStore::remove(const std::string &value)
{
    core::CScopedFastLock lock(m_Mutex);
    m_Removed.push_back(value);
}

void CStringStore::pruneRemovedNotThreadSafe(void)
{
    core::CScopedFastLock lock(m_Mutex);
    for (std::size_t i = 0u; i < m_Removed.size(); ++i)
    {
        TStrPtrUSetItr j = m_Strings.find(m_Removed[i], STR_HASH, STR_EQUAL);
        if (j != m_Strings.end() && j->unique())
        {
            m_Strings.erase(j);
        }
    }
    m_Removed.clear();
}

void CStringStore::pruneNotThreadSafe(void)
{
    core::CScopedFastLock lock(m_Mutex);
    for (TStrPtrUSetItr i = m_Strings.begin(); i != m_Strings.end(); /**/)
    {
        if (i->unique())
        {
            i = m_Strings.erase(i);
        }
        else
        {
            ++i;
        }
    }
}

void CStringStore::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem)
{
    mem->setName("StringStore");
    core::CMemoryDebug::dynamicSize("influencers", CStringStore::influencers().m_Strings, mem);
    core::CMemoryDebug::dynamicSize("names", CStringStore::names().m_Strings, mem);
}

std::size_t CStringStore::memoryUsage(void)
{
    std::size_t mem = core::CMemory::dynamicSize(CStringStore::influencers().m_Strings);
    mem += core::CMemory::dynamicSize(CStringStore::names().m_Strings);
    return mem;
}

CStringStore::CStringStore(void) :
        m_Reading(0), m_Writing(0), m_EmptyString(boost::make_shared<const std::string>())
{
}

void CStringStore::clearEverythingTestOnly(void)
{
    m_Strings.clear();
    m_Removed.clear();
}

} // model
} // ml

