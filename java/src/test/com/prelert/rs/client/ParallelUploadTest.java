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

package com.prelert.rs.client;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;

import com.prelert.rs.client.datauploader.CsvDataRunner;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.ErrorCode;

/**
 * This program tests the case where multiple processes try to write
 * to the same job and checks the correct errors are returned.
 * A background thread is started which opens a conenction to the API
 * server and writes random data to a job then the main thread tries 
 * various operations that should fail. 
 * The tests are:
 * <ol>
 * <li>Try to close a job when it is being streamed data</li>
 * <li>Try to write to a job when it is being streamed data</li>
 * </ol>
 */
public class ParallelUploadTest 
{
	private final static Logger s_Logger = Logger.getLogger(ParallelUploadTest.class);

	
	public static void main(String[] args) 
	throws FileNotFoundException, IOException
	{		
		if (args.length == 0)
		{
			s_Logger.error("This program has one argument the base Url of the"
					+ " REST API");
			return;
		}
		
		String url = args[0];
		
		CsvDataRunner jobRunner = new CsvDataRunner(url);  
		String jobId = jobRunner.createJob();

		Thread testThread = new Thread(jobRunner);
		testThread.start();
		
		
		// wait for the runner thread to start the upload
		synchronized (jobRunner) 
		{
			try 
			{
				jobRunner.wait();
			}
			catch (InterruptedException e1) 
			{
				s_Logger.error(e1);
			}
		} 
		
		try (EngineApiClient client = new EngineApiClient())
		{
			// cannot close a job when another process is writing to it
			boolean closed = client.closeJob(url, jobId);
			if (closed)
			{
				throw new IllegalStateException("Error closed job while writing to it");
			}
			
			ApiError apiError = client.getLastError();
			if (apiError.getErrorCode() != ErrorCode.NATIVE_PROCESS_RUNNING_ERROR)
			{
				throw new IllegalStateException("Closing Job: Error code should be job in use error");
			}
			
			// cannot write to the job when another process is writing to it
			String data = CsvDataRunner.HEADER + "\n1000,metric,100\n";
			InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			boolean uploaded = client.streamingUpload(url, jobId, is, false);
			
			if (uploaded)
			{
				throw new IllegalStateException("Error wrote to job in use");
			}
			
			apiError = client.getLastError();
			if (apiError.getErrorCode() != ErrorCode.NATIVE_PROCESS_RUNNING_ERROR)
			{
				throw new IllegalStateException("Writing data: Error code should be job in use error");
			}
		}
		
		jobRunner.cancel();
		
		
		try
		{
			testThread.join();
		}
		catch (InterruptedException e) 
		{
			s_Logger.error("Interupted joining test thread", e);
		}		

	}
}
