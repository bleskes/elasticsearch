package org.elasticsearch.xpack.prelert.job.update;


import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.config.verification.JobConfigurationVerifier;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

class BackgroundPersistIntervalUpdater extends AbstractLongUpdater {

    public BackgroundPersistIntervalUpdater(JobDetails job, String updateKey) {
        super(job, updateKey, JobConfigurationVerifier.MIN_BACKGROUND_PERSIST_INTERVAL);
    }

    @Override
    protected void apply() {
        job().setBackgroundPersistInterval(getNewValue());
    }

    @Override
    protected String getInvalidMessageKey() {
        return Messages.JOB_CONFIG_UPDATE_BACKGROUND_PERSIST_INTERVAL_INVALID;
    }
}
