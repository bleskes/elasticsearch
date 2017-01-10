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
import java.util.Locale;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;

public class StringTransform extends Transform {
    private final Function<String, String> convertFunction;

    private StringTransform(Function<String, String> convertFunction,
            List<TransformIndex> readIndexes, List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);
        this.convertFunction = convertFunction;
        if (readIndexes.size() != 1 || writeIndexes.size() != 1) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public TransformResult transform(String[][] readWriteArea) throws TransformException {
        TransformIndex readIndex = readIndexes.get(0);
        TransformIndex writeIndex = writeIndexes.get(0);
        String input = readWriteArea[readIndex.array][readIndex.index];
        readWriteArea[writeIndex.array][writeIndex.index] = convertFunction.apply(input);
        return TransformResult.OK;
    }

    public static StringTransform createLowerCase(List<TransformIndex> readIndexes,
            List<TransformIndex> writeIndexes, Logger logger) {
        return new StringTransform(s -> s.toLowerCase(Locale.ROOT), readIndexes, writeIndexes, logger);
    }

    public static StringTransform createUpperCase(List<TransformIndex> readIndexes,
            List<TransformIndex> writeIndexes, Logger logger) {
        return new StringTransform(s -> s.toUpperCase(Locale.ROOT), readIndexes, writeIndexes, logger);
    }

    public static StringTransform createTrim(List<TransformIndex> readIndexes,
            List<TransformIndex> writeIndexes, Logger logger) {
        return new StringTransform(s -> s.trim(), readIndexes, writeIndexes, logger);
    }
}
