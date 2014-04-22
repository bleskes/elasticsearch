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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.job.JobInUseException;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.rs.data.ErrorCodes;

/**
 * Manages the native processes channelling data to them and parsing the 
 * results. 
 * 
 * This class registers a JVM shutdown hook once the 
 * purpose of which is to stop any running processes
 */
public class ProcessManager 
{
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
	
	private JobDetailsProvider m_JobDetailsProvider;
	
	private ResultsReaderFactory m_ResultsReaderFactory;
	
	public ProcessManager(JobDetailsProvider jobDetails, 
							ResultsReaderFactory readerFactory)
	{
		m_ProcessCtrl = new ProcessCtrl();
						
		m_JobIdToProcessMap = new ConcurrentHashMap<String, ProcessAndDataDescription>();
		
		m_ProcessTimeouts = Executors.newScheduledThreadPool(1);	
		m_JobIdToTimeoutFuture = new ConcurrentHashMap<String, ScheduledFuture<?>>();
		
		m_JobDetailsProvider = jobDetails;
		m_ResultsReaderFactory = readerFactory;
		
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
	 */
	public boolean dataToJob(String jobId, InputStream input)
	throws UnknownJobException, NativeProcessRunException, MissingFieldException,
		JsonParseException, JobInUseException
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
			// create the new process an restore its state 
			// if it has been saved
			process = createProcess(jobId);
			m_JobIdToProcessMap.put(jobId, process);
		}
		
		// We can't write data if someone is already writing to the process.
		if (process.isInUse())
		{
			String msg = "Cannot write to process whilst it is in use";
			s_Logger.error(msg);
			throw new JobInUseException(jobId, msg, ErrorCodes.NATIVE_PROCESS_RUNNING_ERROR);
		}
				
		// check the process is running, throws if not
		processStillRunning(process, jobId);
		
		// write the data to the process
		try
		{
			process.setInUse(true);
			
			writeToJob(process.getDataDescription(), process.getInterestingFields(),
					input, process.getProcess().getOutputStream(), process.getLogger());
						
			// check there wasn't an error in the input. 
			// throws if there was. 
			processStillRunning(process, jobId);
		}
		catch (IOException e)
		{
			String msg = String.format("Exception writing to process for job %s", jobId);
			s_Logger.error(msg);
			process.getLogger().error(msg);
						
			StringBuilder sb = new StringBuilder(msg)
					.append('\n').append(e.getMessage()).append('\n');
			readProcessErrorOutput(process, sb);
			
			throw new NativeProcessRunException(sb.toString(), 
					ErrorCodes.NATIVE_PROCESS_WRITE_ERROR);
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
		JobDetails job = m_JobDetailsProvider.getJobDetails(jobId);
		
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

		DetectorState state = null;
		if (restoreState)
		{
			state = m_JobDetailsProvider.getPersistedState(jobId);			
		}

		Process nativeProcess = null;
		try
		{
			// if state is null or empty it will be ignored
			// else it is used to restore the models			
			nativeProcess = m_ProcessCtrl.buildProcess(
					ProcessCtrl.AUTODETECT_API, job, state, logger);	
		} 
		catch (IOException e) 
		{
			String msg = "Failed to launch process for job " + job.getId();
			s_Logger.error(msg);
			logger.error(msg, e);
			throw new NativeProcessRunException(msg, 
					ErrorCodes.NATIVE_PROCESS_START_ERROR, e);
		}				

		List<String> analysisFields = job.getAnalysisConfig().analysisFields();

		ProcessAndDataDescription procAndDD = new ProcessAndDataDescription(
				nativeProcess, job.getId(),
				job.getDataDescription(), job.getTimeout(), analysisFields, logger);			

		// Launch results parser in a new thread
		Thread th = new Thread(m_ResultsReaderFactory.newResultsParser(jobId, 
								procAndDD.getProcess().getInputStream(),
								logger),
								"Bucket-Parser");
		th.start();		
		
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
			
			String msg = "Cannot close job as the process is reading data";
			s_Logger.error(msg);
			throw new JobInUseException(jobId, msg, ErrorCodes.NATIVE_PROCESS_RUNNING_ERROR);
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
					
					String msg = String.format("Process exited with code %d.", exitValue);	
					process.getLogger().warn(msg);
					s_Logger.error(msg + " Removing resources for job " + jobId);

					if (exitValue != 0)
					{
						// Read any error output from the process
						StringBuilder sb = new StringBuilder();
						readProcessErrorOutput(process, sb);
						
						throw new NativeProcessRunException(sb.toString(), 
								ErrorCodes.NATIVE_PROCESS_ERROR);		
					}
				}
				catch (IOException ioe)
				{
					String msg = "Exception closing running process input stream";
					s_Logger.warn(msg);
					process.getLogger().warn(msg);
				}
				catch (InterruptedException e) 
				{
					String msg = "Interupted waiting for process to exit";
					s_Logger.debug(msg, e);
					process.getLogger().debug(msg, e);
				}
			}
		}
		catch (NativeProcessRunException e) 
		{
			String msg = "Native process has already exited";
			s_Logger.error(msg);
			process.getLogger().error(msg);
			
			// clean up resources and re-throw
			m_JobIdToProcessMap.remove(jobId);				
			try
			{
				process.getProcess().getOutputStream().close();
			}
			catch (IOException ioe)
			{
				s_Logger.debug("Exception closing stopped process input stream");
			}
			
			throw e;
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
			
			s_Logger.error(msg + "Removing resources for job " + jobId);

			// Read any error output from the process and 
			// add to the returned error. 
			StringBuilder sb = new StringBuilder(msg).append('\n');
			readProcessErrorOutput(process, sb);
						
			throw new NativeProcessRunException(sb.toString(), 
					ErrorCodes.NATIVE_PROCESS_ERROR);
		}
		catch (IllegalThreadStateException e)
		{
			s_Logger.debug("Process is running");
			return true;
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
	 * @param jobLogger
	 * @throws JsonParseException 
	 * @throws MissingFieldException If any fields are missing from the CSV header
	 * @throws IOException 
	 */
	public void writeToJob(DataDescription dataDescription, 
			List<String> analysisFields,
			InputStream input, OutputStream output, Logger jobLogger) 
	throws JsonParseException, MissingFieldException, IOException
	{
		// Oracle's documentation recommends buffering process streams
		BufferedOutputStream bufferedStream = new BufferedOutputStream(output);
		
		if (dataDescription != null && dataDescription.transform())
		{
			if (dataDescription.getFormat() == DataFormat.JSON)
			{
				PipeToProcess.transformAndPipeJson(dataDescription, analysisFields, input, 
						bufferedStream, jobLogger);
			}
			else
			{
				PipeToProcess.transformAndPipeCsv(dataDescription, analysisFields, input, 
						bufferedStream, jobLogger);
			}
		}
		else
		{			
			PipeToProcess.pipeCsv(dataDescription, analysisFields, input, 
					bufferedStream, jobLogger);
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

							m_JobDetailsProvider.setJobFinishedTimeandStatus(jobId, 
									new Date(), JobStatus.FINISHED);
						}
						catch (UnknownJobException | NativeProcessRunException e)
						{
							s_Logger.error("Error in job finish timeout", e);
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
		s_Logger.info("Shutting down the Engine API");
		
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
	 * Get a JSON document containing some of the usage info.
	 * 
	 * @return The JSON document in string form
	 */
	public String getUsageInfo()
	{
		return m_ProcessCtrl.getUsageInfo();
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
	 * Create the job's logger.
	 * 
	 * @param jobId
	 * @return
	 */
	private Logger createLogger(String jobId) 
	{		
		try
		{
			try
			{
				Path logDir = FileSystems.getDefault().getPath(ProcessCtrl.LOG_DIR, jobId);		
				Files.createDirectory(logDir);
			}
			catch (FileAlreadyExistsException e)
			{
			}

			Logger logger = Logger.getLogger(jobId);
			logger.setAdditivity(false);
			logger.setLevel(Level.DEBUG);

			if (logger.getAppender("engine_api_file_appender") == null)
			{
				Path logFile = FileSystems.getDefault().getPath(ProcessCtrl.LOG_DIR,
						jobId, "engine_api.log");
				RollingFileAppender fileAppender = new RollingFileAppender(
						new PatternLayout("%d{dd MMM yyyy HH:mm:ss zz} [%t] %-5p %c{3} - %m%n"),
						logFile.toString());

				fileAppender.setName("engine_api_file_appender");
				fileAppender.setMaxFileSize("1MB");
				fileAppender.setMaxBackupIndex(9);

				logger.addAppender(fileAppender);

				//			ConsoleAppender consoleAppender = new ConsoleAppender(
				//					new PatternLayout("%d{dd MMM yyyy HH:mm:ss zz} [%t] %-5p %c{3} - %m%n"));
				//			
				//			logger.addAppender(consoleAppender);
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

}
