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
import java.util.Date;
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
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;
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
	static final public String BOTH_NORMALIZATIONS = "both"; 
	
	static final long FAREQUOTE_NUM_BUCKETS = 1439;
	
	static final String TEST_JOB_ID = "farequote-norm-test";
	
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
		config.setId(TEST_JOB_ID);
		config.setDataDescription(dd);
		
		String jobId = m_WebServiceClient.createJob(apiUrl, config);
		if (jobId == null || jobId.isEmpty())
		{
			s_Logger.error("No Job Id returned by create job");
			test(jobId != null && jobId.isEmpty() == false);
		}
		test(jobId.equals(TEST_JOB_ID));
		
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
	public boolean verifyFarequoteSysChangeNormalisedBuckets(String baseUrl, String jobId) 
	throws IOException
	{	
		Pagination<Bucket> allBuckets = m_WebServiceClient.getBuckets(baseUrl, 
				jobId, SYS_CHANGE_NORMALIZATION, false, 0l, 1500l);
		test(allBuckets.getDocumentCount() == FAREQUOTE_NUM_BUCKETS);
		test(allBuckets.getHitCount() == FAREQUOTE_NUM_BUCKETS);
		
		
		/*
		 * Test that getting all the results at once is the same as 
		 * paging them.
		 */
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
		 * Test that paging buckets by date is the same
		 */		
		pagedBuckets = new ArrayList<>();
		skip = 0; take = 140;
		while (skip < allBuckets.getHitCount())
		{
			Pagination<Bucket> buckets = m_WebServiceClient.getBuckets(baseUrl, 
					jobId, SYS_CHANGE_NORMALIZATION, false, skip, take,
			allBuckets.getDocuments().get(0).getEpoch(),
			allBuckets.getDocuments().get((int)allBuckets.getHitCount()-1).getEpoch() +1); 
			
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
	public boolean verifyFarequoteUnusualNormalisedBuckets(String baseUrl, String jobId) 
	throws IOException
	{
		/*
		 * Get the unusual behaviour normalised buckets through the 
		 * standard results endpoint
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
		 * Test that paging buckets by date is the same
		 */		
		pagedBuckets = new ArrayList<>();
		skip = 0; take = 140;
		while (skip < allBuckets.getHitCount())
		{
			Pagination<Bucket> buckets = m_WebServiceClient.<Long>getBuckets(baseUrl, 
					jobId, UNUSUAL_BEHAVIOUR_NORMALIZATION, true, skip, take,
					allBuckets.getDocuments().get(0).getEpoch(),
					allBuckets.getDocuments().get((int)allBuckets.getHitCount()-1).getEpoch() +1); 
					
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
		for (Bucket bucket: allBuckets.getDocuments())
		{
			double bucketMax = 0.0;
			for (AnomalyRecord r : bucket.getRecords())
			{
				if (r.isSimpleCount() != null && r.isSimpleCount())
				{
					continue;
				}
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
		 * Check the bucket score is equal to the max anomaly
		 * record score in the bucket
		 */
		// the test takes ages to go through every bucket, don't do all for now
		int count = 30;
		for (Bucket bucket: allBuckets.getDocuments())
		{
			Pagination<AnomalyRecord> records = m_WebServiceClient.getBucketRecords(
					baseUrl, jobId, bucket.getId(), "both");			
			
			double bucketMax = 0.0;
			for (AnomalyRecord r : records.getDocuments())
			{
				if (r.isSimpleCount() != null && r.isSimpleCount())
				{
					continue;
				}
				bucketMax = Math.max(r.getUnusualScore(), bucketMax);
			}
			
			test(bucketMax == bucket.getAnomalyScore());
			
			if (count-- < 0)
			{
				break;
			}
		}
		
		
		/*
		 * Test get buckets by date range with a time string
		 * and check the anomaly score is equal to getting all
		 * buckets at once
		 */	
		String [] startDateFormats = new String[] {"2013-01-30T15:10:00Z", "1359558600"};
		String [] endDateFormats = new String[] {"2013-01-31T22:10:00.000+0000", "1359670200"};
		
		for (int i=0; i<startDateFormats.length; i++)
		{
			Pagination<Bucket> byDate = m_WebServiceClient.getBuckets(
					baseUrl, jobId, UNUSUAL_BEHAVIOUR_NORMALIZATION, true, 0l, 1000l, 
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
	public boolean verifyFarequoteNormalisedRecords(String baseUrl, String jobId) 
	throws IOException
	{		
		// Test for different normalisation arguments
		String [] normTypes = new String[] {BOTH_NORMALIZATIONS, 
				SYS_CHANGE_NORMALIZATION, UNUSUAL_BEHAVIOUR_NORMALIZATION};
		
		for (String normType : normTypes)
		{
			// there are 1332 records in the farequote results
			Pagination<AnomalyRecord> allRecords = m_WebServiceClient.getRecords(
					baseUrl, jobId, normType, 0l, 1400l);

			/*
			 * Test that getting all the results at once is the same as 
			 * paging them.
			 */
			List<AnomalyRecord> pagedRecords = new ArrayList<>();
			long skip = 0, take = 1000;

			Pagination<AnomalyRecord> page = m_WebServiceClient.getRecords(
					baseUrl, jobId, normType, skip, take);
			skip += take;
			pagedRecords.addAll(page.getDocuments());

			while (skip < page.getHitCount())
			{
				page = m_WebServiceClient.getRecords(baseUrl, jobId, normType, skip, take);
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
			 * Test paging by date is the same as getting them all 
			 * at once
			 */
			
			// need start and end dates first
			Pagination<Bucket> allBuckets = m_WebServiceClient.getBuckets(baseUrl, 
					jobId, UNUSUAL_BEHAVIOUR_NORMALIZATION, true, 0l, 1500l);
			long startDate = allBuckets.getDocuments().get(0).getEpoch();
			long endDate = allBuckets.getDocuments().get(allBuckets.getDocumentCount()-1).getEpoch() + 1;
			
			pagedRecords = new ArrayList<>();
			skip = 0; take = 200;

			page = m_WebServiceClient.getRecords(
					baseUrl, jobId, normType, skip, take,
					startDate, endDate);
			skip += take;
			pagedRecords.addAll(page.getDocuments());

			while (skip < page.getHitCount())
			{
				page = m_WebServiceClient.getRecords(
						baseUrl, jobId, normType, skip, take,
						startDate, endDate);
				
				skip += take;
				pagedRecords.addAll(page.getDocuments());
			}

			recordIndex = 0;
			test(allRecords.getHitCount() == pagedRecords.size());	
			
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
				switch (normType)
				{
				case SYS_CHANGE_NORMALIZATION:
					if (record.getAnomalyScore() >= 100.0)
					{
						maxAnomalyScoreCount++;
					}
					test(record.getUnusualScore() == null);
					break;
								
				case UNUSUAL_BEHAVIOUR_NORMALIZATION:
					if (record.getUnusualScore() >= 100.0)
					{
						maxUnusualScoreCount++;
					}
					test(record.getAnomalyScore() == null);
					break;
					
				default:
					if (record.getUnusualScore() >= 100.0)
					{
						maxUnusualScoreCount++;
					}
					if (record.getAnomalyScore() >= 100.0)
					{
						maxAnomalyScoreCount++;
					}
				}
			}
			
			if (SYS_CHANGE_NORMALIZATION.equals(normType))
			{
				test(maxAnomalyScoreCount >= 1);
			}
			else if (UNUSUAL_BEHAVIOUR_NORMALIZATION.equals(normType))
			{
				test(maxUnusualScoreCount >= 1);
			}
			else
			{
				test(maxAnomalyScoreCount >= 1);
				test(maxUnusualScoreCount >= 1);
			}

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
				
				Collections.sort(byDate.getDocuments(), new Comparator<AnomalyRecord>() {

					@Override
					public int compare(AnomalyRecord o1, AnomalyRecord o2) 
					{
						return o1.getTimestamp().compareTo(o2.getTimestamp());
					}
				});

				// must be equal or after start date and before the end date
				test(byDate.getDocuments().get(0).getTimestamp().compareTo(new Date(1359558600000l)) >= 0);
				test(byDate.getDocuments().get(byDate.getDocumentCount() -1)
						.getTimestamp().compareTo(new Date(1359669900000l)) < 0);
			}

		}
		return true;
	}

	
	/**
	 * Checks the error response is correct when using an 
	 * unknown normalisation type or the wrong type for buckets.
	 *  
	 * @param baseUrl
	 * @param jobId
	 * @throws IOException
	 */
	public void testInvalidNormalisationArgument(String baseUrl, String jobId) 
	throws IOException
	{
		m_WebServiceClient.getRecords(baseUrl, jobId, "made_up_norm_type", 0l, 1400l);
		ApiError error =  m_WebServiceClient.getLastError();
		test(error != null);
		test(error.getErrorCode() == ErrorCode.INVALID_NORMALIZATION_ARG);
		
		// cannot have 'both' normalization on buckets
		m_WebServiceClient.getBuckets(baseUrl, jobId, BOTH_NORMALIZATIONS, false);
		error =  m_WebServiceClient.getLastError();
		test(error != null);
		test(error.getErrorCode() == ErrorCode.INVALID_NORMALIZATION_ARG);
		
		m_WebServiceClient.getBucket(baseUrl, jobId, "bucket_id", true, BOTH_NORMALIZATIONS);
		error =  m_WebServiceClient.getLastError();
		test(error != null);
		test(error.getErrorCode() == ErrorCode.INVALID_NORMALIZATION_ARG);
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
		
		// Always delete the test job first in case it is hanging around
		// from a previous run
		test.m_WebServiceClient.deleteJob(baseUrl, TEST_JOB_ID);

		String farequoteJob = TEST_JOB_ID;
		jobUrls.add(farequoteJob);
		
		File fareQuoteData = new File(prelertTestDataHome + 
				"/engine_api_integration_test/farequote.csv");		
		// Farequote test
		test.createFarequoteJob(baseUrl);
		test.m_WebServiceClient.fileUpload(baseUrl, farequoteJob, 
				fareQuoteData, false);
		test.m_WebServiceClient.closeJob(baseUrl, farequoteJob);
		
		
		test.verifyFarequoteSysChangeNormalisedBuckets(baseUrl, farequoteJob);
		test.verifyFarequoteUnusualNormalisedBuckets(baseUrl, farequoteJob);
		test.verifyFarequoteNormalisedRecords(baseUrl, farequoteJob);
		test.testInvalidNormalisationArgument(baseUrl, farequoteJob);
		
		
		//==========================
		// Clean up test jobs
		test.deleteJobs(baseUrl, jobUrls);
	
		test.close();
		
		s_Logger.info("All tests passed Ok");
	}
}
