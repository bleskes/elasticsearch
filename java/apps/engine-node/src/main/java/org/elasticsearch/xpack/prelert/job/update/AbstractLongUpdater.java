package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

abstract class AbstractLongUpdater extends AbstractUpdater {
    private Long newValue;
    private long minVal;

    public AbstractLongUpdater(JobDetails job, String updateKey, long minVal) {
        super(job, updateKey);
        this.minVal = minVal;
    }

    @Override
    void update(JsonNode node) {
        if (node.isIntegralNumber() || node.isNull()) {
            newValue = node.isIntegralNumber() ? node.asLong() : null;
            if (newValue != null && newValue < minVal) {
                throwInvalidValue();
            }
        } else {
            throwInvalidValue();
        }
        apply();
    }

    protected Long getNewValue() {
        return newValue;
    }

    private void throwInvalidValue() {
        throw ExceptionsHelper.invalidRequestException(Messages.getMessage(getInvalidMessageKey()), ErrorCodes.INVALID_VALUE);
    }

    protected abstract String getInvalidMessageKey();
    protected abstract void apply();
}
