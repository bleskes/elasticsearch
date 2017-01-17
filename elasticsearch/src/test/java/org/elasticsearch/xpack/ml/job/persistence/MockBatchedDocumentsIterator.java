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

import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;

public class MockBatchedDocumentsIterator<T> implements BatchedDocumentsIterator<T> {
    private final List<Deque<T>> batches;
    private int index;
    private boolean wasTimeRangeCalled;
    private String interimFieldName;

    public MockBatchedDocumentsIterator(List<Deque<T>> batches) {
        this.batches = batches;
        index = 0;
        wasTimeRangeCalled = false;
        interimFieldName = "";
    }

    @Override
    public BatchedDocumentsIterator<T> timeRange(long startEpochMs, long endEpochMs) {
        wasTimeRangeCalled = true;
        return this;
    }

    @Override
    public BatchedDocumentsIterator<T> includeInterim(String interimFieldName) {
        this.interimFieldName = interimFieldName;
        return this;
    }

    @Override
    public Deque<T> next() {
        if ((!wasTimeRangeCalled) || !hasNext()) {
            throw new NoSuchElementException();
        }
        return batches.get(index++);
    }

    @Override
    public boolean hasNext() {
        return index != batches.size();
    }

    /**
     * If includeInterim has not been called this is an empty string
     */
    public String getInterimFieldName() {
        return interimFieldName;
    }
}