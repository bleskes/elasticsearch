/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.process.autodetect;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.persistence.DataPersisterFactory;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.none.NoneJobDataPersister;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.job.process.exceptions.ClosedJobException;
import com.prelert.job.process.exceptions.DataUploadException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.process.writer.ControlMsgToProcessWriter;
import com.prelert.job.process.writer.DataToProcessWriter;
import com.prelert.job.process.writer.DataToProcessWriterFactory;
import com.prelert.job.process.writer.LengthEncodedWriter;
import com.prelert.job.process.writer.RecordWriter;
import com.prelert.job.status.CountingInputStream;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfigs;

/**
 * Manages the native autodetect processes channelling
 * data to them and parsing the results.
 */
public class ProcessManager
{
    private static final Logger LOGGER = Logger.getLogger(ProcessManager.class);

    private ConcurrentMap<String, ProcessAndDataDescription> m_JobIdToProcessMap;

    private final JobProvider m_JobProvider;
    private final ProcessFactory m_ProcessFactory;
    private final DataPersisterFactory m_DataPersisterFactory;
    private final JobLoggerFactory m_JobLoggerFactory;

    public ProcessManager(JobProvider jobProvider, ProcessFactory processFactory,
            DataPersisterFactory dataPersisterFactory, JobLoggerFactory jobLoggerFactory)
    {
        m_JobIdToProcessMap = new ConcurrentHashMap<String, ProcessAndDataDescription>();
        m_JobProvider = jobProvider;
        m_ProcessFactory = processFactory;
        m_DataPersisterFactory = dataPersisterFactory;
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
    }

     /**
     * Passes data to the native process. There are 3 alternate cases handled
     * by this function
     * <ol>
     * <li>This is the first data sent to the job to create a new process</li>
     * <li>The process has already been created and is still active</li>
     * <li>The process was created and has expired with its internal state
     * saved to the database. Create a new process and restore the persisted
     * state</li>
     * </ol>
     * This is a blocking call that won't return until all the data has been
     * written to the process. A new thread is launched to parse the process's
     * output.
     * <br>
     * If there is an error due to the data being in the wrong format or some
     * other runtime error a {@linkplain NativeProcessRunException} is thrown
     * <br>
     * For CSV data if a configured field is missing from the header
     * a {@linkplain MissingFieldException} is thrown
     *
     * @param job
     * @param input
     * @param params
     * @return True if successful or false if the data can't be written because
     * it is already processing some data
     * @throws UnknownJobException
     * @throws NativeProcessRunException If there is a problem creating a new process
     * @throws MissingFieldException If a configured field is missing from
     * the CSV header
     * @throws JsonParseException
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws MalformedJsonException
     * @return Count of records, fields, bytes, etc written
     */
    public DataCounts processDataLoadJob(JobDetails job, InputStream input, DataLoadParams params)
            throws UnknownJobException, NativeProcessRunException, MissingFieldException,
            JsonParseException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        JobDataPersister persister = params.isPersisting() ? m_DataPersisterFactory
                .newDataPersister(job.getId()) : new NoneJobDataPersister();
        return processDataLoadJob(job, input, persister, params);
    }

    private DataCounts processDataLoadJob(JobDetails job, InputStream input,
            JobDataPersister jobDataPersister, DataLoadParams params) throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JsonParseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        ProcessAndDataDescription process = m_JobIdToProcessMap.get(job.getId());
        boolean isExistingProcess = process != null;

        if (process == null)
        {
            // create the new process and restore its state
            // if it has been saved
            process = m_ProcessFactory.createProcess(job);
            m_JobIdToProcessMap.put(job.getId(), process);
        }

        DataCounts stats = new DataCounts();

        // the guard must be released after it is acquired
        try
        {
            // check the process is running, throws if not
            processStillRunning(process);

            if (params.isResettingBuckets())
            {
                writeResetBucketsControlMessage(job.getId(), params, process, isExistingProcess);
            }

            // write the data to the process
            stats = writeToJob(process.getDataDescription(), process.getAnalysisConfig(),
                    process.getSchedulerConfig(), process.getTransforms(),
                    input, process.getProcess().getOutputStream(),
                    process.getStatusReporter(),
                    jobDataPersister,
                    process.getLogger());

            // check there wasn't an error in the input.
            // throws if there was.
            processStillRunning(process);
        }
        catch (IOException e)
        {
            String msg = String.format("Exception writing to process for job %s", job.getId());

            StringBuilder sb = new StringBuilder(msg)
                    .append('\n').append(e.toString()).append('\n');
            readProcessErrorOutput(process, sb);
            process.getLogger().error(sb);

            if (e.getCause() instanceof TimeoutException)
            {
                process.getLogger().warn("Connection to process was dropped due to " +
                        "a timeout - if you are feeding this job from a connector it " +
                        "may be that your connector stalled for too long", e.getCause());
            }

            throw new NativeProcessRunException(sb.toString(),
                    ErrorCodes.NATIVE_PROCESS_WRITE_ERROR);
        }
        return stats;
    }

    private void writeResetBucketsControlMessage(String jobId, DataLoadParams params,
            ProcessAndDataDescription process, boolean isExistingProcess) throws IOException
    {
        if (isExistingProcess)
        {
            ControlMsgToProcessWriter writer = ControlMsgToProcessWriter.create(
                    process.getProcess().getOutputStream(),
                    process.getAnalysisConfig());
            writer.writeResetBucketsMessage(params);
        }
        else
        {
            LOGGER.warn("Cannot reset buckets for job '" + jobId + "'. Buckets can only be reset "
                    + "after first data has been sent to the process.");
        }
    }

    public void writeUpdateConfigMessage(String jobId, String config)
            throws NativeProcessRunException
    {
        ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);
        if (process == null)
        {
            return;
        }
        try
        {
            processStillRunning(process);
        }
        catch (NativeProcessRunException e)
        {
            return;
        }

        ControlMsgToProcessWriter writer = ControlMsgToProcessWriter.create(
                process.getProcess().getOutputStream(),
                process.getAnalysisConfig());

        try
        {
            writer.writeUpdateConfigMessage(config);
            processStillRunning(process);
        }
        catch (IOException e)
        {
            String msg = String.format("Exception updating process for job %s", jobId);
            throwNativeProcessRunExceptionFromIoException(process, e, msg);
        }
    }

    /**
     * Get the number of running active job.
     * A job is considered to be running if it has an active
     * native autodetect process running.
     * @return Count of running jobs
     */
    public int numberOfRunningJobs()
    {
        return m_JobIdToProcessMap.size();
    }

    /**
     * Total number of detectors in the running jobs
     * @return
     */
    public int numberOfRunningDetectors()
    {
        int sum = 0;
        for (ProcessAndDataDescription pdd : m_JobIdToProcessMap.values())
        {
            sum += pdd.getAnalysisConfig().getDetectors().size();
        }
        return sum;
    }

    public List<String> runningJobs()
    {
        List<String> result = new ArrayList<>();
        for (String key : m_JobIdToProcessMap.keySet())
        {
            result.add(key);
        }
        return result;
    }

    /**
     * Return true if the job's autodetect process is running.
     * Is thread safe as uses a concurrent map.
     * @param jobId
     * @return
     */
    public boolean jobIsRunning(String jobId)
    {
        return m_JobIdToProcessMap.get(jobId) != null;
    }

    /**
     * Flush the running job, ensuring that the native process has had the
     * opportunity to process all data previously sent to it with none left
     * sitting in buffers.
     *
     * @param jobId The job to flush
     * @param interimResultsParams Parameters about whether interim results calculation
     * should occur and for which period of time
     * @throws NativeProcessRunException If the process has already terminated
     */
    public void flushJob(String jobId, InterimResultsParams interimResultsParams)
            throws NativeProcessRunException
    {
        LOGGER.info("Flushing job " + jobId);

        ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);
        if (process == null)
        {
            LOGGER.warn("Native process for job '" + jobId + "' is not running - nothing to flush");
            return;
        }

        process.getLogger().info("Flushing job " + jobId);

        // write the data to the process
        try
        {
            // Check the process is running, throws if not
            processStillRunning(process);

            ControlMsgToProcessWriter writer = ControlMsgToProcessWriter.create(
                    process.getProcess().getOutputStream(),
                    process.getAnalysisConfig());

            writer.writeCalcInterimMessage(interimResultsParams);
            String flushId = writer.writeFlushMessage();

            // Check there wasn't an error in the transfer.
            // Throws if there was.
            processStillRunning(process);

            process.getResultsReader().waitForFlushComplete(flushId);

            // This test detects if the back-end process crashed while
            // processing the flush.   Throws if it did.
            processStillRunning(process);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            String msg = String.format("Interrupted while flushing process for job %s", jobId);

            process.getLogger().error(msg);

            throw new NativeProcessRunException(msg,
                    ErrorCodes.NATIVE_PROCESS_FLUSH_INTERRUPTED);
        }
        catch (IOException ioe)
        {
            String msg = String.format("Exception flushing process for job %s", jobId);
            throwNativeProcessRunExceptionFromIoException(process, ioe, msg);
        }
    }

    private void throwNativeProcessRunExceptionFromIoException(ProcessAndDataDescription process,
            IOException ioe, String msg) throws NativeProcessRunException
    {
        StringBuilder sb = new StringBuilder(msg)
                .append('\n').append(ioe.toString()).append('\n');
        readProcessErrorOutput(process, sb);
        process.getLogger().error(sb);
        throw new NativeProcessRunException(sb.toString(), ErrorCodes.NATIVE_PROCESS_WRITE_ERROR);
    }

    /**
     * Stop the running process.
     * Closing the stream into the native process causes the process
     * to terminate its IO loop and stop.<br>
     * The return value is based on the status of the native process
     * ProcessStatus.IN_USE is returned if the process is currently processing
     * data in which case this function should be tried again after a wait period
     * else the process is stopped successfully and ProcessStatus.COMPLETED is
     * returned.
     *
     * @param jobId
     * @throws NativeProcessRunException If the process has already terminated
     */
    public void closeJob(String jobId) throws NativeProcessRunException
    {
        /*
         * Be careful modifying this function because is can throw exceptions in
         * different places; there are quite a lot of code paths through it.
         * Some code appears repeated but it is because of the multiple code paths
         */
        LOGGER.info("Closing job " + jobId);

        ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);
        if (process == null)
        {
            LOGGER.warn("No job with id '" + jobId + "' to shutdown");
            return;
        }

        try
        {
            setJobStatus(jobId, process.getLogger(), JobStatus.CLOSING);

            process.getLogger().info("Closing job " + jobId);

            try
            {
                // check the process is running, throws if not
                if (processStillRunning(process))
                {
                    terminateProcess(jobId, process);
                }
            }
            catch (NativeProcessRunException npre)
            {
                String msg = String.format("Native process for job '%s' has already exited",
                        jobId);
                LOGGER.error(msg);
                process.getLogger().error(msg);

                // clean up resources and re-throw
                process.deleteAssociatedFiles();

                try
                {
                    process.getProcess().getOutputStream().close();
                }
                catch (IOException ioe)
                {
                    LOGGER.debug("Exception closing stopped process input stream", ioe);
                }

                setJobFinishedTimeAndStatus(jobId, process.getLogger(), JobStatus.FAILED);

                throw npre;
            }
        }
        finally
        {
            m_JobIdToProcessMap.remove(jobId);
            m_JobLoggerFactory.close(jobId, process.getLogger());
        }
    }

    private void terminateProcess(String jobId, ProcessAndDataDescription process)
            throws NativeProcessRunException
    {
        try
        {
            // closing its input causes the process to exit
            process.getProcess().getOutputStream().close();

            // wait for the process to exit
            int exitValue = process.getProcess().waitFor();

            // wait for the results parsing and write to to the datastore
            process.joinParserThread();

            process.deleteAssociatedFiles();

            setJobFinishedTimeAndStatus(jobId, process.getLogger(), JobStatus.CLOSED);

            String msg = String.format("Process returned with value %d.", exitValue);
            if (exitValue != 0)
            {
                process.getLogger().error(msg);

                // Read any error output from the process
                StringBuilder sb = new StringBuilder();
                readProcessErrorOutput(process, sb);
                process.getLogger().error(sb);

                throw new NativeProcessRunException(sb.toString(),
                        ErrorCodes.NATIVE_PROCESS_ERROR);
            }
            else
            {
                process.getLogger().info(msg);
            }
        }
        catch (IOException | InterruptedException e)
        {
            String msg = "Exception closing the running native process";
            LOGGER.warn(msg);
            process.getLogger().warn(msg, e);

            setJobFinishedTimeAndStatus(jobId, process.getLogger(), JobStatus.FAILED);
        }
    }

    /**
     * Checks if the native process is still running. If the process has
     * exited this is due to an error as it should only stop once its
     * inputstream is closed. If it has stopped the thrown exception
     * contains the error output of the process else true is returned.
     *
     * @param process
     * @param jobId
     * @return true if the process is still running or throw an exception if
     * if has terminated for some reason
     * @throws NativeProcessRunException If the process has exited
     */
    private boolean processStillRunning(ProcessAndDataDescription process)
    throws NativeProcessRunException
    {
        // Sanity check make sure the process hasn't terminated already
        try
        {
            int exitValue = process.getProcess().exitValue();

            // If we get here the process has exited.
            String msg = String.format("Process exited with code %d.", exitValue);
            process.getLogger().warn(msg);

            // Read any error output from the process and
            // add to the returned error.
            StringBuilder sb = new StringBuilder(msg).append('\n');
            readProcessErrorOutput(process, sb);

            process.getLogger().warn(sb);

            throw new NativeProcessRunException(sb.toString(),
                    ErrorCodes.NATIVE_PROCESS_ERROR);
        }
        catch (IllegalThreadStateException e)
        {
            return true;
        }
    }

    private void setJobStatus(String jobId, Logger processLogger, JobStatus status)
    {
        try
        {
            m_JobProvider.setJobStatus(jobId, status);
        }
        catch (UnknownJobException e)
        {
            String msg = String.format("Error cannot set job status to " + status);
            processLogger.warn(msg, e);
            LOGGER.warn(msg, e);
        }
    }

    private void setJobFinishedTimeAndStatus(String jobId, Logger processLogger,
            JobStatus status)
    {
        try
        {
            m_JobProvider.setJobFinishedTimeAndStatus(jobId, new Date(), status);
        }
        catch (UnknownJobException e)
        {
            String msg = String.format("Error cannot set finished job status and time");
            processLogger.warn(msg, e);
            LOGGER.warn(msg, e);
        }
    }

    /**
     * Transform the data according to the data description and
     * pipe to the output.
     * Data is written via the recordWriter parameter
     *
     *@param recordWriter
     * @param dataDescription
     * @param analysisFields
     * @param input
     * @param statusReporter
     * @param jobLogger
     * @throws JsonParseException
     * @throws MissingFieldException If any fields are missing from the CSV header
     * @throws IOException
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws MalformedJsonException
     * @return Count of records, fields, bytes, etc written
     */
    public DataCounts writeToJob(boolean includeControlField, RecordWriter recordWriter,
            DataDescription dataDescription,
            AnalysisConfig analysisConfig,
            SchedulerConfig schedulerConfig,
            TransformConfigs transforms,
            InputStream input,
            StatusReporter statusReporter,
            JobDataPersister dataPersister,
            Logger jobLogger)
    throws JsonParseException, MissingFieldException, IOException,
        HighProportionOfBadTimestampsException, OutOfOrderRecordsException, MalformedJsonException
    {
        DataToProcessWriter writer = new DataToProcessWriterFactory().create(includeControlField, recordWriter,
                dataDescription, analysisConfig, schedulerConfig, transforms, statusReporter, dataPersister, jobLogger);

        return writer.write(input);
    }

    /**
     * Transform the data according to the data description and
     * pipe to the output.
     * Data is written via BufferedOutputStream which is more
     * suited for small writes.
     * Only the fields matching those in the list <code>analysisFields</code>
     * are send to the process.
     * For CSV data <code>MissingFieldException</code> is
     * thrown if any fields are missing from the header JSON objects may
     * be different so an error is logged in that case.
     *
     * @param dataDescription
     * @param analysisConfig
     * @param transforms
     * @param input
     * @param output
     * @param statusReporter
     * @param dataPersister
     * @param jobLogger
     * @return
     * @throws JsonParseException
     * @throws MissingFieldException
     * @throws IOException
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws MalformedJsonException
     */
    public DataCounts writeToJob(DataDescription dataDescription,
            AnalysisConfig analysisConfig,
            SchedulerConfig schedulerConfig,
            TransformConfigs transforms,
            InputStream input, OutputStream output,
            StatusReporter statusReporter,
            JobDataPersister dataPersister,
            Logger jobLogger)
    throws JsonParseException, MissingFieldException, IOException,
        HighProportionOfBadTimestampsException, OutOfOrderRecordsException, MalformedJsonException
    {
        // Don't close the output stream as it causes the autodetect
        // process to quit
        // Oracle's documentation recommends buffering process streams
        BufferedOutputStream bufferedStream = new BufferedOutputStream(output);
        LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(bufferedStream);
        CountingInputStream countingStream = new CountingInputStream(input, statusReporter);

        try
        {
            DataToProcessWriter writer = new DataToProcessWriterFactory().create(true, lengthEncodedWriter,
                    dataDescription, analysisConfig, schedulerConfig, transforms, statusReporter, dataPersister, jobLogger);

            return writer.write(countingStream);
        }
        catch (RuntimeException e)
        {
            // finish reporting status to persist stats but catch any
            // exception so it does not suppress any exception thrown in the try block
            tryFinishReporting(statusReporter, jobLogger);

            DataUploadException dataUploadException = new DataUploadException(
                    statusReporter.incrementalStats(), e);
            LOGGER.error(dataUploadException.getMessage(), e);
            throw dataUploadException;
        }
        finally
        {
            countingStream.close();

            // flush the writer but catch any exception so it does not suppress
            // any exception thrown in the try block
            tryFlushingWriter(lengthEncodedWriter, jobLogger);
        }
    }

    private static void tryFinishReporting(StatusReporter statusReporter, Logger jobLogger)
    {
        try
        {
            statusReporter.finishReporting();
        }
        catch (Exception statusReporterException)
        {
            jobLogger.warn("Exception while trying to finish reporting", statusReporterException);
        }
    }

    private static void tryFlushingWriter(LengthEncodedWriter lengthEncodedWriter, Logger jobLogger)
    {
        try
        {
            lengthEncodedWriter.flush();
        }
        catch (IOException e)
        {
            jobLogger.warn("Exception flushing lengthEncodedWriter", e);
        }
    }

    /**
     * Get the analytics version string.
     *
     * @return
     */
    public String getAnalyticsVersion()
    {
        return ProcessCtrl.getAnalyticsVersion();
    }


    /**
     * Get a JSON document containing some of the usage and license info.
     *
     * @return The JSON document in string form
     */
    public String getInfo()
    {
        return ProcessCtrl.getInfo();
    }


    /**
     * Read the error output from the process into the string builder.
     *
     * @param process
     * @param sb This will be modified and returned.
     * @return The parameter <code>sb</code>
     */
    private StringBuilder readProcessErrorOutput(ProcessAndDataDescription process,
                                        StringBuilder sb)
    {
        try
        {
            if (process.getErrorReader().ready() == false)
            {
                return sb;
            }

            String line;
            while ((line = process.getErrorReader().readLine()) != null)
            {
                sb.append(line).append('\n');
            }
        }
        catch (IOException e)
        {
            process.getLogger().warn("Exception thrown reading the native processes "
                    + "error output", e);
        }

        return sb;
    }

    public void addAlertObserver(String jobId, AlertObserver ao) throws ClosedJobException
    {
        ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);
        if (process!= null)
        {
            process.getResultsReader().addAlertObserver(ao);
        }
        else
        {
            String message = String.format("Cannot alert on job '%s' because "
                    + "the job is not running", jobId);
            LOGGER.info(message);
            throw new ClosedJobException(message);
        }
    }

    public boolean removeAlertObserver(String jobId, AlertObserver ao)
    {
        ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);
        if (process != null)
        {
            return process.getResultsReader().removeAlertObserver(ao);
        }

        return false;
    }

    public void deletePersistedData(String jobId)
    {
        m_DataPersisterFactory.newDataPersister(jobId).deleteData();
    }
}
