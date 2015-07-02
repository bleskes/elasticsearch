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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.process.results.ResultsReader;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfigs;

/**
 * The native process, settings objects and output parsers.
 *
 * ErrorReader is a buffered reader connected to the Process's error output.
 * {@link #getLogger} returns a logger that logs to the
 * jobs log directory
 */
public class ProcessAndDataDescription
{
	private final Process m_Process;
	private final DataDescription m_DataDescription;
	private final long m_TimeoutSeconds;
	private final BufferedReader m_ErrorReader;
	private final List<String> m_InterestingFields;

	private ResultsReader m_OutputParser;
	private Thread m_OutputParserThread;

	private Logger m_JobLogger;

	private StatusReporter m_StatusReporter;
	private AnalysisConfig m_AnalysisConfig;
	private TransformConfigs m_Transforms;
	private List<File> m_FilesToDelete;

	private final Semaphore m_ProcessGuard;

	/**
	 * Object for grouping the native process, its data description
	 * and interesting fields and its timeout period.
	 *
	 * @param process The native process.
	 * @param jobId
	 * @param dd
	 * @param timeout
	 * @param interestingFields The list of fields used in the analysis
	 * @param logger The job's logger
	 * @param outputParser
	 * @param dataPersister
	 * @param filesToDelete List of files that should be deleted when the
	 *        process is complete
	 *
	 */
	public ProcessAndDataDescription(Process process, String jobId,
			DataDescription dd,
			long timeout, AnalysisConfig analysisConfig,
			TransformConfigs transforms,
			Logger logger, StatusReporter reporter,
			ResultsReader outputParser,
			List<File> filesToDelete)
	{
		m_Process = process;
		m_DataDescription = dd;
		m_TimeoutSeconds = timeout;

		m_ErrorReader = new BufferedReader(
				new InputStreamReader(m_Process.getErrorStream(),
						StandardCharsets.UTF_8));

		m_AnalysisConfig = analysisConfig;
		m_InterestingFields = analysisConfig.analysisFields();

		m_Transforms = transforms;

		m_StatusReporter = reporter;
		m_JobLogger = logger;

		m_OutputParser = outputParser;

		m_OutputParserThread = new Thread(m_OutputParser, jobId + "-Bucket-Parser");
		m_OutputParserThread.start();

		m_FilesToDelete = filesToDelete;

		m_ProcessGuard = new Semaphore(1);
	}

	public Process getProcess()
	{
		return m_Process;
	}

	public DataDescription getDataDescription()
	{
		return m_DataDescription;
	}


	public Semaphore guard()
	{
	    return m_ProcessGuard;
	}

	/**
	 * The timeout value in seconds.
	 * The process is stopped once this interval has expired
	 * without any new data.
	 */
	public long getTimeout()
	{
		return m_TimeoutSeconds;
	}

	/**
	 * Get the reader attached to the process's error output.
	 * The reader is mutable and <b>not</b> thread safe.
	 * @return
	 */
	public BufferedReader getErrorReader()
	{
		return m_ErrorReader;
	}

	public AnalysisConfig getAnalysisConfig()
	{
		return m_AnalysisConfig;
	}

	public TransformConfigs getTransforms()
	{
		return m_Transforms;
	}


	/**
	 * The list of fields required for the analysis.
	 * The remaining fields can be filtered out.
	 * @return
	 */
	public List<String> getInterestingFields()
	{
		return m_InterestingFields;
	}


	public Logger getLogger()
	{
		return m_JobLogger;
	}

	public StatusReporter getStatusReporter()
	{
		return m_StatusReporter;
	}


	/**
	 * Wait for the output parser thread to finish
	 *
	 * @throws InterruptedException
	 */
	public void joinParserThread()
	throws InterruptedException
	{
		m_OutputParserThread.join();
	}

	public ResultsReader getResultsReader()
	{
		return m_OutputParser;
	}


	public void deleteAssociatedFiles()
	{
		if (m_FilesToDelete == null)
		{
			return;
		}

		for (File fileToDelete : m_FilesToDelete)
		{
			if (fileToDelete.delete() == true)
			{
				m_JobLogger.debug("Deleted file " + fileToDelete.toString());
			}
			else
			{
				m_JobLogger.warn("Failed to delete file " + fileToDelete.toString());
			}
		}
	}
}
