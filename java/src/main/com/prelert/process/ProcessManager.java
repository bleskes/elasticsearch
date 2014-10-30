/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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
 ***********************************************************/
package com.prelert.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import com.prelert.data.ProcessStatus;
import com.prelert.data.ProcessStatus.ProcessRunStatus;


public class ProcessManager
{
	private static final String PRELERT_HOME_TOKEN = "${PRELERT_HOME}";

	/**
	 * Paths could contain spaces, so each argument must be an array element
	 */
	private static final String[] UNIX_START_CMD = { PRELERT_HOME_TOKEN + "/bin/prelert_ctl", "start" };
	private static final String[] UNIX_STOP_CMD = { PRELERT_HOME_TOKEN + "/bin/prelert_ctl", "stop" };
	private static final String[] UNIX_STATUS_CMD = { PRELERT_HOME_TOKEN + "/bin/prelert_ctl", "status" };

	/**
	 * Paths could contain spaces, so each argument must be an array element
	 */
	private static final String[] WINDOWS_START_CMD = { "cmd", "/c", PRELERT_HOME_TOKEN + "\\bin\\prelert_ctl.bat", "start" };
	private static final String[] WINDOWS_STOP_CMD = { "cmd", "/c", PRELERT_HOME_TOKEN + "\\bin\\prelert_ctl.bat", "stop" };
	private static final String[] WINDOWS_STATUS_CMD = { "cmd", "/c", PRELERT_HOME_TOKEN + "\\bin\\prelert_ctl.bat", "status" };

	private static Logger s_Logger = Logger.getLogger(ProcessManager.class);
	
	private ProcessMonitor m_ProcessMonitor;
	

	/**
	 * Creates the new ProcessManager monitoring loggingevents
	 * on the default port.
	 * @throws IOException
	 */
	public ProcessManager() throws IOException
	{
		this(ProcessMonitor.DEFAULT_LOG_PORT);
	}


	/**
	 * Creates the new ProcessManager monitoring loggingevents
	 * on the given port.
	 * @param port
	 * @throws IOException
	 */
	public ProcessManager(int port) throws IOException
	{
		try
		{
			m_ProcessMonitor = new ProcessMonitor(port);
		}
		catch (IOException e)
		{
			s_Logger.fatal("Could not create ProcessMonitor. Error: " + e);
			throw e;
		}
	}
	
	
	/**
	 * Start the process monitor.
	 */
	public void startProcessMonitor()
	{
		m_ProcessMonitor.start();
	}
	
	/**
	 * Stops the process monitor thread.
	 * Returns immediately without waiting for the thread
	 * to join.
	 */
	public void stopProcessMonitor()
	{
		m_ProcessMonitor.quit();
	}
	
	/**
	 * Starts the Prelert processes by calling process platform specific
	 * process start script.
	 *
	 * @return true if the start up script executed successfully.
	 */
	static public boolean startProcesses()
	{
		String[] command;
		if (SystemUtils.IS_OS_WINDOWS)
		{
			command = WINDOWS_START_CMD;
		}
		else if (SystemUtils.IS_OS_UNIX)
		{
			command = UNIX_START_CMD;
		}
		else
		{
			throw new UnsupportedOperationException("Unknown platform " + SystemUtils.OS_NAME);
		}

		// Run the command
		return runCommand(command) == 0;
	}


	/**
	 * Stops the Prelert processes by calling process platform specific
	 * process stop script.
	 *
	 * @return true if the shutdown script executed successfully.
	 */
	static public boolean stopProcesses()
	{
		String[] command;
		if (SystemUtils.IS_OS_WINDOWS)
		{
			command = WINDOWS_STOP_CMD;
		}
		else if (SystemUtils.IS_OS_UNIX)
		{
			command = UNIX_STOP_CMD;
		}
		else
		{
			throw new UnsupportedOperationException("Unknown platform " + SystemUtils.OS_NAME);
		}
		
		// Run the command
		return runCommand(command) == 0;
	}


	/**
	 * Run the command string and wait for it to return.
	 * Parses the cmd string and replaces the PRELERT_HOME_TOKEN with
	 * the actual value of ${PRELERT_HOME}.
	 *
	 * @param cmd
	 * @return The result of running the command or -1 if the command
	 * 			cannot be executed.
	 */
	static private int runCommand(String[] cmd)
	{
		String prelertHome = getPrelertHome();
		String[] cmdCopy = new String[cmd.length];

		StringBuilder debugStr = new StringBuilder();
		for (int count = 0; count < cmd.length; ++count)
		{
			cmdCopy[count] = cmd[count].replace(PRELERT_HOME_TOKEN, prelertHome);

			debugStr.append(" arg");
			debugStr.append(count);
			debugStr.append(" = \"");
			debugStr.append(cmdCopy[count]);
			debugStr.append('\"');
		}
		s_Logger.info("About to run:" + debugStr);

		try
		{
			java.lang.Process process = Runtime.getRuntime().exec(cmdCopy);
			try
			{
				InputStream stdout = process.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
				
				InputStream stderr = process.getErrorStream();
				BufferedReader error = new BufferedReader(new InputStreamReader(stderr));
				
				String line;
				while ((line = reader.readLine()) != null)
				{
					s_Logger.debug(line);
				}
				
				while ((line = error.readLine()) != null)
				{
					s_Logger.debug(line);
				}

				int result = process.waitFor();
				s_Logger.info("Process return code: " + result);

				return result;
			}
			catch (InterruptedException e)
			{
				s_Logger.error("Interrupted waiting for finish of command" + debugStr);
			}
		}
		catch (IOException e)
		{
			s_Logger.error("Exception executing command" + debugStr + '\n' + e);
		}
		
		return -1;
	}
	
	
	/**
	 * Run the command string and wait for it to return.
	 * The output of the command is returned in a BufferedReader.
	 *
	 * Parses the cmd string and replaces the PRELERT_HOME_TOKEN with
	 * the actual value of ${PRELERT_HOME}.
	 *
	 * @param cmd
	 * @return a reader object or <code>null</code>
	 */
	private BufferedReader runCommandGetOutput(String[] cmd)
	{
		String prelertHome = getPrelertHome();
		String[] cmdCopy = new String[cmd.length];

		StringBuilder debugStr = new StringBuilder();
		for (int count = 0; count < cmd.length; ++count)
		{
			cmdCopy[count] = cmd[count].replace(PRELERT_HOME_TOKEN, prelertHome);

			debugStr.append(" arg");
			debugStr.append(count);
			debugStr.append(" = \"");
			debugStr.append(cmdCopy[count]);
			debugStr.append('\"');
		}
		s_Logger.info("About to run:" + debugStr);

		try
		{
			java.lang.Process process = Runtime.getRuntime().exec(cmd);
			try
			{
				InputStream stdout = process.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

				int result = process.waitFor();
				s_Logger.info("Process return code: " + result);

				return reader;
			}
			catch (InterruptedException e)
			{
				s_Logger.error("Interrupted waiting for finish of command" + debugStr);
			}
		}
		catch (IOException e)
		{
			s_Logger.error("Exception executing command" + debugStr + '\n' + e);
		}

		return null;
	}
	
		
	/**
	 * Returns true if any of the processes have had a fatal error.
	 * @return
	 */
	public boolean hasFatalError()
	{
		return m_ProcessMonitor.hasFatalError();
	}


	/**
	 * If a fatal error has occurred this returns the error message.
	 *
	 * @return
	 */
	public String getFatalErrorMessage()
	{
		return m_ProcessMonitor.getFatalErrorMessage();
	}


	/**
	 * Returns the status of the named process.
	 * If the status is not known then the 'prelert_ctl status' command
	 * is run and its output parsed.
	 *
	 * @param processName
	 * @return
	 */
	public ProcessStatus getProcessStatus(String processName)
	{
		ProcessStatus processStatus = m_ProcessMonitor.getProcessStatus(processName);
		
		if (processStatus.getStatus().equals(ProcessRunStatus.UNKNOWN))
		{
			List<ProcessStatus> statusList = getProcessesStatusFromPrelertCtl();
			
			for (ProcessStatus status : statusList)
			{
				if (status.getProcessName().equals(processName))
				{
					processStatus = status;
					break;
				}
			}
			
		}
		
		return processStatus;
	}
	
	
	/**
	 * Return a the status of all installed processes.
	 * First ask for the status from prelert_ctl then
	 * update with the specific status if a log message
	 * has been received from that process.
	 *
	 * @return
	 */
	public List<ProcessStatus> getProcessesStatus()
	{
		List<ProcessStatus> statusList = getProcessesStatusFromPrelertCtl();
		
		for (int i=0; i<statusList.size(); ++i)
		{
			ProcessStatus cachedStatus = m_ProcessMonitor.getProcessStatus(statusList.get(i).getProcessName());
			if (!cachedStatus.getStatus().equals(ProcessRunStatus.UNKNOWN))
			{
				statusList.set(i, cachedStatus);
			}
		}
		
		return statusList;
	}	
	
	
	/**
	 * Returns the status of all installed Prelert processes
	 * using the 'prelert_ctl status' command.
	 *
	 * @return
	 */
	private List<ProcessStatus> getProcessesStatusFromPrelertCtl()
	{
		// try and get the status from the status script.
		String[] command;
		if (SystemUtils.IS_OS_WINDOWS)
		{
			command = WINDOWS_STATUS_CMD;
		}
		else if (SystemUtils.IS_OS_UNIX)
		{
			command = UNIX_STATUS_CMD;
		}
		else
		{
			throw new UnsupportedOperationException("Unknown platform " + SystemUtils.OS_NAME);
		}
		
		BufferedReader reader = runCommandGetOutput(command);
		if (reader != null)
		{
			List<ProcessStatus> statusList;
			if (SystemUtils.IS_OS_WINDOWS)
			{
				statusList = parseStatusOutputWindows(reader);
			}
			else if (SystemUtils.IS_OS_UNIX)
			{
				statusList = parseStatusOutputUnix(reader);
			}
			else
			{
				throw new UnsupportedOperationException("Unknown platform " + SystemUtils.OS_NAME);
			}
			
			return statusList;
		}
		else
		{
			return Collections.emptyList();
		}
	}


	/**
	 * Parses the output from the 'prelert_ctl status' command.
	 *
	 * Output is formated like:
	 * /path/to/process/processname: up (pid 29709) 1 seconds
	 * or
	 * /path/to/process/processname: supervise not running
	 *
	 */
	private List<ProcessStatus> parseStatusOutputUnix(BufferedReader processOutput)
	{
		List<ProcessStatus> results = new ArrayList<ProcessStatus>();
		
		try
		{
			String line;
			while ((line = processOutput.readLine()) != null)
			{
				if (line.isEmpty())
				{
					continue;
				}
				
				String [] split = line.split(": ");
				if (split.length != 2)
				{
					continue;
				}

				ProcessStatus status = new ProcessStatus();
				String processName = new File(split[0]).getName();
				status.setProcessName(processName);
				status.setMessage("Status from prelert_ctl");
				status.setTimeStamp(new Date());
				
				if (split[1].startsWith("up"))
				{
					status.setStatus(ProcessRunStatus.RUNNING);
				}
				else
				{
					status.setStatus(ProcessRunStatus.STOPPED);
				}
				
				results.add(status);
			}
		}
		catch (IOException e)
		{
			s_Logger.info("Error processing Status output");
		}
		
		return results;
	}
	
	
	/**
	 * Parse the output of the windows prelert status command
	 *
	 * SERVICE_NAME: PrelertTsPointWriter
	 *   TYPE               : 10  WIN32_OWN_PROCESS
	 *   STATE              : 1  STOPPED
	 *   WIN32_EXIT_CODE    : 0  (0x0)
	 *   SERVICE_EXIT_CODE  : 0  (0x0)
	 *   CHECKPOINT         : 0x0
	 *   WAIT_HINT          : 0x0
	 *
	 * @param processOutput
	 * @return
	 */
	private List<ProcessStatus> parseStatusOutputWindows(BufferedReader processOutput)
	{
		final String SERVICE_NAME = "SERVICE_NAME:";
		final String STATE = "STATE";
		
		List<ProcessStatus> results = new ArrayList<ProcessStatus>();
		
		try
		{
			String line;
			while ((line = processOutput.readLine()) != null)
			{
				if (line.startsWith(SERVICE_NAME))
				{
					String processName = line.substring(SERVICE_NAME.length()).trim();

					line = processOutput.readLine();
					line = processOutput.readLine();
					if (!line.startsWith(STATE))
					{
						s_Logger.error("Error parsing status output: " + line);
						continue;
					}
					
					String[] split = line.split(" ");
					if (split.length == 0)
					{
						s_Logger.error("Error parsing status output: " + line);
						continue;
					}
					String status = split[split.length-1];
					
					ProcessStatus processStatus = new ProcessStatus();
					processStatus.setProcessName(processName);
					processStatus.setMessage("Status from prelert_ctl");
					processStatus.setTimeStamp(new Date());
					
					if (status.equals("RUNNING"))
					{
						processStatus.setStatus(ProcessRunStatus.RUNNING);
					}
					else if (status.equals("STOPPED"))
					{
						processStatus.setStatus(ProcessRunStatus.STOPPED);
					}
					else if (status.equals("PAUSED"))
					{
						processStatus.setStatus(ProcessRunStatus.PAUSED);
					}
					
					results.add(processStatus);
				}
			}
		}
		catch (IOException e)
		{
			s_Logger.info("Error processing Status output");
		}
		
		
		return results;		
	}
	
	
	/**
	 * Returns the absolute path of $PRELERT_HOME
	 * @return
	 */
	static public String getPrelertHome()
	{
		String prelertHome =  System.getProperty("prelert.home");
		if (prelertHome == null)
		{
			String cwd = System.getProperty("user.dir");
			prelertHome = cwd + File.separator + "..";
		}
		
		return prelertHome;
	}
}
