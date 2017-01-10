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
package org.elasticsearch.xpack.ml.transforms;

import java.util.List;
import java.util.StringJoiner;

import org.apache.logging.log4j.Logger;


/**
 * Concatenate input fields
 */
public class Concat extends Transform {
    private static final String EMPTY_STRING = "";

    private final String delimiter;

    public Concat(List<TransformIndex> readIndexes, List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);
        delimiter = EMPTY_STRING;
    }

    public Concat(String join, List<TransformIndex> readIndexes, List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);
        delimiter = join;
    }

    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Concat has only 1 output field
     */
    @Override
    public TransformResult transform(String[][] readWriteArea)
            throws TransformException {
        if (writeIndexes.isEmpty()) {
            return TransformResult.FAIL;
        }

        TransformIndex writeIndex = writeIndexes.get(0);

        StringJoiner joiner = new StringJoiner(delimiter);
        for (TransformIndex i : readIndexes) {
            joiner.add(readWriteArea[i.array][i.index]);
        }
        readWriteArea[writeIndex.array][writeIndex.index] = joiner.toString();

        return TransformResult.OK;
    }
}
