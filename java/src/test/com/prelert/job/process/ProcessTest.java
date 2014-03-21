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


import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.persistence.elasticsearch.ElasticSearchPersister;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.parsing.AutoDetectParseException;
import com.prelert.rs.data.parsing.AutoDetectResultsParser;

/**
 * Simple program to launch a Prelert autodetect process pipe data into it
 * and parse the results.  
 */
public class ProcessTest 
{
	static private class ReadOutput implements Runnable
	{
		private InputStream m_Stream;		
		private AutoDetectResultsParser.BucketsAndState m_BucketsAndState;
		
		public ReadOutput(InputStream stream)
		{
			m_Stream = stream;
		}
		
		public AutoDetectResultsParser.BucketsAndState getParsedData()
		{
			return m_BucketsAndState;
		}
		
		public void run() 
		{			
			Node node = nodeBuilder().client(true).node();
			Client client = node.client();
			
			final String JOBID = "development";
			
			ElasticSearchPersister persister = new ElasticSearchPersister(JOBID, client);
			
			try 
			{
				m_BucketsAndState = AutoDetectResultsParser.parseResults(m_Stream, 
						persister);				
			}
			catch (JsonParseException e) 
			{
				e.printStackTrace();
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			catch (AutoDetectParseException e) 
			{
				e.printStackTrace();
			}
			

			System.out.println("-----------------------");
			System.out.println("Run Complete");
		}		
	}
	
	public static void main(String[] args) throws IOException 
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender(); 		
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n")); 
		console.setThreshold(Level.DEBUG);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
		  
		if (args.length < 1)
		{
			System.out.println("A input file must be specified");
			return;
		}
		String filePath = args[0];
		System.out.println("Using file " + filePath);
				
		
		// Open input
		FileInputStream fs = new FileInputStream(filePath);
		
		Detector detector = new Detector();
		detector.setFieldName("hitcount");
		detector.setByFieldName("url");
		List<Detector> d = new ArrayList<>();
		d.add(detector);
		AnalysisConfig conf = new AnalysisConfig();
		conf.setDetectors(d);

		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter("\t");
		
		String jobId = "TestJob";
		JobDetails job = new JobDetails(jobId, 
					new JobConfiguration.JobConfigurationBuilder(conf)
				.dataDescription(dd)
				.build());
		
				
		// Start autodetect 
		ProcessCtrl ctrl = new ProcessCtrl();		
		Process pr = ctrl.buildProcess(ProcessCtrl.AUTODETECT_API, job);

		// thread to read the autodetect output
		ReadOutput read = new ReadOutput(pr.getInputStream());
		Thread th = new Thread(read);
		th.start();
		
		// pipe file into autodetect
		pipe(fs, pr.getOutputStream());
				
		try 
		{
			th.join();
		}
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	
		for (Bucket b : read.getParsedData().getBuckets())
		{
			System.out.println(b.getAnomalyScore());
		}
		
		
		System.out.println("Job Finished");
	}
			
	private static void pipe(InputStream is, OutputStream os) throws IOException 
	{
		int n;
		byte[] buffer = new byte[131072];
		while((n = is.read(buffer)) > -1) 
		{
			os.write(buffer, 0, n);   // Don't allow any extra bytes to creep in, final write
		}
			 
		os.close ();
	}
}
