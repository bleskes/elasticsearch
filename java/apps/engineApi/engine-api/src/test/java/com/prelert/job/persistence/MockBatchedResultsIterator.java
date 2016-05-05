/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.persistence;

import static org.junit.Assert.assertEquals;

import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import com.prelert.job.UnknownJobException;

public class MockBatchedResultsIterator<T> implements BatchedResultsIterator<T>
{
    private final long m_StartEpochMs;
    private final long m_EndEpochMs;
    private final List<Deque<T>> m_Batches;
    private int m_Index;
    private boolean m_WasTimeRangeCalled;
    private String m_InterimFieldName;

    public MockBatchedResultsIterator(long startEpochMs, long endEpochMs, List<Deque<T>> batches)
    {
        m_StartEpochMs = startEpochMs;
        m_EndEpochMs = endEpochMs;
        m_Batches = batches;
        m_Index = 0;
        m_WasTimeRangeCalled = false;
        m_InterimFieldName = "";
    }

    @Override
    public BatchedResultsIterator<T> timeRange(long startEpochMs, long endEpochMs)
    {
        assertEquals(m_StartEpochMs, startEpochMs);
        assertEquals(m_EndEpochMs, endEpochMs);
        m_WasTimeRangeCalled = true;
        return this;
    }

    @Override
    public BatchedResultsIterator<T> includeInterim(String interimFieldName)
    {
        m_InterimFieldName = interimFieldName;
        return this;
    }

    @Override
    public Deque<T> next() throws UnknownJobException
    {
        if (!m_WasTimeRangeCalled || !hasNext())
        {
            throw new NoSuchElementException();
        }
        return m_Batches.get(m_Index++);
    }

    @Override
    public boolean hasNext()
    {
        return m_Index != m_Batches.size();
    }

    /**
     * If includeInterim has not been called this is an empty string
     *
     * @return
     */
    public String getInterimFieldName()
    {
        return m_InterimFieldName;
    }
}