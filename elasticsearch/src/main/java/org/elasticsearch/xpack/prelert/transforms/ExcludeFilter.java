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
package org.elasticsearch.xpack.prelert.transforms;

import java.util.List;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.xpack.prelert.job.condition.Condition;


/**
 * Abstract base class for exclude filters
 */
public abstract class ExcludeFilter extends Transform {
    private final Condition condition;

    /**
     * The condition should have been verified by now and it <i>must</i> have a
     * valid value &amp; operator
     */
    public ExcludeFilter(Condition condition, List<TransformIndex> readIndexes,
            List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);
        this.condition = condition;
    }

    public Condition getCondition() {
        return condition;
    }
}
