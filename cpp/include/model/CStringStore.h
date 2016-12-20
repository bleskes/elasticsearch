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
#ifndef INCLUDED_prelert_model_CStringStore_h
#define INCLUDED_prelert_model_CStringStore_h

#include <core/AtomicTypes.h>
#include <core/CMemory.h>
#include <core/CFastMutex.h>
#include <core/CNonCopyable.h>

#include <model/ImportExport.h>

#include <functional>
#include <string>

#include <boost/shared_ptr.hpp>
#include <boost/unordered_set.hpp>

class CStringStoreTest;

namespace prelert
{

namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}

namespace model
{

//! \brief
//! DESCRIPTION:\n
//!
//! A collection of person/attribute strings, held by shared_ptr, and an
//! independent collection of influencer strings (which have a different
//! life-cycle).
//! Users of the class look up a string by "const std::string &", and
//! receive a boost::shared_ptr\<std::string\> in its stead.
//!
//! Specific person/attribute strings will be able to be pruned, if their
//! reference count is only 1 (i.e. we hold the only reference).
//!
//! IMPLEMENTATION DECISIONS:\n
//! A singleton class: there should only be one collection strings for
//! person names/attributes, and a separate collection for influencer
//! strings.
//! Write access is locked for the benefit of future threading.
//!
class MODEL_EXPORT CStringStore : private core::CNonCopyable
{
    public:
        typedef boost::shared_ptr<const std::string> TStrPtr;
        struct MODEL_EXPORT SHashStrPtr
        {
            std::size_t operator()(const TStrPtr &key) const
            {
                boost::hash<std::string> hasher;
                return hasher(*key);
            }
        };
        struct MODEL_EXPORT SStrPtrEqual
        {
            bool operator()(const TStrPtr &lhs, const TStrPtr &rhs) const
            {
                return *lhs == *rhs;
            }
        };
        typedef boost::unordered_set<TStrPtr, SHashStrPtr, SStrPtrEqual> TStrPtrUSet;
        typedef TStrPtrUSet::iterator TStrPtrUSetItr;
        typedef TStrPtrUSet::const_iterator TStrPtrUSetCItr;

    public:
        //! Call this to tidy up any strings no longer needed.
        static void tidyUpNotThreadSafe(void);

        //! Singleton pattern for person/attribute names.
        static CStringStore &names(void);

        //! Singleton pattern for influencer names.
        static CStringStore &influencers(void);

        //! Fast method to get the pointer for an empty string.
        const TStrPtr &getEmpty(void) const;

        //! (Possibly) add \p value to the store and get back a pointer to it.
        TStrPtr get(const std::string &value);

        //! (Possibly) remove \p value from the store.
        void remove(const std::string &value);

        //! Prune strings which have been removed.
        void pruneRemovedNotThreadSafe(void);

        //! Iterate over the string store and remove unused entries.
        void pruneNotThreadSafe(void);

        //! Get the memory used by this component
        static void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem);

        //! Get the memory used by this component
        static std::size_t memoryUsage(void);

    private:
        typedef std::vector<std::string> TStrVec;

    private:
        //! Constructor of a Singleton is private.
        CStringStore(void);

        //! Bludgeoning device to delete all objects in store.
        void clearEverythingTestOnly(void);

        //! The unique instance for person/attribute names.
        static CStringStore ms_NamesInstance;

        //! The unique instance for influencer names.
        static CStringStore ms_InfluencersInstance;

    private:
        //! Fence for reading operations (in which case we "leak" a string
        //! if we try to write at the same time). See get for details.
        atomic_t::atomic_int m_Reading;

        //! Fence for writing operations (in which case we "leak" a string
        //! if we try to read at the same time). See get for details.
        atomic_t::atomic_int m_Writing;

        //! The empty string is often used so we store it outside the set.
        TStrPtr m_EmptyString;

        //! Set to keep the person/attribute string pointers
        TStrPtrUSet m_Strings;

        //! A list of the strings to remove.
        TStrVec m_Removed;

        //! Locking primitive
        core::CFastMutex m_Mutex;

        friend class ::CStringStoreTest;
};


} // model
} // prelert

#endif // INCLUDED_prelert_model_CStringStore_h
