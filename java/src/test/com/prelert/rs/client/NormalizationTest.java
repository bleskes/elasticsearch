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
import java.util.Collections;
import java.util.Comparator;
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
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Pagination;


/**
 * Tests the normalisation of the API results and records endpoints.
 * Does not validate the actual normalised values other than to say 
 * that for a given batch job at least 1 anomaly should be 100%.
 * <br/>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/farequote.csv</li>
 * </ol>
 *
 */
public class NormalizationTest implements Closeable
{
	static final private Logger s_Logger = Logger.getLogger(JobsTest.class);
	
	static final public String SYS_CHANGE_NORMALIZATION = "s";
	static final public String UNUSUAL_BEHAVIOUR_NORMALIZATION = "u";
	static final public String BOTH_NORMALIZATIONS = null;
	
	static final long FAREQUOTE_NUM_BUCKETS = 1439;
	
	
	/**
	 * The default base Url used in the test
	 */
	static final public String API_BASE_URL = "http://localhost:8080/engine/v1";
	
	private EngineApiClient m_WebServiceClient;
	
	
	/**
	 * Creates a new http client call {@linkplain #close()} once finished
	 */
	public NormalizationTest()
	{
		m_WebServiceClient = new EngineApiClient();
	}
	
	@Override
	public void close() throws IOException 
	{
		m_WebServiceClient.close();
	}	
	
	
	public String createFarequoteJob(String apiUrl) 
	throws ClientProtocolException, IOException
	{
		Detector d = new Detector();
		d.setFieldName("responsetime");
		d.setByFieldName("airline");
		
		AnalysisConfig ac = new AnalysisConfig();
		ac.setBucketSpan(300L);
		ac.setDetectors(Arrays.asList(d));
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(',');
		dd.setTimeField("time");
		dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");
		
		JobConfiguration config = new JobConfiguration(ac);
		config.setDescription("Farequote normalisation test");
		config.setId("farequote");
		config.setDataDescription(dd);
		
		String jobId = m_WebServiceClient.createJob(apiUrl, config);
		if (jobId == null || jobId.isEmpty())
		{
			s_Logger.error("No Job Id returned by create job");
			test(jobId != null && jobId.isEmpty() == false);
		}
		test(jobId.equals("farequote"));
		
		return jobId;
	}
	
	
	
	/**
	 * Test the system change normalisation for the farequote
	 * data set.  
	 * <ol>
	 * <li>Test that getting all the results at once is the same as 
	 * paging them</li>
	 * <li>Check there is one large anomaly in the data with score 100</li>
	 * <li>Get buckets by date range and check the values match the same
	 * buckets from get all results.</li>
	 * </ol>
	 * 
	 * 
	 * @param baseUrl
	 * @param jobId
	 * @return
	 * @throws IOException
	 */
	public boolean verifyFarequoteSysChangeNormalisation(String baseUrl, String jobId) 
	throws IOException
	{	
		/*
		 * Test that getting all the results at once is the same as 
		 * paging them.
		 */
		Pagination<Bucket> allBuckets = m_WebServiceClient.getBuckets(baseUrl, 
				jobId, SYS_CHANGE_NORMALIZATION, false, 0l, 1500l);
		test(allBuckets.getDocumentCount() == FAREQUOTE_NUM_BUCKETS);
		test(allBuckets.getHitCount() == FAREQUOTE_NUM_BUCKETS);
		
		
		List<Bucket> pagedBuckets = new ArrayList<>();
		long skip = 0, take = 100;
		while (skip < FAREQUOTE_NUM_BUCKETS)
		{
			Pagination<Bucket> buckets = m_WebServiceClient.getBuckets(baseUrl, 
					jobId, SYS_CHANGE_NORMALIZATION, false, skip, take);
			pagedBuckets.addAll(buckets.getDocuments());
			
			skip += take;
		}
		test(pagedBuckets.size() == FAREQUOTE_NUM_BUCKETS);
		
		for (int i=0; i<FAREQUOTE_NUM_BUCKETS; i++)
		{
			test(Double.compare(pagedBuckets.get(i).getAnomalyScore(),
								allBuckets.getDocuments().get(i).getAnomalyScore()) == 0);
			
			test(pagedBuckets.get(i).equals(allBuckets.getDocuments().get(i)));
		}
		
		
		/*
		 * We know there is one large anomaly in the farequote data
		 * with score 100
		 */
		int maxAnomalyScoreCount = 0;
		for (Bucket b : pagedBuckets)
		{
			if (b.getAnomalyScore() >= 100.0)
			{
				maxAnomalyScoreCount++;
			}
		}
		test(maxAnomalyScoreCount == 1);
		
		// the big anomaly is in bucket 772
		test(pagedBuckets.get(771).getAnomalyScore() >= 100.0);
		
		/*
		 * Test get buckets by date range with a time string
		 */
		String [] startDateFormats = new String[] {"2013-01-30T15:10:00Z", "1359558600"};
		String [] endDateFormats = new String[] {"2013-01-31T22:10:00.000+0000", "1359670200"};
		
		for (int i=0; i<startDateFormats.length; i++)
		{
			Pagination<Bucket> byDate = m_WebServiceClient.getBuckets(
					baseUrl, jobId, SYS_CHANGE_NORMALIZATION, false, 0l, 1000l, 
					startDateFormats[i], endDateFormats[i]);

			test(byDate.getDocuments().get(0).getEpoch() == 1359558600l);
			test(byDate.getDocuments().get(byDate.getDocumentCount() -1).getEpoch() == 1359669900l);

			int startIndex = Collections.binarySearch(allBuckets.getDocuments(),
					byDate.getDocuments().get(0), 
					new Comparator<Bucket>() {
				@Override
				public int compare(Bucket o1, Bucket o2) 
				{
					return Long.compare(o1.getEpoch(), o2.getEpoch());
				}
			});

			test(startIndex >= 0);
			for (int j=0; j<byDate.getDocumentCount(); j++)
			{
				test(Double.compare(byDate.getDocuments().get(j).getAnomalyScore(),
						allBuckets.getDocuments().get(j + startIndex).getAnomalyScore()) == 0);
				
				test(byDate.getDocuments().get(j).equals(allBuckets.getDocuments().get(j + startIndex)));
			}
		}
		
		
		return true;
	}
	
	
	/**
	 * Test the unusual behaviour normalisation for the farequote
	 * data set.  
	 * <ol>
	 * <li>Test that getting all the results at once is the same as 
	 * paging them</li>
	 * <li>Check the bucket score is the max anomaly score</li>
	 * <li>At least one bucket has the score = 100</li>
	 * <li>Get buckets by date range and check the values match the same
	 * buckets from get all results.</li>
	 * </ol>
	 * 
	 * @param baseUrl
	 * @param jobId
	 * @return
	 * @throws IOException
	 */
	public boolean verifyFarequoteUnusualNormalisedRecords(String baseUrl, String jobId) 
	throws IOException
	{
		/*
		 * Get the unusual behaviour normalised buckets through the standard endpoint
		 */ 
		Pagination<Bucket> allBuckets = m_WebServiceClient.getBuckets(baseUrl, 
				jobId, UNUSUAL_BEHAVIOUR_NORMALIZATION, true, 0l, 1500l);
		test(allBuckets.getDocumentCount() == allBuckets.getHitCount());
		
		
		/*
		 * Test that getting all the results at once is the same as 
		 * paging them.
		 */		
		List<Bucket> pagedBuckets = new ArrayList<>();
		long skip = 0, take = 98;
		while (skip < allBuckets.getHitCount())
		{
			Pagination<Bucket> buckets = m_WebServiceClient.getBuckets(baseUrl, 
					jobId, UNUSUAL_BEHAVIOUR_NORMALIZATION, true, skip, take);
			pagedBuckets.addAll(buckets.getDocuments());
			
			skip += take;
		}
		test(pagedBuckets.size() == allBuckets.getDocumentCount());
		
		for (int i=0; i<pagedBuckets.size(); i++)
		{
			test(Double.compare(pagedBuckets.get(i).getAnomalyScore(),
								allBuckets.getDocuments().get(i).getAnomalyScore()) == 0);
			
			test(pagedBuckets.get(i).equals(allBuckets.getDocuments().get(i)));
		}
		
		
		/*
		 * The bucket anomaly score is the max unusual score
		 */
		double bucketMax = 0.0;
		for (Bucket bucket: allBuckets.getDocuments())
		{
			for (AnomalyRecord r : bucket.getRecords())
			{
				bucketMax = Math.max(r.getUnusualScore(), bucketMax);
			}
			
			test(bucketMax == bucket.getAnomalyScore());
		}
		
		/*
		 * At least one bucket should have a score of 100
		 */
		int maxAnomalyScoreCount = 0;
		for (Bucket b : pagedBuckets)
		{
			if (b.getAnomalyScore() >= 100.0)
			{
				maxAnomalyScoreCount++;
			}
		}
		test(maxAnomalyScoreCount >= 1);
		
		
		/*
		 * Test get buckets by date range with a time string
		 */
		String [] startDateFormats = new String[] {"2013-01-30T15:10:00Z", "1359558600"};
		String [] endDateFormats = new String[] {"2013-01-31T22:10:00.000+0000", "1359670200"};
		
		for (int i=0; i<startDateFormats.length; i++)
		{
			Pagination<Bucket> byDate = m_WebServiceClient.getBuckets(
					baseUrl, jobId, UNUSUAL_BEHAVIOUR_NORMALIZATION, false, 0l, 1000l, 
					startDateFormats[i], endDateFormats[i]);

			test(byDate.getDocuments().get(0).getEpoch() == 1359558600l);
			test(byDate.getDocuments().get(byDate.getDocumentCount() -1).getEpoch() == 1359669900l);

			int startIndex = Collections.binarySearch(allBuckets.getDocuments(),
					byDate.getDocuments().get(0), 
					new Comparator<Bucket>() {
				@Override
				public int compare(Bucket o1, Bucket o2) 
				{
					return Long.compare(o1.getEpoch(), o2.getEpoch());
				}
			});

			test(startIndex >= 0);
			for (int j=0; j<byDate.getDocumentCount(); j++)
			{
				test(byDate.getDocuments().get(j).equals(allBuckets.getDocuments().get(j + startIndex)));
			}
		}
		
		return true;
	}
	
		
	/**
	 * Get records via the 'records' end point and check the normalised
	 * scores for both unusual & state change.
	 * 
	 * @param baseUrl
	 * @param jobId
	 * @return
	 * @throws IOException
	 */
	public boolean verifyFarequoteRecordsNormalisedForBoth(String baseUrl, String jobId) 
	throws IOException
	{		
		// there are 1332 records in the farequote results
		Pagination<AnomalyRecord> allRecords = m_WebServiceClient.getRecords(
				baseUrl, jobId, BOTH_NORMALIZATIONS, 0l, 1400l);

		/*
		 * Test that getting all the results at once is the same as 
		 * paging them.
		 */
		List<AnomalyRecord> pagedRecords = new ArrayList<>();
		long skip = 0, take = 1000;

		Pagination<AnomalyRecord> page = m_WebServiceClient.getRecords(
				baseUrl, jobId, BOTH_NORMALIZATIONS, skip, take);
		skip += take;
		pagedRecords.addAll(page.getDocuments());

		while (skip < page.getHitCount())
		{
			page = m_WebServiceClient.getRecords(baseUrl, jobId, null, skip, take);
			skip += take;
			pagedRecords.addAll(page.getDocuments());
		}
		
		int recordIndex = 0;
		for (AnomalyRecord record : allRecords.getDocuments())
		{
			test(record.equals(pagedRecords.get(recordIndex)));
			recordIndex++;
		}
		test(recordIndex == pagedRecords.size());
		
		/*
		 * There should be at least one anomaly with score = 100
		 * for each type
		 */
		int maxAnomalyScoreCount = 0;
		int maxUnusualScoreCount = 0;
		for (AnomalyRecord record : pagedRecords)
		{
			if (record.getAnomalyScore() >= 100.0)
			{
				maxAnomalyScoreCount++;
			}
			if (record.getUnusualScore() >= 100.0)
			{
				maxUnusualScoreCount++;
			}
		}
		test(maxAnomalyScoreCount >= 1);
		test(maxUnusualScoreCount >= 1);
		
				
		/*
		 * Test get records by date range with a time string
		 */
		String [] startDateFormats = new String[] {"2013-01-30T15:10:00Z", "1359558600"};
		String [] endDateFormats = new String[] {"2013-01-31T22:10:00.000+0000", "1359670200"};
		for (int i=0; i<startDateFormats.length; i++)
		{
			Pagination<AnomalyRecord> byDate = m_WebServiceClient.getRecords(
					baseUrl, jobId, BOTH_NORMALIZATIONS, 0l, 2000l, 
					startDateFormats[i], endDateFormats[i]);

			test(byDate.getDocuments().get(0).getTimestamp().getTime() == 1359558600l);
			test(byDate.getDocuments().get(byDate.getDocumentCount() -1).getTimestamp().getTime() == 1359669900l);
		}
		
		return true;
	}

	
	/**
	 * Delete all the jobs in the list of job ids
	 * 
	 * @param baseUrl The URL of the REST API i.e. an URL like
	 * 	<code>http://prelert-host:8080/engine/version/</code>
	 * @param jobIds The list of ids of the jobs to delete
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void deleteJobs(String baseUrl, List<String> jobIds) 
	throws IOException, InterruptedException
	{
		for (String jobId : jobIds)
		{
			s_Logger.debug("Deleting job " + jobId);
			
			boolean success = m_WebServiceClient.deleteJob(baseUrl, jobId); 
			if (success == false)
			{
				s_Logger.error("Error deleting job " + baseUrl + "/" + jobId);
			}
		}
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
	
	
	/**
	 * The program takes one argument which is the base Url of the RESTful API.
	 * If no arguments are given then {@value #API_BASE_URL} is used. 
	 * 
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException 
	 */
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
		
				
		NormalizationTest test = new NormalizationTest();
		List<String> jobUrls = new ArrayList<>();
	/*
		File fareQuoteData = new File(prelertTestDataHome + 
				"/engine_api_integration_test/farequote.csv");		
		// Farequote test
		String farequoteJob = test.createFarequoteJob(baseUrl);
		test.m_WebServiceClient.fileUpload(baseUrl, farequoteJob, 
				fareQuoteData, false);
		test.m_WebServiceClient.closeJob(baseUrl, farequoteJob);
*/
		String farequoteJob = "farequote";
//		test.verifyFarequoteSysChangeNormalisation(baseUrl, farequoteJob);
//		test.verifyFarequoteUnusualNormalisedRecords(baseUrl, farequoteJob);
		test.verifyFarequoteRecordsNormalisedForBoth(baseUrl, farequoteJob);
		
		
		//==========================
		// Clean up test jobs
		test.deleteJobs(baseUrl, jobUrls);
	
		test.close();
		
		s_Logger.info("All tests passed Ok");
	}
}
