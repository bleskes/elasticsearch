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

import org.elasticsearch.xpack.ml.job.condition.Condition;

/**
 * Matches a field against a regex
 */
public class ExcludeFilterRegex extends ExcludeFilter {
    private final Pattern pattern;

    public ExcludeFilterRegex(Condition condition, List<TransformIndex> readIndexes,
            List<TransformIndex> writeIndexes, Logger logger) {
        super(condition, readIndexes, writeIndexes, logger);

        pattern = Pattern.compile(getCondition().getValue());
    }

    /**
     * Returns {@link TransformResult#EXCLUDE} if the record matches the regex
     */
    @Override
    public TransformResult transform(String[][] readWriteArea)
            throws TransformException {
        TransformResult result = TransformResult.OK;
        for (TransformIndex readIndex : readIndexes) {
            String field = readWriteArea[readIndex.array][readIndex.index];
            Matcher match = pattern.matcher(field);

            if (match.matches()) {
                result = TransformResult.EXCLUDE;
                break;
            }
        }

        return result;
    }

}
