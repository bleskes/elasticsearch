package org.elasticsearch.xpack.prelert.job.update;

import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

class ResultsRetentionDaysUpdater extends AbstractLongUpdater {

    public ResultsRetentionDaysUpdater(JobDetails job, String updateKey) {
        super(job, updateKey, 0);
    }

    @Override
    protected void apply() {
        job().setResultsRetentionDays(getNewValue());
    }

    @Override
    protected String getInvalidMessageKey() {
        return Messages.JOB_CONFIG_UPDATE_RESULTS_RETENTION_DAYS_INVALID;
    }
}
