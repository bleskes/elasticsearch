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

    public MockBatchedResultsIterator(long startEpochMs, long endEpochMs, List<Deque<T>> batches)
    {
        m_StartEpochMs = startEpochMs;
        m_EndEpochMs = endEpochMs;
        m_Batches = batches;
        m_Index = 0;
        m_WasTimeRangeCalled = false;
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
}