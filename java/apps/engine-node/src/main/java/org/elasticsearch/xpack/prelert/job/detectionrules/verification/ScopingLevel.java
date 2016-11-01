/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.detectionrules.verification;

import org.elasticsearch.xpack.prelert.job.Detector;

enum ScopingLevel
{
    PARTITION(3),
    OVER(2),
    BY(1);

    int level;

    private ScopingLevel(int level)
    {
        this.level = level;
    }

    boolean isHigherThan(ScopingLevel other)
    {
        return level > other.level;
    }

    static ScopingLevel from(Detector detector, String fieldName)
    {
        if (fieldName.equals(detector.getPartitionFieldName()))
        {
            return ScopingLevel.PARTITION;
        }
        if (fieldName.equals(detector.getOverFieldName()))
        {
            return ScopingLevel.OVER;
        }
        if (fieldName.equals(detector.getByFieldName()))
        {
            return ScopingLevel.BY;
        }
        throw new IllegalArgumentException(
                "fieldName '" + fieldName + "' does not match an analysis field");
    }
}
