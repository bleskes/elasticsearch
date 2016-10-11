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
     * The condition should have been verified by now and it <i>must</i>
     * have a valid value & operator
     *
     * @param condition
     * @param readIndexes
     * @param writeIndexes
     * @param logger
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
