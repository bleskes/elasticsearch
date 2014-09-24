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


import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.JobDetails;
import com.prelert.job.JobDetails.Counts;
import com.prelert.rs.data.SingleDocument;


/**
 * 
 * Usage data is not available in the API it has to be queried 
 * from Elasticsearch (although the total counts are in the job
 * the hourly data is only in Elasticsearch). 
 * 
 * This test will only work if port 9300 (or whatever the ES port 
 * is) is open  
 */
public class UsageTest implements Closeable
{
	static final private Logger s_Logger = Logger.getLogger(UsageTest.class);

	/**
	 * The default base Url used in the test
	 */
	static final public String API_BASE_URL = "http://localhost:8080/engine/v1";
	
	static final long FLIGHTCENTRE_NUM_BUCKETS = 296;
	static final long FLIGHTCENTRE_NUM_RECORDS = 175910;
	static final long FLIGHTCENTRE_NUM_FIELDS = FLIGHTCENTRE_NUM_RECORDS * 3;
	static final long FLIGHTCENTRE_NUM_PROCESSED_FIELDS = FLIGHTCENTRE_NUM_RECORDS * 2;
	static final long FLIGHTCENTRE_INPUT_BYTES_CSV = 5724086;
			
	static final long FLIGHTCENTRE_INPUT_BYTES_JSON = 17510020;
	
	
	private EngineApiClient m_WebServiceClient;
		
	public UsageTest(String elasticSearchClusterName)
	{
		m_WebServiceClient = new EngineApiClient();		
	}
	
	@Override
	public void close() throws IOException 
	{
		m_WebServiceClient.close();
	}
	
	
	
	public String runFarequoteJob(String apiUrl, File dataFile, boolean isJson,
			boolean compressed) 
	throws ClientProtocolException, IOException
	{
		Detector d = new Detector();
		d.setFieldName("responsetime");
		d.setByFieldName("airline");
		
		AnalysisConfig ac = new AnalysisConfig();
		ac.setBucketSpan(300L);
		ac.setDetectors(Arrays.asList(d));
		
		DataDescription dd = new DataDescription();
		if (isJson)
		{
			dd.setFormat(DataFormat.JSON);
			dd.setTimeField("timestamp");
			dd.setTimeFormat("epoch");			
		}
		else
		{
			dd.setFormat(DataFormat.DELINEATED);
			dd.setFieldDelimiter(',');
			dd.setTimeField("_time");
			dd.setTimeFormat("epoch");	
		}

		
		JobConfiguration config = new JobConfiguration(ac);
		config.setDescription("Farequote usage test");
		config.setDataDescription(dd);
		
		String jobId = m_WebServiceClient.createJob(apiUrl, config);
		if (jobId == null || jobId.isEmpty())
		{
			s_Logger.error("No Job Id returned by create job");
			s_Logger.info(m_WebServiceClient.getLastError().toJson());
			test(jobId != null && jobId.isEmpty() == false);
		}
		
		boolean success = m_WebServiceClient.fileUpload(apiUrl, jobId, dataFile, compressed);
		if (!success)
		{
			s_Logger.error(m_WebServiceClient.getLastError().toJson());
			test(success);
		}
		m_WebServiceClient.closeJob(apiUrl, jobId);
		
		
		return jobId;
	}
	
	
	public void validateFlightCentreCounts(String apiUrl, String jobId,
			boolean isJson) 
	throws IOException
	{
		SingleDocument<JobDetails> job =  m_WebServiceClient.getJob(apiUrl, jobId);
		test(job.isExists());
		
		Counts counts = job.getDocument().getCounts();
		test(counts.getBucketCount() == FLIGHTCENTRE_NUM_BUCKETS);
		test(counts.getInputRecordCount() == FLIGHTCENTRE_NUM_RECORDS);
		test(counts.getInputFieldCount() == FLIGHTCENTRE_NUM_FIELDS);
		
		if (isJson)
		{
			test(counts.getInputBytes() == FLIGHTCENTRE_INPUT_BYTES_JSON);
		}
		else
		{
			// the gzipped data is 1 byte smaller (assuming this is a new line)
			test(counts.getInputBytes() == FLIGHTCENTRE_INPUT_BYTES_CSV || 
					counts.getInputBytes() == FLIGHTCENTRE_INPUT_BYTES_CSV -1);

		}
		
		test(counts.getProcessedRecordCount() == counts.getInputRecordCount());
		test(counts.getProcessedFieldCount() == FLIGHTCENTRE_NUM_PROCESSED_FIELDS);
		
		test(counts.getInvalidDateCount() == 0);
		test(counts.getMissingFieldCount() == 0);
		test(counts.getOutOfOrderTimeStampCount() == 0);		
	}
	
	
	
	
	
	/**
	 * Throws an IllegalStateException if <code>condition</code> is false.
	 * 
	 * @param condition
	 * @throws IllegalStateException
	 */
	public static void test(boolean condition) 
	throws IllegalStateException
	{
		if (condition == false)
		{
			throw new IllegalStateException();
		}
	}
	
	
	
	public static void main(String[] args) 
	throws IOException, InterruptedException
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender(); 		
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n")); 
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
				
		String baseUrl = API_BASE_URL;
		if (args.length > 0)
		{
			baseUrl = args[0];
		}
		
		s_Logger.info("Testing Service at " + baseUrl);
		
		final String prelertTestDataHome = System.getProperty("prelert.test.data.home");
		if (prelertTestDataHome == null)
		{
			s_Logger.error("Error property prelert.test.data.home is not set");
			return;
		}
		
		File flightCentreDataCsv = new File(prelertTestDataHome + 
				"/engine_api_integration_test/flightcentre.csv");	
		File flightCenterDataCsvGzip = new File(prelertTestDataHome + 
				"/engine_api_integration_test/flightcentre.csv.gz");	
		File flightCentreDataJson = new File(prelertTestDataHome + 
				"/engine_api_integration_test/flightcentre.json");		
		
		try (UsageTest test = new UsageTest(ES_CLUSTER_NAME))
		{
			List<String> jobs = new ArrayList<>();
			
			boolean isJson = false;
			boolean isCompressed = false;
			String jobId;
			
			jobId = test.runFarequoteJob(baseUrl, flightCentreDataCsv, isJson, isCompressed);
			test.validateFlightCentreCounts(baseUrl, jobId, isJson);
			
			isCompressed = true;
			jobId = test.runFarequoteJob(baseUrl, flightCenterDataCsvGzip, isJson, isCompressed);
			test.validateFlightCentreCounts(baseUrl, jobId, isJson);
						
			isJson = true;
			isCompressed = false;
			jobId = test.runFarequoteJob(baseUrl, flightCentreDataJson, isJson, isCompressed);
			test.validateFlightCentreCounts(baseUrl, jobId, isJson);
			
			jobs.add(jobId);
			
			
			
			for (String id : jobs)
			{
				test.m_WebServiceClient.deleteJob(baseUrl, id);
			}
		}
		
		s_Logger.info("All tests passed Ok");
		
	}
}


