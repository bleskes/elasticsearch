package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

class SchedulerConfigUpdater extends AbstractUpdater {

    public SchedulerConfigUpdater(JobDetails job, String updateKey) {
        super(job, updateKey);
    }

    @Override
    void update(JsonNode node) {
        checkJobIsScheduled();
        checkSchedulerIsStopped();
        SchedulerConfig.Builder newConfig = parseSchedulerConfig(node);
        checkNotNull(newConfig);
        checkDataSourceHasNotChanged(newConfig);
        job().setSchedulerConfig(newConfig.build());
    }

    private void checkJobIsScheduled() {
        if (job().getSchedulerConfig() == null) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_SCHEDULER_NO_SUCH_SCHEDULED_JOB, jobId()),
                    ErrorCodes.NO_SUCH_SCHEDULED_JOB);
        }
    }

    private void checkSchedulerIsStopped() {
        SchedulerState schedulerState = job().getSchedulerState();
        if (schedulerState != null && schedulerState.getStatus() != JobSchedulerStatus.STOPPED) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_UPDATE_IN_CURRENT_STATE,
                    jobId(), job().getSchedulerState().getStatus()), ErrorCodes.CANNOT_UPDATE_JOB_SCHEDULER);
        }
    }

    private static SchedulerConfig.Builder parseSchedulerConfig(JsonNode node) {
        try {
            return JSON_MAPPER.convertValue(node, SchedulerConfig.Builder.class);
        } catch (IllegalArgumentException e) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_SCHEDULE_CONFIG_PARSE_ERROR), ErrorCodes.INVALID_VALUE);
        }
    }

    private static void checkNotNull(SchedulerConfig.Builder schedulerConfig) {
        if (schedulerConfig == null) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_SCHEDULE_CONFIG_CANNOT_BE_NULL),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    private void checkDataSourceHasNotChanged(SchedulerConfig.Builder schedulerConfig) {
        JobDetails job = job();
        SchedulerConfig currentSchedulerConfig = job.getSchedulerConfig();
        SchedulerConfig.DataSource currentDataSource = currentSchedulerConfig.getDataSource();
        SchedulerConfig.DataSource newDataSource = schedulerConfig.getDataSource();
        if (!currentDataSource.equals(newDataSource)) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_SCHEDULE_CONFIG_DATA_SOURCE_INVALID,
                    currentDataSource, newDataSource), ErrorCodes.INVALID_VALUE);
        }
    }
}
