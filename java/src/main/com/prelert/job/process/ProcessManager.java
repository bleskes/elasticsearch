/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.job.NativeProcessRunException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.process.ProcessCtrl;

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
	 * it stopped normally and NOT_FOUND means the manager is not tracking
	 * that job.
	 */
	public enum ProcessStatus {IN_USE, COMPLETED, NOT_FOUND};
	
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
	 *  
	 * @param jobId
	 * @param input 
	 * @return True if successful or false if the data can't be written because
	 * it is already processing some data
	 * @throws UnknownJobException 
	 * @throws NativeProcessRunException If there is a problem creating a new process
	 */
	public boolean dataToJob(String jobId, InputStream input)
	throws UnknownJobException, NativeProcessRunException
	{
		// stop the timeout 
		ScheduledFuture<?> future = m_JobIdToTimeoutFuture.remove(jobId);
		if (future != null)
		{
			future.cancel(false);
		}
		
		ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);		
		
		if (process == null)
		{
			// create the new process an restore its state 
			// if it has been saved
			process = createProcess(jobId);
		}
		
		// We can't write data if someone is already writing to the process.
		if (process.isInUse())
		{
			s_Logger.error("Cannot write to process whilst it is in use");
			return false;
		}
				
		// check the process is running, throws if not
		processStillRunning(process, jobId);
		
		
		// write the data to the process
		try
		{			
			process.setInUse(true);
			
			if (process.getDataDescription() != null &&
					process.getDataDescription().transform())
			{
				if (process.getDataDescription().getFormat() == DataFormat.JSON)
				{
					transformAndPipeJson(process.getDataDescription(), input, 
							process.getProcess().getOutputStream());
				}
				else 
				{				
					transformAndPipeCsv(process.getDataDescription(), input, 
							process.getProcess().getOutputStream());
				}				
			}
			else
			{
				// no transform write the data straight through
				pipe(input, process.getProcess().getOutputStream());
			}
			
			// check there wasn't an error in the input. 
			// throws if there was. 
			processStillRunning(process, jobId);
		}
		catch (IOException e)
		{
			String msg = String.format("Exception writing to process for job %s", jobId);
			s_Logger.error(msg, e);
			
			StringBuilder sb = new StringBuilder(msg).append('\n');
			readProcessErrorOutput(process, sb);
			
			throw new NativeProcessRunException(sb.toString(), e);
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
	 * @throws NativeProcessRunException
	 */	
	private ProcessAndDataDescription createProcess(JobDetails job, 
										boolean restoreState)
	throws UnknownJobException, NativeProcessRunException
	{
		String jobId = job.getId();
		
		ProcessAndDataDescription procAndDD = null;
		try 
		{
			DetectorState state = null;
			if (restoreState)
			{
				state = m_JobDetailsProvider.getPersistedState(jobId);			
			}
			
			// if state is null or empty it will be ignored
			// else it is used to restore the models			
			Process nativeProcess = m_ProcessCtrl.buildProcess(
					ProcessCtrl.AUTODETECT_API, job, state);				

			procAndDD = new ProcessAndDataDescription(nativeProcess, 
					job.getDataDescription(), job.getTimeout());			
		} 
		catch (IOException e) 
		{
			s_Logger.error("Failed to launch process for job " + job.getId(), e);
			throw new NativeProcessRunException("Error starting the native process "
					+ "for job " + job.getId(), e);
		}				

		m_JobIdToProcessMap.put(jobId, procAndDD);
		ScheduledFuture<?> future = startShutdownTimer(jobId, job.getTimeout());
		
		m_JobIdToTimeoutFuture.put(jobId, future);

		// Launch results parser in a new thread
		Thread th = new Thread(m_ResultsReaderFactory.newResultsParser(jobId, 
								procAndDD.getProcess().getInputStream()));
		th.start();		
		
		s_Logger.debug("Created process for job " + jobId);
		
		return procAndDD;
	}

	
	/**
	 * Stop the running process.
	 * Closing the stream into the native process causes the process
	 * to terminate its IO loop and stop.<br/>
	 * The return value is based on the status of the native process if it
	 * cannot be found ProcessStatus.NOT_FOUND is returned else 
	 * ProcessStatus.IN_USE if the process is currently processing data in which
	 * case this function should be tried again after a wait period. If the
	 * process is stopped successfully ProcessStatus.COMPLETED is returned.
	 * 
	 * @param jobId
	 * @return The process finished status
	 * @throws UnknownJobException If the job is already finished or cannot be 
	 * found
	 * @throws NativeProcessRunException 
	 */
	public ProcessStatus finishJob(String jobId) 
	throws UnknownJobException, NativeProcessRunException 
	{
		s_Logger.info("Finishing job " + jobId);
		
		ScheduledFuture<?> future = m_JobIdToTimeoutFuture.get(jobId);
		if (future == null)
		{
			s_Logger.error("No timeout future for job '" + jobId + "'");
		}
		else
		{
			future.cancel(false);			
		}		
		m_JobIdToTimeoutFuture.remove(jobId);
		
		ProcessAndDataDescription process = m_JobIdToProcessMap.get(jobId);	
		if (process == null)
		{
			s_Logger.error("No job with id '" + jobId + "' to shutdown");
			throw new UnknownJobException(jobId, "Cannot finish job");
		}
		
		try
		{
			// check the process is running, throws if not
			processStillRunning(process, jobId);

			if (process.isInUse())
			{
				s_Logger.error("Cannot finish job while it is reading data");
				return ProcessStatus.IN_USE;
			}

			try 
			{
				// stop the process and remove from map
				process.getProcess().getOutputStream().close();
			} 
			catch (IOException e) 
			{
				s_Logger.error("Error closing process stream", e);
				// return process completed as if it's outputstream is 
				// closed than it has stopped
			}
		}
		finally
		{
			m_JobIdToProcessMap.remove(jobId);		
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
			s_Logger.error(msg + "Removing resources for job " + jobId);

			// Read any error output from the process and 
			// add to the returned error. 
			StringBuilder sb = new StringBuilder(msg).append('\n');
			readProcessErrorOutput(process, sb);
			
			throw new NativeProcessRunException(sb.toString());
		}
		catch (IllegalThreadStateException e)
		{
			s_Logger.debug("Process is running");
			return true;
		}
	}
	
	/**
	 * Read the contents from input stream and write to output stream.
	 * Flushes the outputstream once all data is written.
	 * 
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	private void pipe(InputStream is, OutputStream os) 
	throws IOException 
	{
		int n;
		byte[] buffer = new byte[131072];
		while((n = is.read(buffer)) > -1) 
		{
			os.write(buffer, 0, n);
		}		
		os.flush();
	}
	
	private void transformAndPipeCsv(DataDescription dd, InputStream is, OutputStream os)
	throws IOException 
	{
		String timeField = dd.getTimeField();
		if (timeField == null)
		{
			timeField = ProcessCtrl.DEFAULT_TIME_FIELD;
		}
		
		char delimiter = ProcessCtrl.DEFAULT_DELIMITER; 
		if (dd.getFieldDelimiter() != null)
		{
			delimiter = dd.getFieldDelimiter().charAt(0);
		}
		
		CsvPreference csvPref = new CsvPreference.Builder(
				DataDescription.QUOTE_CHAR,
				delimiter,
				new String(new char [] {DataDescription.LINE_ENDING})).build();	
		
		try (CsvListReader csvReader = new CsvListReader(new InputStreamReader(is), csvPref))
		{

			String [] header = csvReader.getHeader(true);
			int timeFieldIndex = -1;
			for (int i=0; i<header.length; i++)
			{
				if (timeField.equals(header[i]))
				{
					timeFieldIndex = i;
					break;
				}		
			}

			if (timeFieldIndex < 0)
			{
				String message = String.format("Cannot find time field '%s' in CSV header '%s'",
						timeField, header);
				s_Logger.error(message);
				throw new IOException(message);
			}
			
			try (CsvListWriter csvWriter = new CsvListWriter(new OutputStreamWriter(os), csvPref))
			{				
				csvWriter.writeHeader(header);

				DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());

				List<String> line;
				while ((line = csvReader.read()) != null)
				{
					try
					{
						String epoch =  new Long(dateFormat.parse(line.get(timeFieldIndex)).getTime() / 1000).toString();
						line.set(timeFieldIndex, epoch);

						csvWriter.write(line);
					}
					catch (ParseException pe)
					{
						String message = String.format("Cannot parse date '%s' with format string '%s'",
								line.get(timeFieldIndex), dd.getTimeFormat());

						s_Logger.error(message);
					}		
				}
			}
		}
	}
	
	
	/**
	 * Relies on all the java objects being uniform i.e. having the 
	 * same fields in the same order.
	 * Flushes the outputstream once all data is written.
	 * 
	 * @param dd 
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	private void transformAndPipeJson(DataDescription dd, InputStream is, 
			OutputStream os)
	throws IOException 
	{
		String timeField = dd.getTimeField();
		if (timeField == null)
		{
			timeField = ProcessCtrl.DEFAULT_TIME_FIELD;
		}
		
		JsonParser parser = new JsonFactory().createParser(is);
		
		JsonToken token = parser.nextToken();
		// if start of an array ignore it, we expect an array of buckets
		if (token == JsonToken.START_ARRAY)
		{
			token = parser.nextToken();
			s_Logger.debug("JSON starts with an array");
		}

		if (token != JsonToken.START_OBJECT)
		{
			s_Logger.error("Expecting Json Start Object token");
			throw new IOException(
					"Invalid JSON should start with an array of objects or an object."
					+ "Bad token = " + token);
		}
		

		if (dd.getTimeFormat() != null)
		{
			pipeJsonAndTransformTime(parser, os, dd);
		}
		else
		{
			pipeJson(parser, os);
		}
		os.flush();
		
		parser.close();	
	}
	
	/**
	 * Parse the Json objects and write to output stream.
	 * 
	 * @param parser
	 * @param os
	 * @throws IOException 
	 * @throws JsonParseException 
	 */
	private void pipeJson(JsonParser parser, OutputStream os) 
	throws JsonParseException, IOException
	{	
		StringBuilder line = new StringBuilder();
		// first extract the field names and write those as the header

		StringBuilder header = new StringBuilder();
		
		JsonToken token = parser.nextToken();
		while (token != JsonToken.END_OBJECT)
		{
			if (token == JsonToken.FIELD_NAME)
			{
				header.append(parser.getCurrentName())
					.append(DataDescription.DEFAULT_DELIMITER);
				
				token = parser.nextToken();
				line.append(parser.getText())
				.	append(DataDescription.DEFAULT_DELIMITER);
			}
			token = parser.nextToken();
		}		
		// overwrite the extra delimiter char with a newline
		header.setCharAt(header.length() -1, DataDescription.LINE_ENDING);
		line.setCharAt(line.length() -1, DataDescription.LINE_ENDING);

		
		os.write(header.toString().getBytes("UTF-8"));
		os.write(line.toString().getBytes("UTF-8"));
		line.delete(0, line.length());
		
		// now send the rest of the data
		int recordCount = 1;
		token = parser.nextToken();
		while (token == JsonToken.START_OBJECT)
		{			
			while (token != JsonToken.END_OBJECT)
			{
				if (token == JsonToken.FIELD_NAME)
				{
					token = parser.nextToken();
					line.append(parser.getText())
						.append(DataDescription.DEFAULT_DELIMITER);
				}
				token = parser.nextToken();
			}
			// remove the extra delimiter char
			line.setCharAt(line.length() -1, DataDescription.LINE_ENDING);
			
			os.write(line.toString().getBytes("UTF-8"));

			line.delete(0, line.length());
			recordCount++;
			token = parser.nextToken();
		}
		os.flush();
		
		s_Logger.info("Transferred " + recordCount + " Json records to autodetect.");
	}	
	
	/**
	 * Parse the Json objects convert the timestamp to epoch time
	 * and write to output stream. This shares a lot of code with
	 * {@linkplain #pipeJson(JsonParser, OutputStream)} repeated
	 * for the sake of efficiency.
	 * 
	 * @param parser
	 * @param os
	 * @param dd
	 * @throws IOException 
	 * @throws JsonParseException 
	 */
	private void pipeJsonAndTransformTime(JsonParser parser, OutputStream os,
			DataDescription dd) 
	throws JsonParseException, IOException
	{	
		DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());
		String timeField = dd.getTimeField();
		if (timeField == null)
		{
			timeField = ProcessCtrl.DEFAULT_TIME_FIELD;
		}
		
		
		StringBuilder line = new StringBuilder();
		// first extract the field names and write those as the header
		StringBuilder header = new StringBuilder();
		
		JsonToken token = parser.nextToken();
		while (token != JsonToken.END_OBJECT)
		{
			if (token == JsonToken.FIELD_NAME)
			{
				String fieldName = parser.getCurrentName();
				header.append(fieldName)
					.append(DataDescription.DEFAULT_DELIMITER);
				
				token = parser.nextToken();
				String value = parser.getText();
				
				if (fieldName.equals(timeField))
				{
					try 
					{
						value = new Long(dateFormat.parse(value).getTime() / 1000).toString();
					}
					catch (ParseException e) 
					{
						s_Logger.error("Cannot parse '" + value + 
								"' as a date using format string '" + 
								dd.getTimeFormat() + "'");
					}
				}
				
				line.append(value)
				.	append(DataDescription.DEFAULT_DELIMITER);
			}
			token = parser.nextToken();
		}		
		// overwrite the extra delimiter char with a newline
		header.setCharAt(header.length() -1, DataDescription.LINE_ENDING);
		line.setCharAt(line.length() -1, DataDescription.LINE_ENDING);

		
		os.write(header.toString().getBytes("UTF-8"));
		os.write(line.toString().getBytes("UTF-8"));
		line.delete(0, line.length());
		
		// now send the rest of the data
		int recordCount = 1;
		token = parser.nextToken();
		while (token == JsonToken.START_OBJECT)
		{			
			while (token != JsonToken.END_OBJECT)
			{
				if (token == JsonToken.FIELD_NAME)
				{
					token = parser.nextToken();
					line.append(parser.getText())
						.append(DataDescription.DEFAULT_DELIMITER);
				}
				token = parser.nextToken();
			}
			// remove the extra delimiter char
			line.setCharAt(line.length() -1, DataDescription.LINE_ENDING);
			
			os.write(line.toString().getBytes("UTF-8"));

			line.delete(0, line.length());
			recordCount++;
			token = parser.nextToken();
		}
		os.flush();
		
		s_Logger.info("Transferred " + recordCount + " Json records to autodetect." );
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
							ProcessStatus status = finishJob(jobId);
							if (status == ProcessStatus.IN_USE)
							{
								// reschedule the shutdown
								startShutdownTimer(jobId, 10);
							}		
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
		// Stop new being scheduled
		m_ProcessTimeouts.shutdownNow();
		
		s_Logger.info(String.format("Terminating %d active autodetect processes", 
				m_JobIdToTimeoutFuture.size()));
		
		for (String jobId : m_JobIdToTimeoutFuture.keySet())
		{
			try 
			{
				finishJob(jobId);
			}
			catch (UnknownJobException | NativeProcessRunException e) 
			{
				s_Logger.error("Error stopping running job " + jobId);
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
			String line;
			while ((line = process.getErrorReader().readLine()) != null)
			{
				sb.append(line).append('\n');
			}
		}
		catch (IOException e)
		{
			s_Logger.warn("Exception thrown reading the native processes "
					+ "error output", e);
		}
		return sb;
	}
}
