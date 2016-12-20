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

#ifndef INCLUDED_prelert_maths_CCounter_h
#define INCLUDED_prelert_maths_CCounter_h

#include <core/CoreTypes.h>

#include <maths/ImportExport.h>

#include <boost/circular_buffer.hpp>
#include <boost/shared_ptr.hpp>

#include <stdint.h>


namespace prelert
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace maths
{

//! \brief A very lightweight counter of events per specified time interval.
//!
//! DESCRIPTION:\n
//! The idea of this class is to count the arrival of events in specified
//! time intervals starting at a specified time, i.e.\n
//! <pre class="fragment">
//!   \f$[t0 + n \times l, t0 + (n+1) \times l)\f$
//! </pre>
//!
//! where,\n
//!   \f$t0\f$ is \p startTime supplied to the constructor,\n
//!   \f$l\f$ is \p bucketLength supplied to the constructor and\n
//!   \f$n = 0, 1, ...\f$
//!
//! Note, that the intervals are closed below and open above. This class also
//! computes a mean rate for the bucket count and maintains a count of the
//! number of non-empty buckets.
//!
//! It is possible to configure the counter to remember a fixed length history
//! of the current bucket counts by providing a buffer to the constructor.
//!
//! IMPLEMENTATION DECISIONS:\n
//! In order to make this as fast and lightweight as possible it assumes that
//! events are received in time order and it only remembers the current bucket
//! count unless history is specifically requested. The mean can be computed
//! online provided the number of buckets observed to date is known. As such
//! it only needs one double (the mean count) and four integers: the number of
//! bins, the current count and the two times. The count of non-empty buckets
//! is also stored (as an integer).
//!
//! The history is held by pointer so that the user does not pay the penalty
//! of 56 bytes (for a boost::circular_buffer object) if they do not need any
//! history.
class MATHS_EXPORT CCounter
{
    public:
        //! The statistics we remember for each bucket in the history.
        struct SBucket
        {
            SBucket(double meanCountPreceding,
                    uint64_t numberBucketsPreceding,
                    uint64_t numberNonEmptyBucketsPreceding,
                    uint64_t currentCount);

            double s_MeanCountPreceding;
            uint64_t s_NumberBucketsPreceding;
            uint64_t s_NumberNonEmptyBucketsPreceding;
            uint64_t s_CurrentCount;
        };

        typedef boost::circular_buffer<SBucket> TBucketCirBuf;
        typedef TBucketCirBuf::iterator TBucketCirBufItr;
        typedef TBucketCirBuf::const_iterator TBucketCirBufCItr;
        typedef TBucketCirBuf::const_reverse_iterator TBucketCirBufCRItr;
        typedef boost::shared_ptr<TBucketCirBuf> TBucketCirBufPtr;

    public:
        //! \param startTime The start of the period being observed.
        //! \param bucketLength The bucketing time interval.
        //! \param history Optional circular buffer used for recording most
        //! recent bucket counts.
        CCounter(const core_t::TTime &startTime,
                 const core_t::TTime &bucketLength,
                 const TBucketCirBufPtr history = TBucketCirBufPtr());

        //! Construct by traversing a state document
        CCounter(core::CStateRestoreTraverser &traverser);

        //! Construct with some history by traversing a state document
        CCounter(const TBucketCirBufPtr history,
                 core::CStateRestoreTraverser &traverser);

        //! \name Value Semantics
        //@{
        CCounter(const CCounter &other);
        CCounter &operator=(const CCounter &rhs);
        //@}

        //! Get the current bucket start time.
        core_t::TTime bucketStart(void) const;

        //! Get the current bucket end time.
        core_t::TTime bucketEnd(void) const;

        //! Get the bucketing time interval.
        core_t::TTime bucketLength(void) const;

        //! Check if counts are available at the specified time.
        bool countsAvailable(core_t::TTime time) const;

        //! Update the counts with the arrival of an event at \p time.
        void addArrival(core_t::TTime time);

        //! Update time now to \p time.
        void timeNow(core_t::TTime time);

        //! Get the baseline count at \p time.
        double meanBucketCount(core_t::TTime time) const;

        //! Get the current bucket time at \p time.
        uint64_t currentBucketCount(core_t::TTime time) const;

        //! Get the number of buckets with a count of zero.
        uint64_t numberNonEmptyBuckets(core_t::TTime time) const;

        //! Get the total number of buckets.
        uint64_t numberBuckets(core_t::TTime time) const;

        //! Compute the number of full buckets which have elapsed at \p time.
        uint64_t computeElapsedBuckets(core_t::TTime time) const;

        //! Persist state by passing information to the supplied inserter
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

    private:
        //! Persist state for a history bucket by passing information to the
        //! supplied inserter
        static void historyBucketAcceptPersistInserter(const SBucket &bucket,
                                                       core::CStatePersistInserter &inserter);

        //! Restore the member values from an XML node hierarchy.
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Restore the member values from an XML node hierarchy.
        bool historyAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Create a new bucket for \p time and update counts.
        void startNewBucket(core_t::TTime time);

        //! Compute the baseline count at \p time.
        double computeMeanBucketCount(core_t::TTime time) const;

        //! Get the historical bucket at \p time if it is available.
        SBucket *historicalBucket(core_t::TTime time) const;

    private:
        //! The current mean bucket count.
        double m_MeanBucketCount;

        //! The number of buckets observed to date.
        uint64_t m_NumberBuckets;

        //! The number of non-empty buckets observed to date.
        uint64_t m_NumberNonEmptyBuckets;

        //! The current bucket count.
        uint64_t m_CurrentBucketCount;

        //! The starting time.
        core_t::TTime m_BucketStart;

        //! The bucketing time interval.
        core_t::TTime m_BucketLength;

        //! Optional history buffer to maintain a fixed number of counts.
        //! (This is 56 bytes empty so don't store this by value.)
        TBucketCirBufPtr m_History;
};

}
}

#endif // INCLUDED_prelert_maths_CCounter_h

