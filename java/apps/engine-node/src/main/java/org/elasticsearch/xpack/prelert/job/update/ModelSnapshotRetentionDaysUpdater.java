package org.elasticsearch.xpack.prelert.job.update;

import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

class ModelSnapshotRetentionDaysUpdater extends AbstractLongUpdater {

    public ModelSnapshotRetentionDaysUpdater(JobDetails job, String updateKey) {
        super(job, updateKey, 0);
    }

    @Override
    protected void apply() {
        job().setModelSnapshotRetentionDays(getNewValue());
    }

    @Override
    protected String getInvalidMessageKey() {
        return Messages.JOB_CONFIG_UPDATE_MODEL_SNAPSHOT_RETENTION_DAYS_INVALID;
    }
}
