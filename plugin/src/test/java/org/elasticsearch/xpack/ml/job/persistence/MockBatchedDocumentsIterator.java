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
package org.elasticsearch.xpack.ml.job.persistence;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.search.SearchHit;

import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;

public class MockBatchedDocumentsIterator<T> extends BatchedDocumentsIterator<T> {
    private final List<Deque<T>> batches;
    private int index;
    private boolean wasTimeRangeCalled;
    private Boolean includeInterim;
    private Boolean requireIncludeInterim;

    public MockBatchedDocumentsIterator(List<Deque<T>> batches) {
        super(mock(Client.class), "foo");
        this.batches = batches;
        index = 0;
        wasTimeRangeCalled = false;
    }

    @Override
    public BatchedDocumentsIterator<T> timeRange(long startEpochMs, long endEpochMs) {
        wasTimeRangeCalled = true;
        return this;
    }

    @Override
    public BatchedDocumentsIterator<T> includeInterim(boolean includeInterim) {
        this.includeInterim = includeInterim;
        return this;
    }

    @Override
    public Deque<T> next() {
        if (requireIncludeInterim != null && requireIncludeInterim != includeInterim) {
            throw new IllegalStateException("Required include interim value [" + requireIncludeInterim + "]; actual was ["
                    + includeInterim + "]");
        }
        if ((!wasTimeRangeCalled) || !hasNext()) {
            throw new NoSuchElementException();
        }
        return batches.get(index++);
    }

    @Override
    protected String getType() {
        return null;
    }

    @Override
    protected T map(SearchHit hit) {
        return null;
    }

    @Override
    public boolean hasNext() {
        return index != batches.size();
    }

    @Nullable
    public Boolean isIncludeInterim() {
        return includeInterim;
    }

    public void requireIncludeInterim(boolean value) {
        this.requireIncludeInterim = value;
    }
}