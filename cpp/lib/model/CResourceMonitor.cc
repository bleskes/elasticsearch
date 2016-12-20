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

#include <model/CResourceMonitor.h>

#include <core/Constants.h>
#include <core/CStatistics.h>

#include <model/CModelEnsemble.h>

#include <algorithm>
#include <limits>


namespace prelert
{

namespace model
{

// Only prune once per hour
const core_t::TTime CResourceMonitor::MINIMUM_PRUNE_FREQUENCY(60 * 60);

CResourceMonitor::CResourceMonitor(void) : m_AllowAllocations(true),
    m_ByteLimitHigh(0), m_ByteLimitLow(0), m_Current(0), m_Previous(0), m_Peak(0),
    m_LastAllocationFailureReport(0), m_MemoryStatus(model_t::E_MemoryStatusOk),
    m_HasPruningStarted(false), m_PruneThreshold(0), m_LastPruneTime(0),
    m_PruneWindow(std::numeric_limits<std::size_t>::max()),
    m_PruneWindowMaximum(std::numeric_limits<std::size_t>::max()),
    m_PruneWindowMinimum(std::numeric_limits<std::size_t>::max()),
    m_NoLimit(false)
{
    this->memoryLimit((sizeof(size_t) < 8) ? 1024: 4096);
}

CResourceMonitor::CResourceMonitor(std::size_t limit) : m_AllowAllocations(true),
    m_ByteLimitHigh(0), m_ByteLimitLow(0), m_Current(0), m_Previous(0), m_Peak(0),
    m_LastAllocationFailureReport(0), m_MemoryStatus(model_t::E_MemoryStatusOk),
    m_HasPruningStarted(false), m_PruneThreshold(0), m_LastPruneTime(0),
    m_PruneWindow(std::numeric_limits<std::size_t>::max()),
    m_PruneWindowMaximum(std::numeric_limits<std::size_t>::max()),
    m_PruneWindowMinimum(std::numeric_limits<std::size_t>::max()),
    m_NoLimit(false)
{
    this->memoryLimit(limit);
}

void CResourceMonitor::memoryUsageReporter(const TMemoryUsageReporterFunc &reporter)
{
    m_MemoryUsageReporter = reporter;
}

void CResourceMonitor::registerComponent(CModelEnsemble &models)
{
    LOG_TRACE("Registering component: " << &models);
    m_Models.insert(TModelEnsemblePSizePr(&models, std::size_t(0)));
}

void CResourceMonitor::unRegisterComponent(CModelEnsemble &models)
{
    TModelEnsemblePSizeMapItr iter = m_Models.find(&models);
    if (iter == m_Models.end())
    {
        LOG_ERROR("Inconsistency - component has not been registered: " << &models);
        return;
    }

    LOG_TRACE("Unregistering component: " << &models);
    m_Models.erase(iter);
}

void CResourceMonitor::memoryLimit(std::size_t limit)
{
    // The threshold for no limit is set such that any negative limit cast to
    // a size_t (which is unsigned) will be taken to mean no limit
    if (limit > std::numeric_limits<std::size_t>::max() / 2)
    {
        LOG_INFO("Setting no memory limit");
        m_NoLimit = true;
        // The high limit is set to around half what it could potentially be.
        // The reason is that other code will do "what if" calculations on this
        // number, such as "what would total memory usage be if we allocated 10
        // more models?", and it causes problems if these calculations overflow.
        m_ByteLimitHigh = std::numeric_limits<std::size_t>::max() / 2 + 1;
        m_ByteLimitLow = m_ByteLimitHigh - 1024;
        m_PruneThreshold = static_cast<std::size_t>(m_ByteLimitHigh / 5 * 3);
        return;
    }
    uint64_t wantedLimit = static_cast<uint64_t>(limit) * 1024 * 1024;
    static const uint64_t SENSIBLE_LIMIT = 16384ull << (sizeof(std::size_t) * 4);
    if (wantedLimit > SENSIBLE_LIMIT)
    {
        LOG_INFO("Capping requested memory limit of " << wantedLimit <<
                 " bytes at sensible level of " << SENSIBLE_LIMIT <<
                 " bytes for a " << (sizeof(std::size_t) * 8) <<
                 " bit operating system");
        m_ByteLimitHigh = static_cast<std::size_t>(SENSIBLE_LIMIT);
    }
    else
    {
        m_ByteLimitHigh = static_cast<std::size_t>(wantedLimit);
    }
    m_PruneThreshold = static_cast<std::size_t>(m_ByteLimitHigh / 5 * 3);

    m_ByteLimitLow = m_ByteLimitHigh - 1024;
}

void CResourceMonitor::refresh(CModelEnsemble &models, core_t::TTime bucketStartTime)
{
    if (m_NoLimit)
    {
        return;
    }
    this->forceRefresh(models, bucketStartTime);
}

void CResourceMonitor::forceRefresh(CModelEnsemble &models, core_t::TTime bucketStartTime)
{
    this->forceRefreshNoSend(models);

    // Try and report if usage has gone up by more than 1%
    if (this->needToSendReport())
    {
        this->sendMemoryUsageReport(bucketStartTime);
    }
}

void CResourceMonitor::forceRefreshNoSend(CModelEnsemble &models)
{
    this->memUsage(models);
    core::CStatistics::stat(stat_t::E_MemoryUsage).set(m_Current);
    LOG_TRACE("Checking allocations: currently at " << m_Current);
    this->updateAllowAllocations();
}

void CResourceMonitor::updateAllowAllocations(void)
{
    if (m_AllowAllocations)
    {
        if (m_Current > m_ByteLimitHigh)
        {
            LOG_INFO("Over allocation limit. " << m_Current <<
                " bytes used, the limit is " << m_ByteLimitHigh);
            m_AllowAllocations = false;
        }
    }
    else
    {
        if (m_Current < m_ByteLimitLow)
        {
            LOG_INFO("Below allocation limit, used " << m_Current);
            m_AllowAllocations = true;
        }
    }
}

bool CResourceMonitor::needToSendReport(void)
{
    // Has the usage changed by more than 1% ?
    if (m_Current > m_Previous)
    {
        if ((m_Current - m_Previous) > m_Previous / 100)
        {
            return true;
        }
    }
    else
    {
        if ((m_Previous - m_Current) > m_Previous / 100)
        {
            return true;
        }
    }

    if (!m_AllocationFailures.empty())
    {
        core_t::TTime lastestAllocationError = (--m_AllocationFailures.end())->first;
        if (lastestAllocationError > m_LastAllocationFailureReport)
        {
            return true;
        }
    }
    return false;
}

bool CResourceMonitor::pruneIfRequired(core_t::TTime endTime)
{
    // The basic idea here is that as the memory usage goes up, we
    // prune models to bring it down again. If usage declines, we
    // relax the pruning window to let it go back up again.

    bool aboveThreshold = m_Current > m_PruneThreshold;

    if (m_HasPruningStarted == false && !aboveThreshold)
    {
        LOG_TRACE("No pruning required. " << m_Current << " / " << m_PruneThreshold);
        return false;
    }

    if (endTime < m_LastPruneTime + MINIMUM_PRUNE_FREQUENCY)
    {
        LOG_TRACE("Too soon since last prune to prune again");
        return false;
    }

    if (m_Models.empty())
    {
        return false;
    }

    if (m_HasPruningStarted == false)
    {
        // The longest we'll consider keeping priors for is 1M buckets.
        CModel * model = m_Models.begin()->first->model(endTime);
        if (model == 0)
        {
            return false;
        }
        m_PruneWindowMaximum = model->defaultPruneWindow();
        m_PruneWindow = m_PruneWindowMaximum;
        m_PruneWindowMinimum = model->minimumPruneWindow();
        m_HasPruningStarted = true;
        this->acceptPruningResult();
        LOG_DEBUG("Pruning started. Window (buckets): " << m_PruneWindow);
    }

    if (aboveThreshold)
    {
        // Do a prune and see how much we got back
        // These are the expensive operations
        std::size_t usageAfter = 0;
        for (TModelEnsemblePSizeMapItr i = m_Models.begin();
             i != m_Models.end(); ++i)
        {
            i->first->pruneModels(endTime, m_PruneWindow);
            i->second = i->first->memoryUsage();
            usageAfter += i->second;
        }
        m_Current = usageAfter;
        this->updateAllowAllocations();
    }

    LOG_TRACE("Pruning models. Usage: " <<
              m_Current << ". Current window: " << m_PruneWindow << " buckets");

    if (m_Current < m_PruneThreshold)
    {
        // Expand the window
        m_PruneWindow = std::min(m_PruneWindow + std::size_t(
            (endTime - m_LastPruneTime) / m_Models.begin()->first->model(endTime)->bucketLength()),
            m_PruneWindowMaximum);
        LOG_TRACE("Expanding window, to " << m_PruneWindow);
    }
    else
    {
        // Shrink the window
        m_PruneWindow = std::max(static_cast<std::size_t>(m_PruneWindow * 99 / 100),
                                 m_PruneWindowMinimum);
        LOG_TRACE("Shrinking window, to " << m_PruneWindow);
    }

    m_LastPruneTime = endTime;
    return aboveThreshold;
}

bool CResourceMonitor::areAllocationsAllowed(void) const
{
    return m_AllowAllocations;
}

bool CResourceMonitor::areAllocationsAllowed(std::size_t size) const
{
    if (m_AllowAllocations)
    {
        return m_Current + size < m_ByteLimitHigh;
    }
    return false;
}

std::size_t CResourceMonitor::allocationLimit(void) const
{
    return m_ByteLimitHigh - m_Current;
}

void CResourceMonitor::memUsage(CModelEnsemble &models)
{
    TModelEnsemblePSizeMapItr iter = m_Models.find(&models);
    if (iter == m_Models.end())
    {
        LOG_ERROR("Inconsistency - component has not been registered: " <<
                  &models);
        return;
    }
    std::size_t modelPreviousUsage = iter->second;
    std::size_t modelCurrentUsage = iter->first->memoryUsage();
    iter->second = modelCurrentUsage;
    m_Current += (modelCurrentUsage - modelPreviousUsage);
}

void CResourceMonitor::sendMemoryUsageReport(core_t::TTime bucketStartTime)
{
    m_Peak = std::max(m_Peak, m_Current);
    if (!m_MemoryUsageReporter.empty())
    {
        m_MemoryUsageReporter(this->createMemoryUsageReport(bucketStartTime));
        if (!m_AllocationFailures.empty())
        {
            m_LastAllocationFailureReport = m_AllocationFailures.rbegin()->first;
        }
    }
    m_Previous = m_Current;
}

CResourceMonitor::SResults CResourceMonitor::createMemoryUsageReport(core_t::TTime bucketStartTime)
{
    SResults res;
    res.s_ByFields = 0;
    res.s_OverFields = 0;
    res.s_PartitionFields = 0;
    res.s_Usage = m_Current;
    res.s_AllocationFailures = 0;
    res.s_MemoryStatus = m_MemoryStatus;
    res.s_BucketStartTime = bucketStartTime;
    for (TModelEnsemblePSizeMapItr iter = m_Models.begin();
         iter != m_Models.end();
         ++iter)
    {
        ++res.s_PartitionFields;
        res.s_OverFields += iter->first->numberOverFieldValues();
        res.s_ByFields += iter->first->numberByFieldValues();
    }
    for (TTimeSizeMapCItr iter = m_AllocationFailures.begin();
         iter != m_AllocationFailures.end();
         ++iter)
    {
        ++res.s_AllocationFailures;
    }

    return res;
}

void CResourceMonitor::acceptAllocationFailureResult(core_t::TTime time)
{
    m_MemoryStatus = model_t::E_MemoryStatusHardLimit;
    ++m_AllocationFailures[time];
}

void CResourceMonitor::acceptPruningResult(void)
{
    if (m_MemoryStatus == model_t::E_MemoryStatusOk)
    {
        m_MemoryStatus = model_t::E_MemoryStatusSoftLimit;
    }
}

bool CResourceMonitor::haveNoLimit(void) const
{
    return m_NoLimit;
}

} // model
} // prelert

