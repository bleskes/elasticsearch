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
package org.elasticsearch.xpack.prelert.job.manager;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.data.DataProcessor;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectCommunicator;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectCommunicatorFactory;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

public class AutodetectProcessManager implements DataProcessor {

    private static final Logger LOGGER = Loggers.getLogger(AutodetectProcessManager.class);
    private final AutodetectCommunicatorFactory autodetectCommunicatorFactory;
    private final JobManager jobManager;
    private final ConcurrentMap<String, AutodetectCommunicator> autoDetectCommunicatorByJob;


    public AutodetectProcessManager(AutodetectCommunicatorFactory autodetectCommunicatorFactory, JobManager jobManager) {
        this.autodetectCommunicatorFactory = autodetectCommunicatorFactory;
        this.jobManager = jobManager;
        this.autoDetectCommunicatorByJob = new ConcurrentHashMap<>();
    }

    @Override
    public DataCounts processData(String jobId, InputStream input, DataLoadParams params) {

        AutodetectCommunicator communicator = autoDetectCommunicatorByJob.get(jobId);
        if (communicator == null) {
            communicator = create(jobId, params.isIgnoreDowntime());
            autoDetectCommunicatorByJob.put(jobId, communicator);
        }

        try {
            if (params.isResettingBuckets()) {
                communicator.writeResetBucketsControlMessage(params);
            }

            return communicator.writeToJob(input);
            // TODO check for errors from autodetect
        }
        catch (IOException e) {
            String msg = String.format(Locale.ROOT, "Exception writing to process for job %s", jobId);

            if (e.getCause() instanceof TimeoutException)
            {
                LOGGER.warn("Connection to process was dropped due to a timeout - if you are feeding this job from a connector it " +
                        "may be that your connector stalled for too long", e.getCause());
            }

            throw ExceptionsHelper.serverError(msg, ErrorCodes.NATIVE_PROCESS_WRITE_ERROR);
        }
    }

    private AutodetectCommunicator create(String jobId, boolean ignoreDowntime) {
        Job job = jobManager.getJobOrThrowIfUnknown(jobId);
        return autodetectCommunicatorFactory.create(job, ignoreDowntime);
    }

    @Override
    public void flushJob(String jobId, InterimResultsParams params) {
        LOGGER.debug("Flushing job {}", jobId);

        AutodetectCommunicator communicator = autoDetectCommunicatorByJob.get(jobId);
        if (communicator == null) {
            LOGGER.debug("Cannot flush: no active autodetect process for job {}", jobId);
            return;
        }

        try {
            communicator.flushJob(params);
            // TODO check for errors from autodetect
        }
        catch (IOException ioe)
        {
            String msg = String.format(Locale.ROOT, "Exception flushing process for job %s", jobId);
            LOGGER.warn(msg);
            throw ExceptionsHelper.serverError(msg, ioe, ErrorCodes.NATIVE_PROCESS_WRITE_ERROR);
        }
    }

    public void writeUpdateConfigMessage(String jobId, String config) throws IOException {

        AutodetectCommunicator communicator = autoDetectCommunicatorByJob.get(jobId);
        if (communicator == null) {
            LOGGER.debug("Cannot update config: no active autodetect process for job {}", jobId);
            return;
        }

        communicator.writeUpdateConfigMessage(config);
        // TODO check for errors from autodetect
    }

    @Override
    public void closeJob(String jobId) {
        LOGGER.debug("Closing job {}", jobId);
        AutodetectCommunicator communicator = autoDetectCommunicatorByJob.get(jobId);
        if (communicator == null) {
            LOGGER.debug("Cannot close: no active autodetect process for job {}", jobId);
            return;
        }

        try {
            communicator.close();
            // TODO check for errors from autodetect
            // TODO delete associated files (model config etc)
        }
        catch (IOException e) {
            LOGGER.info("Exception closing stopped process input stream", e);
        }
        finally {
            autoDetectCommunicatorByJob.remove(jobId);
            setJobFinishedTimeAndStatus(jobId, JobStatus.CLOSED);
        }
    }

    public int numberOfRunningJobs()
    {
        return autoDetectCommunicatorByJob.size();
    }

    public boolean jobHasActiveAutodetectProcess(String jobId)
    {
        return autoDetectCommunicatorByJob.get(jobId) != null;
    }

    public Duration jobUpTime(String jobId) {
        AutodetectCommunicator communicator = autoDetectCommunicatorByJob.get(jobId);
        if (communicator == null) {
            return Duration.ZERO;
        }

        return Duration.between(communicator.getProcessStartTime(), ZonedDateTime.now());
    }

    private void setJobFinishedTimeAndStatus(String jobId, JobStatus status)
    {
        // NORELEASE Implement this.
        // Perhaps move the JobStatus and finish time to a separate document stored outside the cluster state
        LOGGER.error("Cannot set finished job status and time- Not Implemented");
    }

    public void addAlertObserver(String jobId, AlertObserver alertObserver)
    {
        AutodetectCommunicator communicator = autoDetectCommunicatorByJob.get(jobId);
        if (communicator != null)
        {
            communicator.addAlertObserver(alertObserver);
        }
        else
        {
            throw ExceptionsHelper.jobClosed(jobId);
        }
    }

    public boolean removeAlertObserver(String jobId, AlertObserver alertObserver)
    {
        AutodetectCommunicator communicator = autoDetectCommunicatorByJob.get(jobId);
        if (communicator != null)
        {
            return communicator.removeAlertObserver(alertObserver);
        }

        return false;
    }

}
