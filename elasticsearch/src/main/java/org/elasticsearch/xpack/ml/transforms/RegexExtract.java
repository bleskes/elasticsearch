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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

public class RegexExtract extends Transform {
    private final Pattern pattern;

    public RegexExtract(String regex, List<TransformIndex> readIndexes,
            List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);

        pattern = Pattern.compile(regex);
    }

    @Override
    public TransformResult transform(String[][] readWriteArea)
            throws TransformException {
        TransformIndex readIndex = readIndexes.get(0);
        String field = readWriteArea[readIndex.array][readIndex.index];

        Matcher match = pattern.matcher(field);

        if (match.find()) {
            int maxMatches = Math.min(writeIndexes.size(), match.groupCount());
            for (int i = 0; i < maxMatches; i++) {
                TransformIndex index = writeIndexes.get(i);
                readWriteArea[index.array][index.index] = match.group(i + 1);
            }

            return TransformResult.OK;
        } else {
            logger.warn("Transform 'extract' failed to match field: " + field);
        }

        return TransformResult.FAIL;
    }
}
