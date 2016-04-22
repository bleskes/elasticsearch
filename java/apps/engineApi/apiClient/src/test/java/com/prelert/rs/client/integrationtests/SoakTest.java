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

package com.prelert.rs.client.integrationtests;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.rs.client.datauploader.JsonDataRunner;

/**
 * Creates a number of producer threads to write data to the Engine REST API
 */
public class SoakTest 
{
	private static final Logger LOGGER = Logger.getLogger(SoakTest.class);
	
	public static final String DEFAULT_CHARSET = "UTF-8";
	
	public static void main(String[] args) 
	throws FileNotFoundException, IOException
	{		
		// configure log4j
		ConsoleAppender console = new ConsoleAppender(); 		
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n")); 
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
		
		
		if (args.length == 0)
		{
			LOGGER.error("This program has one argument: the path to the properties file " 
					+ "containing the settings");
			return;
		}
		
		String filename = args[0];
		Properties props = new Properties();
		props.load(new FileReader(filename));
		
		String serviceUrl = props.getProperty("service_url");
		int numProducers = Integer.parseInt(props.getProperty("num_producers"));
		long numTimeSeries = Long.parseLong(props.getProperty("num_time_series"));
		long pointInterval = Long.parseLong(props.getProperty("time_series_point_interval_secs"));
		long numInteration = Long.parseLong(props.getProperty("num_iterations"));
		long bucketSpan = Long.parseLong(props.getProperty("bucket_span"));
		
		List<Thread> threads = new ArrayList<>();
		for (int i=0; i<numProducers; i++)
		{
			JsonDataRunner test = new JsonDataRunner(serviceUrl, numTimeSeries,
					numInteration, pointInterval, bucketSpan);  
						
			test.createJob("soaktest-" + i);
			
			Thread testThread = new Thread(test);
			testThread.start();
			threads.add(testThread);
		}
		
		for (Thread th : threads)
		{
			try
			{
				th.join();
			}
			catch (InterruptedException e) 
			{
				LOGGER.error("Interupted joining test thread", e);
			}
		}
	}

}
