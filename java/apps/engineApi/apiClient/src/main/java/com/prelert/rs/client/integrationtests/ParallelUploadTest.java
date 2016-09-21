/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.rs.client.integrationtests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.rs.client.datauploader.ConcurrentActionClient;
import com.prelert.rs.client.datauploader.CsvDataRunner;

/**
 * This program tests the case where multiple processes try to write
 * to the same job and checks the correct errors are returned.
 * A background thread is started which opens a conenction to the API
 * server and writes random data to a job then the main thread tries
 * various operations that should fail.
 * The tests are:
 * <ol>
 * <li>Try to close a job when it is being streamed data</li>
 * <li>Try to flush a job when it is being streamed data</li>
 * <li>Try to write to a job when it is being streamed data</li>
 * <li>Try to delete a job when it is being streamed data</li>
 * </ol>
 */
public class ParallelUploadTest
{
	private static final Logger LOGGER = Logger.getLogger(ParallelUploadTest.class);

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

	public static void main(String[] args)
	throws FileNotFoundException, IOException
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender();
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);

        String url = API_BASE_URL;
        if (args.length > 0)
        {
            url = args[0];
            LOGGER.info("Using URL " + url);
        }
        else
        {
            LOGGER.info("Using default URL " + url);
        }

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
				LOGGER.error(e1);
			}
		}

		ConcurrentActionClient concurrentClient =
		            new ConcurrentActionClient(url, jobId, Optional.empty());

		 concurrentClient.run();

		jobRunner.cancel();


		try
		{
			testThread.join();
		}
		catch (InterruptedException e)
		{
			LOGGER.error("Interupted joining test thread", e);
		}


		LOGGER.info("All tests passed Ok");

	}
}
