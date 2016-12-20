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

#include <maths/CCounter.h>

#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStringUtils.h>

#include <maths/CIntegerTools.h>

#include <boost/bind.hpp>
#include <boost/math/tools/precision.hpp>

#include <algorithm>
#include <iterator>
#include <string>


namespace prelert
{
namespace maths
{

// We obfuscate the XML element names to avoid giving away too much information
// about our model
namespace
{
const std::string MEAN_BUCKET_COUNT_TAG("a");
const std::string NUM_BUCKETS_TAG("b");
const std::string NUM_NON_EMPTY_BUCKETS_TAG("c");
const std::string CURRENT_BUCKET_COUNT_TAG("d");
const std::string BUCKET_START_TAG("e");
const std::string BUCKET_LENGTH_TAG("f");
const std::string HISTORY_TAG("g");

const std::string EMPTY_STRING;
}

CCounter::CCounter(const core_t::TTime &startTime,
                   const core_t::TTime &bucketLength,
                   const TBucketCirBufPtr history) :
        m_MeanBucketCount(0.0),
        m_NumberBuckets(0u),
        m_NumberNonEmptyBuckets(0u),
        m_CurrentBucketCount(0u),
        m_BucketStart(startTime),
        m_BucketLength(bucketLength),
        m_History(history)
{
}

CCounter::CCounter(core::CStateRestoreTraverser &traverser) :
        m_MeanBucketCount(0.0),
        m_NumberBuckets(0u),
        m_NumberNonEmptyBuckets(0u),
        m_CurrentBucketCount(0u),
        m_BucketStart(0),
        m_BucketLength(0)
{
    traverser.traverseSubLevel(boost::bind(&CCounter::acceptRestoreTraverser,
                                           this,
                                           _1));
}

CCounter::CCounter(const TBucketCirBufPtr history,
                   core::CStateRestoreTraverser &traverser) :
        m_MeanBucketCount(0.0),
        m_NumberBuckets(0u),
        m_NumberNonEmptyBuckets(0u),
        m_CurrentBucketCount(0u),
        m_BucketStart(0),
        m_BucketLength(0),
        m_History(history)
{
    traverser.traverseSubLevel(boost::bind(&CCounter::acceptRestoreTraverser,
                                           this,
                                           _1));
}

CCounter::CCounter(const CCounter &other) :
        m_MeanBucketCount(other.m_MeanBucketCount),
        m_NumberBuckets(other.m_NumberBuckets),
        m_NumberNonEmptyBuckets(other.m_NumberNonEmptyBuckets),
        m_CurrentBucketCount(other.m_CurrentBucketCount),
        m_BucketStart(other.m_BucketStart),
        m_BucketLength(other.m_BucketLength),
        m_History()
{
    if (other.m_History != 0)
    {
        m_History.reset(new TBucketCirBuf(*other.m_History));
    }
}

CCounter &CCounter::operator=(const CCounter &rhs)
{
    if (this != &rhs)
    {
        m_MeanBucketCount = rhs.m_MeanBucketCount;
        m_NumberBuckets = rhs.m_NumberBuckets;
        m_NumberNonEmptyBuckets = rhs.m_NumberNonEmptyBuckets;
        m_CurrentBucketCount = rhs.m_CurrentBucketCount;
        m_BucketStart = rhs.m_BucketStart;
        m_BucketLength = rhs.m_BucketLength;
        if (rhs.m_History != 0)
        {
            m_History.reset(new TBucketCirBuf(*rhs.m_History));
        }
        else
        {
            m_History.reset();
        }
    }
    return *this;
}

core_t::TTime CCounter::bucketStart(void) const
{
    return m_BucketStart;
}

core_t::TTime CCounter::bucketEnd(void) const
{
    return m_BucketStart + m_BucketLength;
}

core_t::TTime CCounter::bucketLength(void) const
{
    return m_BucketLength;
}

bool CCounter::countsAvailable(core_t::TTime time) const
{
    core_t::TTime startTime = m_BucketStart;
    if (m_History)
    {
        startTime -= static_cast<core_t::TTime>(m_History->size()) * m_BucketLength;
    }

    return time >= startTime;
}

void CCounter::addArrival(core_t::TTime time)
{
    // Ignore out of order arrival times.
    if (time < m_BucketStart)
    {
        return;
    }

    // Create new buckets as necessary and update the counts.
    if (time >= m_BucketStart + m_BucketLength)
    {
        this->startNewBucket(time);
    }

    ++m_CurrentBucketCount;
}

void CCounter::timeNow(core_t::TTime time)
{
    if (time >= m_BucketStart + m_BucketLength)
    {
        this->startNewBucket(time);
    }
}

double CCounter::meanBucketCount(core_t::TTime time) const
{
    if (time >= m_BucketStart + m_BucketLength)
    {
        return this->computeMeanBucketCount(time);
    }

    if (time < m_BucketStart)
    {
        const SBucket *bucket = this->historicalBucket(time);
        return bucket ? bucket->s_MeanCountPreceding : 0.0;
    }

    return m_MeanBucketCount;
}

uint64_t CCounter::currentBucketCount(core_t::TTime time) const
{
    if (time >= m_BucketStart + m_BucketLength)
    {
        return 0u;
    }

    if (time < m_BucketStart)
    {
        const SBucket *bucket = this->historicalBucket(time);
        return bucket ? bucket->s_CurrentCount : 0u;
    }

    return m_CurrentBucketCount;
}

uint64_t CCounter::numberNonEmptyBuckets(core_t::TTime time) const
{
    if (time >= m_BucketStart + m_BucketLength)
    {
        return m_NumberNonEmptyBuckets + (m_CurrentBucketCount > 0u ? 1u : 0u);
    }

    if (time < m_BucketStart)
    {
        const SBucket *bucket = this->historicalBucket(time);
        return bucket ? bucket->s_NumberNonEmptyBucketsPreceding : 0u;
    }

    return m_NumberNonEmptyBuckets;
}

uint64_t CCounter::numberBuckets(core_t::TTime time) const
{
    if (time < m_BucketStart)
    {
        const SBucket *bucket = this->historicalBucket(time);
        return bucket ? bucket->s_NumberBucketsPreceding : 0u;
    }

    return m_NumberBuckets + this->computeElapsedBuckets(time);
}

uint64_t CCounter::computeElapsedBuckets(core_t::TTime time) const
{
    if (time < m_BucketStart)
    {
        return 0;
    }

    return static_cast<uint64_t>(CIntegerTools::floor(time - m_BucketStart,
                                                      m_BucketLength)
                                 / m_BucketLength);
}

void CCounter::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(MEAN_BUCKET_COUNT_TAG,
                         m_MeanBucketCount,
                         core::CIEEE754::E_SinglePrecision);
    inserter.insertValue(NUM_BUCKETS_TAG, m_NumberBuckets);
    inserter.insertValue(NUM_NON_EMPTY_BUCKETS_TAG, m_NumberNonEmptyBuckets);
    inserter.insertValue(CURRENT_BUCKET_COUNT_TAG, m_CurrentBucketCount);
    inserter.insertValue(BUCKET_START_TAG, m_BucketStart);
    inserter.insertValue(BUCKET_LENGTH_TAG, m_BucketLength);

    if (m_History != 0)
    {
        // Persist the history in reverse order, because it's expanded by
        // pushing to the front.
        for (TBucketCirBufCRItr bucketItr = m_History->rbegin();
             bucketItr != m_History->rend();
             ++bucketItr)
        {
            inserter.insertLevel(HISTORY_TAG,
                                 boost::bind(&CCounter::historyBucketAcceptPersistInserter,
                                             boost::cref(*bucketItr),
                                             _1));
        }
    }
}

void CCounter::historyBucketAcceptPersistInserter(const SBucket &bucket,
                                                  core::CStatePersistInserter &inserter)
{
    inserter.insertValue(MEAN_BUCKET_COUNT_TAG,
                         bucket.s_MeanCountPreceding,
                         core::CIEEE754::E_SinglePrecision);
    inserter.insertValue(NUM_BUCKETS_TAG, bucket.s_NumberBucketsPreceding);
    inserter.insertValue(NUM_NON_EMPTY_BUCKETS_TAG, bucket.s_NumberNonEmptyBucketsPreceding);
    inserter.insertValue(CURRENT_BUCKET_COUNT_TAG, bucket.s_CurrentCount);
}

bool CCounter::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == MEAN_BUCKET_COUNT_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 m_MeanBucketCount) == false)
            {
                LOG_ERROR("Invalid mean bucket count in " << traverser.value());
                return false;
            }
        }
        else if (name == NUM_BUCKETS_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 m_NumberBuckets) == false)
            {
                LOG_ERROR("Invalid number of buckets in " << traverser.value());
                return false;
            }
        }
        else if (name == NUM_NON_EMPTY_BUCKETS_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 m_NumberNonEmptyBuckets) == false)
            {
                LOG_ERROR("Invalid number of non-empty buckets in " << traverser.value());
                return false;
            }
        }
        else if (name == CURRENT_BUCKET_COUNT_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 m_CurrentBucketCount) == false)
            {
                LOG_ERROR("Invalid current bucket count in " << traverser.value());
                return false;
            }
        }
        else if (name == BUCKET_START_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 m_BucketStart) == false)
            {
                LOG_ERROR("Invalid bucket start in " << traverser.value());
                return false;
            }
        }
        else if (name == BUCKET_LENGTH_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 m_BucketLength) == false)
            {
                LOG_ERROR("Invalid bucket length in " << traverser.value());
                return false;
            }
        }
        else if (name == HISTORY_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CCounter::historyAcceptRestoreTraverser,
                                                       this,
                                                       _1)) == false)
            {
                LOG_ERROR("Invalid history in " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

bool CCounter::historyAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    if (m_History == 0)
    {
        LOG_WARN("Attempting to restore history to counter that has no history store");
        return true;
    }

    SBucket bucket(0.0, 0u, 0u, 0u);

    do
    {
        const std::string &name = traverser.name();
        if (name == MEAN_BUCKET_COUNT_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 bucket.s_MeanCountPreceding) == false)
            {
                LOG_ERROR("Invalid mean bucket count in " << traverser.value());
                return false;
            }
        }
        else if (name == NUM_BUCKETS_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 bucket.s_NumberBucketsPreceding) == false)
            {
                LOG_ERROR("Invalid number of buckets in " << traverser.value());
                return false;
            }
        }
        else if (name == NUM_NON_EMPTY_BUCKETS_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 bucket.s_NumberNonEmptyBucketsPreceding) == false)
            {
                LOG_ERROR("Invalid number of non-empty buckets in " << traverser.value());
                return false;
            }
        }
        else if (name == CURRENT_BUCKET_COUNT_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 bucket.s_CurrentCount) == false)
            {
                LOG_ERROR("Invalid current bucket count in " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());

    // The history was persisted in reverse order, hence we push to the
    // front of the circular buffer when restoring.
    m_History->push_front(bucket);

    return true;
}

void CCounter::startNewBucket(core_t::TTime time)
{
    uint64_t nBuckets = this->computeElapsedBuckets(time);

    if (m_History)
    {
        // The most recent buckets are stored at the front of history.
        m_History->push_front(SBucket(m_MeanBucketCount,
                                      m_NumberBuckets,
                                      m_NumberNonEmptyBuckets,
                                      m_CurrentBucketCount));

        uint64_t size = std::min(nBuckets, static_cast<uint64_t>(m_History->max_size()));
        core_t::TTime bucketStartTime = m_BucketStart + m_BucketLength;
        for (uint64_t i = 1; i < size; ++i, bucketStartTime += m_BucketLength)
        {
            m_History->push_front(SBucket(this->meanBucketCount(bucketStartTime),
                                          this->numberBuckets(bucketStartTime),
                                          this->numberNonEmptyBuckets(bucketStartTime),
                                          this->currentBucketCount(bucketStartTime)));
        }
    }

    // This must be computed before updating the number of buckets.
    m_MeanBucketCount = this->computeMeanBucketCount(time);

    m_NumberBuckets += nBuckets;
    m_NumberNonEmptyBuckets += (m_CurrentBucketCount > 0u ? 1u : 0u);
    m_CurrentBucketCount = 0u;
    m_BucketStart += nBuckets * m_BucketLength;
}

double CCounter::computeMeanBucketCount(core_t::TTime time) const
{
    uint64_t nBuckets = this->computeElapsedBuckets(time);
    double totalBuckets = static_cast<double>(m_NumberBuckets + nBuckets);

    // Use the fact that:
    //   mean( {x(i) : i = 1,...,n+1} )
    //     = Sum_{i=1,n+1}( x(i) ) / (n+1)
    //     = Sum_{i=1,n}( x(i) ) / n * n / (n+1) + x(n+1) / (n+1)
    //     = (1 - 1 / (n+1)) * mean( {x(i) : i = 1,...,n} ) + 1 / (n+1) * x(n+1)
    //     = alpha * mean( x(i) : i = 1,...,n} ) + beta * x(n+1)
    //
    // Note that we must account for empty buckets in alpha.

    double alpha = static_cast<double>(nBuckets) / totalBuckets;
    alpha = std::max(alpha, boost::math::tools::epsilon<double>());
    alpha = 1.0 - alpha;

    double beta = 1.0 / totalBuckets;
    beta = std::max(beta, boost::math::tools::epsilon<double>());

    return alpha * m_MeanBucketCount
           + beta * static_cast<double>(m_CurrentBucketCount);
}

CCounter::SBucket *CCounter::historicalBucket(core_t::TTime time) const
{
    if (time < m_BucketStart)
    {
        // Check if we've got this bucket in history.
        if (m_History)
        {
            std::size_t i = static_cast<std::size_t>(
                                CIntegerTools::strictInfimum(m_BucketStart - time,
                                                             m_BucketLength)
                                / m_BucketLength);
            if (i < m_History->size())
            {
                return &(*m_History)[i];
            }
        }
    }

    return 0;
}

CCounter::SBucket::SBucket(double meanCountPreceding,
                           uint64_t numberBucketsPreceding,
                           uint64_t numberNonEmptyBucketsPreceding,
                           uint64_t currentCount) :
        s_MeanCountPreceding(meanCountPreceding),
        s_NumberBucketsPreceding(numberBucketsPreceding),
        s_NumberNonEmptyBucketsPreceding(numberNonEmptyBucketsPreceding),
        s_CurrentCount(currentCount)
{
}

}
}
