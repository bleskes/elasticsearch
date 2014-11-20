/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job.process;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.JobDetails;
import com.prelert.job.JobInUseException;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.DataPersisterFactory;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.quantiles.QuantilesState;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.status.StatusReporterFactory;
import com.prelert.job.usage.UsageReporterFactory;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.parsing.AlertObserver;

/**
 * Manages the native processes channelling data to them and parsing the
 * results.
 *
 * This class registers a JVM shutdown hook once the
 * purpose of which is to stop any running processes
 */
public class ProcessManager
{
	public static final String LOG_FILE_APPENDER_NAME = "engine_api_file_appender";
	/**
	 * JVM shutdown hook stops all the running processes
	 */
	private class StopProcessShutdownHook extends Thread
	{
		ProcessManager m_ProcessManager;

		public StopProcessShutdownHook(ProcessManager pm)
		{
			m_ProcessManager = pm;
		}

		@Override
		public void run()
		{
			m_ProcessManager.stopAllJobs();
		}
	}

	/**
	 * The status of a native process managed by this manager.
	 * IN_USE means the process is running its analysis, COMPLETED means
	 * it stopped normally.
	 */
	public enum ProcessStatus {IN_USE, COMPLETED};

	static private final Logger s_Logger = Logger.getLogger(ProcessManager.class);

	private ProcessCtrl m_ProcessCtrl;

	private ConcurrentMap<String, ProcessAndDataDescription> m_JobIdToProcessMap;
	private ConcurrentMap<String, ScheduledFuture<?>> m_JobIdToTimeoutFuture;

	private ScheduledExecutorService m_ProcessTimeouts;

	private JobProvider m_JobProvider;

	private ResultsReaderFactory m_ResultsReaderFactory;
	private StatusReporterFactory m_StatusReporterFactory;
	private UsageReporterFactory m_UsageReporterFactory;
	private DataPersisterFactory m_DataPersisterFactory;

	public ProcessManager(JobProvider jobProvider,
							ResultsReaderFactory readerFactory,
							StatusReporterFactory statusReporterFactory,
							UsageReporterFactory usageFactory,
							DataPersisterFactory dataPersisterFactory)
	{
		m_ProcessCtrl = new ProcessCtrl();

		m_JobIdToProcessMap = new ConcurrentHashMap<String, ProcessAndDataDescription>();

		m_ProcessTimeouts = Executors.newScheduledThreadPool(1);
		m_JobIdToTimeoutFuture = new ConcurrentHashMap<String, ScheduledFuture<?>>();

		m_JobProvider = jobProvider;
		m_ResultsReaderFactory = readerFactory;
		m_UsageReporterFactory = usageFactory;

		m_StatusReporterFactory = statusReporterFactory;

		m_DataPersisterFactory = dataPersisterFactory;

		addShutdownHook();
	}


	private void addShutdownHook()
	{
		StopProcessShutdownHook shutdownHook = new StopProcessShutdownHook(this);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
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
	 * This is a blocking call that won't return untill all the data has been
	 * written to the process. A new thread is launched to parse the process's
	 * output.
	 * <br/>
	 * If there is an error due to the data being in the wrong format or some
	 * other runtime error a {@linkplain NativeProcessRunException} is thrown
	 * <br/>
	 * For CSV data if a configured field is missing from the header
	 * a {@linkplain MissingFieldException} is thrown
	 *
	 * @param jobId
	 * @param input
	 * @return True if successful or false if the data can't be written because
	 * it is already processing some data
	 * @throws UnknownJobException
	 * @throws NativeProcessRunException If there is a problem creating a new process
	 * @throws MissingFieldException If a configured field is missing from
	 * the CSV header
	 * @throws JsonParseException
	 * @throws JobInUseException if the data cannot be written to because
	 * the job is already handling data
	 * @throws HighProportionOfBadTimestampsException
	 * @throws OutOfOrderRecordsException
	 */
	public boolean processDataLoadJob(String jobId, InputStream input)
	throws UnknownJobException, NativeProcessRunException, MissingFieldException,
		JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
	{
		// stop the timeout
		ScheduledFuture<?> future = m_JobIdToTimeoutFuture.remove(jobId);
		if (future != null)
		{
			if (future.cancel(false) == false)
			{
				s_Logger.warn("Failed to cancel future in dataToJob(...)");
			}
		}
		else
		{
			s_Logger.debug("No future to cancel in dataToJob(...)");
		}

		ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);

		if (process == null)
		{
			// create the new process and restore its state
			// if it has been saved
			process = createProcess(jobId);
			m_JobIdToProcessMap.put(jobId, process);
		}

		// We can't write data if someone is already writing to the process.
		if (process.isInUse())
		{
			String msg = String.format("Another connection is writing to job "
					+ "%s. Jobs will only accept data from one connection at a time",
					jobId);
			s_Logger.warn(msg);
			throw new JobInUseException(jobId, msg, ErrorCode.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
		}

		// check the process is running, throws if not
		processStillRunning(process, jobId);

		// write the data to the process
		try
		{
			process.setInUse(true);

			writeToJob(process.getDataDescription(), process.getAnalysisConfig(),
					input, process.getProcess().getOutputStream(),
					process.getStatusReporter(),
					m_DataPersisterFactory.newDataPersister(jobId, s_Logger),
					process.getLogger());

			// check there wasn't an error in the input.
			// throws if there was.
			processStillRunning(process, jobId);
		}
		catch (IOException e)
		{
			String msg = String.format("Exception writing to process for job %s", jobId);

			StringBuilder sb = new StringBuilder(msg)
					.append('\n').append(e.toString()).append('\n');
			readProcessErrorOutput(process, sb);
			process.getLogger().error(sb);

			throw new NativeProcessRunException(sb.toString(),
					ErrorCode.NATIVE_PROCESS_WRITE_ERROR);
		}
		finally
		{
			process.setInUse(false);

			// start a new timer
			future = startShutdownTimer(jobId, process.getTimeout());
			m_JobIdToTimeoutFuture.put(jobId, future);
		}

		return true;
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
	 * Return true if the job's autodetect process is running.
	 *
	 * @param jobId
	 * @return
	 */
	public boolean jobIsRunning(String jobId)
	{
		return m_JobIdToProcessMap.get(jobId) != null;
	}


	/**
	 * Create a new autodetect process restoring its state if persisted
	 *
	 * @param jobId
	 * @return
	 * @throws UnknownJobException If there is no job with <code>jobId</code>
	 * @throws NativeProcessRunException
	 */
	public ProcessAndDataDescription createProcess(String jobId)
	throws UnknownJobException, NativeProcessRunException
	{
		JobDetails job = m_JobProvider.getJobDetails(jobId);

		return createProcess(job, true);
	}

	/**
	 * Create a new autodetect process from the JobDetails restoring
	 * its state if <code>restoreState</code> is true.
	 *
	 * @param job
	 * @param restoreState Will attempt to restore the state but it isn't an
	 * error if there is no state to restore
	 * @return
	 * @throws UnknownJobException If there is no job with <code>jobId</code>
	 * @throws NativeProcessRunException If an error is encountered creating
	 * the native process
	 */
	private ProcessAndDataDescription createProcess(JobDetails job,
										boolean restoreState)
	throws UnknownJobException, NativeProcessRunException
	{
		String jobId = job.getId();

		Logger logger = createLogger(job.getId());

		QuantilesState quantilesState = null;
		if (restoreState)
		{
			quantilesState = m_JobProvider.getQuantilesState(jobId);
		}

		Process nativeProcess = null;
		List<File> filesToDelete = new ArrayList<>();
		try
		{
			// if state is null or empty it will be ignored
			// else it is used to restore the quantiles
			nativeProcess = ProcessCtrl.buildAutoDetect(
					ProcessCtrl.AUTODETECT_API, job, quantilesState, logger,
					filesToDelete);
		}
		catch (IOException e)
		{
			String msg = "Failed to launch process for job " + job.getId();
			s_Logger.error(msg);
			logger.error(msg, e);
			throw new NativeProcessRunException(msg,
					ErrorCode.NATIVE_PROCESS_START_ERROR, e);
		}


		ProcessAndDataDescription procAndDD = new ProcessAndDataDescription(
				nativeProcess, jobId,
				job.getDataDescription(), job.getTimeout(), job.getAnalysisConfig(), logger,
				m_StatusReporterFactory.newStatusReporter(jobId, job.getCounts(),
						m_UsageReporterFactory.newUsageReporter(jobId, logger),
						 logger),
				m_ResultsReaderFactory.newResultsParser(jobId,
						nativeProcess.getInputStream(),
						logger),
				filesToDelete
				);

		m_JobProvider.setJobStatus(jobId, JobStatus.RUNNING);

		logger.debug("Created process for job " + jobId);

		return procAndDD;
	}


	/**
	 * Stop the running process.
	 * Closing the stream into the native process causes the process
	 * to terminate its IO loop and stop.<br/>
	 * The return value is based on the status of the native process
	 * ProcessStatus.IN_USE is returned if the process is currently processing
	 * data in which case this function should be tried again after a wait period
	 * else the process is stopped successfully and ProcessStatus.COMPLETED is
	 * returned.
	 *
	 *
	 * @param jobId
	 * @return The process finished status
	 * @throws UnknownJobException If the job is already finished or cannot be
	 * found in the local map of processes.
	 * @throws NativeProcessRunException If the process has already terminated
	 * @throws JobInUseException if the job cannot be closed because data is
	 * being streamed to it
	 */
	public ProcessStatus finishJob(String jobId)
	throws NativeProcessRunException, JobInUseException
	{
		/*
		 * Be careful modifying this function because is can throw exceptions in
		 * different places there are quite a lot of code paths through it.
		 * Some code appears repeated but it is because of the multiple code paths
		 */
		s_Logger.info("Finishing job " + jobId);

		ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);
		if (process == null)
		{
			s_Logger.warn("No job with id '" + jobId + "' to shutdown");
			// tidy up
			m_JobIdToTimeoutFuture.remove(jobId);

			return ProcessStatus.COMPLETED;
		}

		process.getLogger().info("Finishing job " + jobId);

		if (process.isInUse())
		{
			s_Logger.error("Cannot finish job while it is reading data");
			process.getLogger().error("Cannot finish job while it is reading data");

			String msg = String.format("Cannot close job %s while the job is actively "
					+ "processing data", jobId);
			s_Logger.error(msg);
			throw new JobInUseException(jobId, msg, ErrorCode.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
		}


		// cancel any time out futures
		ScheduledFuture<?> future = m_JobIdToTimeoutFuture.get(jobId);
		if (future != null)
		{
			if (future.cancel(false) == false)
			{
				s_Logger.warn("Failed to cancel future in finishJob()");
			}
		}
		else
		{
			s_Logger.debug("No future to cancel in finishJob()");
		}
		m_JobIdToTimeoutFuture.remove(jobId);


		try
		{
			// check the process is running, throws if not
			if (processStillRunning(process, jobId))
			{
				m_JobIdToProcessMap.remove(jobId);
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

						// free the logger resources
						closeLogger(process.getLogger());

						throw new NativeProcessRunException(sb.toString(),
								ErrorCode.NATIVE_PROCESS_ERROR);
					}
					else
					{
						process.getLogger().info(msg);
					}

					// free the logger resources
					closeLogger(process.getLogger());
				}
				catch (IOException | InterruptedException e)
				{
					String msg = "Exception closing the running native process";
					s_Logger.warn(msg);
					process.getLogger().warn(msg, e);

					setJobFinishedTimeAndStatus(jobId, process.getLogger(), JobStatus.FAILED);

					// free the logger resources
					closeLogger(process.getLogger());
				}

			}
		}
		catch (NativeProcessRunException npre)
		{
			String msg = String.format("Native process for job '%s' has already exited",
					jobId);
			s_Logger.error(msg);
			process.getLogger().error(msg);

			// clean up resources and re-throw
			process.deleteAssociatedFiles();

			m_JobIdToProcessMap.remove(jobId);
			try
			{
				process.getProcess().getOutputStream().close();
			}
			catch (IOException ioe)
			{
				s_Logger.debug("Exception closing stopped process input stream");
			}

			setJobFinishedTimeAndStatus(jobId, process.getLogger(), JobStatus.FAILED);
			// free the logger resources
			closeLogger(process.getLogger());

			throw npre;
		}

		return ProcessStatus.COMPLETED;
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
	private boolean processStillRunning(ProcessAndDataDescription process,
									String jobId)
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
					ErrorCode.NATIVE_PROCESS_ERROR);
		}
		catch (IllegalThreadStateException e)
		{
			return true;
		}
	}

	private void setJobFinishedTimeAndStatus(String jobId, Logger processLogger,
			JobStatus status)
	{
		try
		{
			m_JobProvider.setJobFinishedTimeandStatus(jobId,
					new Date(), status);
		}
		catch (UnknownJobException e)
		{
			String msg = String.format("Error cannot set finished job status and time");
			processLogger.warn(msg, e);
			s_Logger.warn(msg, e);
		}
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
	 * @param analysisFields
	 * @param input
	 * @param output
	 * @param statusReporter
	 * @param jobLogger
	 * @throws JsonParseException
	 * @throws MissingFieldException If any fields are missing from the CSV header
	 * @throws IOException
	 * @throws HighProportionOfBadTimestampsException
	 * @throws OutOfOrderRecordsException
	 */
	public void writeToJob(DataDescription dataDescription,
			AnalysisConfig analysisConfig,
			InputStream input, OutputStream output,
			StatusReporter statusReporter,
			JobDataPersister dataPersister,
			Logger jobLogger)
	throws JsonParseException, MissingFieldException, IOException,
		HighProportionOfBadTimestampsException, OutOfOrderRecordsException
	{
		// Oracle's documentation recommends buffering process streams
		BufferedOutputStream bufferedStream = new BufferedOutputStream(output);

		if (dataDescription.transform())
		{
			if (dataDescription.getFormat() == DataFormat.JSON)
			{
				PipeToProcess.transformAndPipeJson(dataDescription, analysisConfig, input,
						bufferedStream, statusReporter,
						dataPersister, jobLogger);
			}
			else
			{
				PipeToProcess.transformAndPipeCsv(dataDescription, analysisConfig, input,
						bufferedStream, statusReporter,
						dataPersister, jobLogger);
			}
		}
		else
		{
			PipeToProcess.pipeCsv(dataDescription, analysisConfig, input,
					bufferedStream, statusReporter, dataPersister,jobLogger);
		}
	}


	/**
	 * Add the timeout schedule for <code>jobId</code>.
	 * On time out it tries to shutdown the job but if the job
	 * is still running it schedules another task to try again in 10
	 * seconds.
	 *
	 * @param jobId
	 * @param timeoutSeconds
	 * @return
	 */
	private ScheduledFuture<?> startShutdownTimer(final String jobId,
			long timeoutSeconds)
	{
		ScheduledFuture<?> scheduledFuture =
				m_ProcessTimeouts.schedule(new Runnable() {
					@Override
					public void run()
					{
						s_Logger.info("Timeout expired stopping process for job:" + jobId);

						try
						{
							boolean notFinished = true;
							while (notFinished)
							{
								try
								{
									finishJob(jobId);
									notFinished = false;
								}
								catch (JobInUseException e)
								{
									int waitSeconds = 10;
									String msg = String.format(
											"Job '%s' is reading data and cannot be shutdown " +
													"Rescheduling shutdown for %d seconds", jobId, waitSeconds);
									s_Logger.warn(msg);

									// wait then try again
									try
									{
										Thread.sleep(waitSeconds * 1000);
									}
									catch (InterruptedException e1)
									{
										s_Logger.warn("Interrupted waiting for job to stop", e);
										return;
									}
								}
							}
						}
						catch (NativeProcessRunException e)
						{
							s_Logger.error(String.format("Error in job %s finish timeout", jobId), e);
						}

					}
				},
				timeoutSeconds,
				TimeUnit.SECONDS);

		return scheduledFuture;
	}

	/**
	 * Stop the process manager by shutting down the executor
	 * service and stop all running processes. Processes won't quit
	 * straight away once the input stream is closed but will stop
	 * soon after once the data has been analysed.
	 */
	public void stop()
	{
		stopAllJobs();
	}

	/**
	 * Shutdown the executor service and stop all running processes
	 */
	private void stopAllJobs()
	{
		s_Logger.info("Stopping all Engine API Jobs");

		// Stop new being scheduled
		m_ProcessTimeouts.shutdownNow();

		s_Logger.info(String.format("Terminating %d active autodetect processes",
				m_JobIdToTimeoutFuture.size()));

		for (String jobId : m_JobIdToTimeoutFuture.keySet())
		{
			boolean notFinished = true;
			while (notFinished)
			{
				try
				{
					try
					{
						finishJob(jobId);
						notFinished = false;
					}
					catch (JobInUseException e)
					{
						int waitSeconds = 10;
						String msg = String.format(
								"Job '%s' is reading data and cannot be shutdown " +
										"Rescheduling shutdown for %d seconds", jobId, waitSeconds);
						s_Logger.info(msg);

						// wait then try again
						try
						{
							Thread.sleep(waitSeconds * 1000);
						}
						catch (InterruptedException e1)
						{
							s_Logger.warn("Interrupted waiting for job to stop", e);
							return;
						}
					}
				}
				catch (NativeProcessRunException e)
				{
					s_Logger.error("Error stopping running job " + jobId);
				}
			}
		}
	}

	/**
	 * Get the analytics version string.
	 *
	 * @return
	 */
	public String getAnalyticsVersion()
	{
		return m_ProcessCtrl.getAnalyticsVersion();
	}


	/**
	 * Get a JSON document containing some of the usage and license info.
	 *
	 * @return The JSON document in string form
	 */
	public String getInfo()
	{
		return m_ProcessCtrl.getInfo();
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


	/**
	 * Close the log appender to release the file descriptor and
	 * remove it from the logger.
	 *
	 * @param logger
	 */
	private void closeLogger(Logger logger)
	{
		Appender appender = logger.getAppender(LOG_FILE_APPENDER_NAME);

		if (appender != null)
		{
			appender.close();
			logger.removeAppender(LOG_FILE_APPENDER_NAME);
		}
	}


	/**
	 * Create the job's logger.
	 *
	 * @param jobId
	 * @return
	 */
	private Logger createLogger(String jobId)
	{
		try
		{
			Logger logger = Logger.getLogger(jobId);
			logger.setAdditivity(false);
			logger.setLevel(Level.DEBUG);

			try
			{
				Path logDir = FileSystems.getDefault().getPath(ProcessCtrl.LOG_DIR, jobId);
				Files.createDirectory(logDir);

				// If we get here then we had to create the directory.  In this
				// case we always want to create the appender because any
				// pre-existing appender will be pointing to a directory of the
				// same name that must have been previously removed.  (See bug
				// 697 in Bugzilla.)
				closeLogger(logger);
			}
			catch (FileAlreadyExistsException e)
			{
			}

			if (logger.getAppender(LOG_FILE_APPENDER_NAME) == null)
			{
				Path logFile = FileSystems.getDefault().getPath(ProcessCtrl.LOG_DIR,
						jobId, "engine_api.log");
				RollingFileAppender fileAppender = new RollingFileAppender(
						new PatternLayout("%d{dd MMM yyyy HH:mm:ss zz} [%t] %-5p %c{3} - %m%n"),
						logFile.toString());

				fileAppender.setName(LOG_FILE_APPENDER_NAME);
				fileAppender.setMaxFileSize("1MB");
				fileAppender.setMaxBackupIndex(9);

				// Try to copy the maximum file size and maximum index from the
				// first rolling file appender of the root logger (there will
				// be one unless the user has meddled with the default config).
				// If we fail the defaults set above will remain in force.
				@SuppressWarnings("rawtypes")
				Enumeration rootAppenders = Logger.getRootLogger().getAllAppenders();
				while (rootAppenders.hasMoreElements())
				{
					try
					{
						RollingFileAppender defaultFileAppender = (RollingFileAppender)rootAppenders.nextElement();
						fileAppender.setMaximumFileSize(defaultFileAppender.getMaximumFileSize());
						fileAppender.setMaxBackupIndex(defaultFileAppender.getMaxBackupIndex());
						break;
					}
					catch (ClassCastException e)
					{
						// Ignore it
					}
				}

				logger.addAppender(fileAppender);
			}

			return logger;
		}
		catch (IOException e)
		{
			Logger logger = Logger.getLogger(ProcessAndDataDescription.class);
			logger.error(String.format("Cannot create logger for job '%s' using default",
					jobId), e);

			return logger;
		}
	}


	public void addAlertObserver(String jobId, AlertObserver ao)
	throws ClosedJobException
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
			s_Logger.info(message);
			throw new ClosedJobException(message, jobId);
		}
	}

	public boolean removeAlertObserver(String jobId, AlertObserver ao)
	{
		ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);
		if (process!= null)
		{
			return process.getResultsReader().removeAlertObserver(ao);
		}

		return false;
	}


}
