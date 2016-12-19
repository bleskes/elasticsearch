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
package org.elasticsearch.xpack.prelert.transforms.date;

import java.util.List;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.xpack.prelert.transforms.Transform;
import org.elasticsearch.xpack.prelert.transforms.TransformException;

/**
 * Abstract class introduces the {@link #epochMs()} method for
 * date transforms
 */
public abstract class DateTransform extends Transform {
    protected static final int SECONDS_TO_MS = 1000;

    private long epochMs;

    public DateTransform(List<TransformIndex> readIndexes, List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);
    }

    /**
     * The epoch time from the last transform
     */
    public long epochMs() {
        return epochMs;
    }

    /**
     * Expects 1 input and 1 output.
     */
    @Override
    public final TransformResult transform(String[][] readWriteArea) throws TransformException {
        if (readIndexes.isEmpty()) {
            throw new ParseTimestampException("Cannot parse null string");
        }

        if (writeIndexes.isEmpty()) {
            throw new ParseTimestampException("No write index for the datetime format transform");
        }

        TransformIndex i = readIndexes.get(0);
        String field = readWriteArea[i.array][i.index];

        if (field == null) {
            throw new ParseTimestampException("Cannot parse null string");
        }

        epochMs = toEpochMs(field);
        TransformIndex writeIndex = writeIndexes.get(0);
        readWriteArea[writeIndex.array][writeIndex.index] = Long.toString(epochMs / SECONDS_TO_MS);
        return TransformResult.OK;
    }

    protected abstract long toEpochMs(String field) throws TransformException;
}
