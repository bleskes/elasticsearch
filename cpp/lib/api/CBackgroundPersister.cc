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
#include <api/CBackgroundPersister.h>

#include <core/CLogger.h>
#include <core/CScopedFastLock.h>

#include <string>


namespace prelert
{
namespace api
{

CBackgroundPersister::CBackgroundPersister(core::CDataAdder &dataAdder)
    : m_DataAdder(dataAdder),
      m_IsBusy(false),
      m_BackgroundThread(*this)
{
}

CBackgroundPersister::~CBackgroundPersister(void)
{
   this->waitForIdle();
}

bool CBackgroundPersister::isBusy(void) const
{
    return m_IsBusy;
}

bool CBackgroundPersister::waitForIdle(void)
{
    {
        core::CScopedFastLock lock(m_Mutex);

        if (!m_BackgroundThread.isStarted())
        {
            return true;
        }
    }

    return m_BackgroundThread.waitForFinish();
}

bool CBackgroundPersister::startPersist(core::CDataAdder::TPersistFunc persistFunc)
{
    if (persistFunc.empty())
    {
        return false;
    }

    core::CScopedFastLock lock(m_Mutex);

    if (m_IsBusy)
    {
        return false;
    }

    if (m_BackgroundThread.isStarted())
    {
        // This join should be fast as the busy flag is false so the thread
        // should either have already exited or be on the verge of exiting
        if (m_BackgroundThread.waitForFinish() == false)
        {
            return false;
        }
    }

    m_PersistFunc.swap(persistFunc);

    m_IsBusy = m_BackgroundThread.start();

    return m_IsBusy;
}

CBackgroundPersister::CBackgroundThread::CBackgroundThread(CBackgroundPersister &owner)
    : m_Owner(owner)
{
}

void CBackgroundPersister::CBackgroundThread::run(void)
{
    m_Owner.m_PersistFunc(m_Owner.m_DataAdder);

    // There could be a large amount of data bound into the persistence
    // function, so it's good to release this as soon as it's finished with
    m_Owner.m_PersistFunc.clear();

    core::CScopedFastLock lock(m_Owner.m_Mutex);

    m_Owner.m_IsBusy = false;
}

void CBackgroundPersister::CBackgroundThread::shutdown(void)
{
    // NO-OP - there's no generic way to interrupt the single function call that
    // run() makes
}


}
}

