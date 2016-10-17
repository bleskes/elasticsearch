package org.elasticsearch.xpack.prelert.job.update;

import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

class RenormalizationWindowDaysUpdater extends AbstractLongUpdater {

    public RenormalizationWindowDaysUpdater(JobDetails job, String updateKey) {
        super(job, updateKey, 0);
    }

    @Override
    protected void apply() {
        job().setRenormalizationWindowDays(getNewValue());
    }

    @Override
    protected String getInvalidMessageKey() {
        return Messages.JOB_CONFIG_UPDATE_RENORMALIZATION_WINDOW_DAYS_INVALID;
    }
}
