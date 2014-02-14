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

package com.prelert.rs.client;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.JobDetails;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

/**
 * Autodetect REST API integration test.
 * <br/>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing these 3 files:
 * <ol>
 * <li>/engine_api_integration_test/flightcentre.csv.gz</li>
 * <li>/engine_api_integration_test/flightcentre.json</li>
 * <li>/engine_api_integration_test/farequote_ISO_8601.csv</li>
 * </ol>
 * 
 */
public class JobsTest implements Closeable
{
	final String WIKI_TRAFFIC_JOB_CONFIG = "{\"analysisConfig\" : {"
			+ "\"bucketSpan\":86400,"  
			+ "\"detectors\" :" 
			+ "[{\"fieldName\":\"hitcount\",\"byFieldName\":\"url\"}] },"
			+ "\"dataDescription\":{\"fieldDelimiter\":\"\\t\"} }}";

	final String FLIGHT_CENTRE_JOB_CONFIG = "{\"analysisConfig\" : {"
			+ "\"bucketSpan\":3600,"  
			+ "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] "
			+ "},"
			+ "\"dataDescription\":{\"fieldDelimiter\":\",\"} }}";		
	
	final String FLIGHT_CENTRE_JSON_JOB_CONFIG = "{\"analysisConfig\" : {"
			+ "\"bucketSpan\":3600,"  
			+ "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] "
			+ "},"
			+ "\"dataDescription\":{\"format\":\"json\",\"timeField\":\"timestamp\"} }}";
	
	final String FARE_QUOTE_TIME_FORMAT_CONFIG = "{\"analysisConfig\" : {"
			+ "\"bucketSpan\":3600,"  
			+ "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] "
			+ "},"
			+ "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"time\", " 
			+ "\"timeFormat\":\"yyyy-MM-dd HH:mm:ss\"} }}";	
	
	/**
	 * This is a format string insert a reference job id.
	 */
	final String JOB_REFERENCE_CONFIG = "{\"referenceJobId\" : \"%s\"}";
	
	
	static final private Logger s_Logger = Logger.getLogger(JobsTest.class);
	
	/**
	 * The default base Url used in the test
	 */
	static final public String JOBS_URL = "http://localhost:8080/engine/beta/jobs";
	
	private AutodetectRsClient m_WebServiceClient;
	
	/**
	 * Creates a new http client call {@linkplain #close()} once finished
	 */
	public JobsTest()
	{
		m_WebServiceClient = new AutodetectRsClient();
	}
	
	@Override
	public void close() throws IOException 
	{
		m_WebServiceClient.close();
	}	

	
	private JobDetails getJob(String jobUrl) 
	throws IOException
	{
		return m_WebServiceClient.getJob(jobUrl).getDocument();
	}
	
	/**
	 * Get all the jobs and test the pagination objects values are
	 * set correctly
	 * 
	 * @param baseUrl The URL of the REST API i.e. an URL ending with 
	 * 	<code>api/jobs</code>
	 * @return
	 * @throws IOException
	 */
	public boolean getJobsTest(String baseUrl) 
	throws IOException
	{
		Pagination<JobDetails> jobs = m_WebServiceClient.getJobs(baseUrl); 
		
		test(jobs.getHitCount() == jobs.getDocuments().size());
		test(jobs.getTake() > 0);
		if (jobs.getHitCount() < jobs.getTake())
		{
			test(jobs.getNextPage() == null);
			test(jobs.getPreviousPage() == null);
		}
		else
		{
			String prevPageUrl = null;
			if (jobs.getSkip() > 0)
			{
				int start = Math.max(0,  jobs.getSkip() -jobs.getTake());
				prevPageUrl = String.format("%s?skip=%d&take=%D", baseUrl,
						start, jobs.getTake());							
			}
			test((prevPageUrl == null && jobs.getPreviousPage() == null) ||
					prevPageUrl.equals(jobs.getPreviousPage().toString()));
			
			String nextPageUrl = null;
			if (jobs.getHitCount() > jobs.getSkip() + jobs.getTake())
			{
				int start = jobs.getSkip() + jobs.getTake();
				nextPageUrl = String.format("%s?skip=%d&take=%D", baseUrl,
						start, jobs.getTake());							
			}
			test((nextPageUrl == null && jobs.getNextPage() == null) ||
					nextPageUrl.equals(jobs.getNextPage().toString()));
		}
		
			
		return true;
	}
	
	/**
	 * Creates a job using the Wiki Traffic stats configuration then 
	 * reads it back verifying all the correct properties are set. 
	 * 
	 * @param baseUrl The URL of the REST API i.e. an URL ending with 
	 * 	<code>api/jobs</code>
	 * 
	 * @return The url to the created job
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String createWikiTrafficJobTest(String baseUrl) 
	throws ClientProtocolException, IOException
	{	
		String location = m_WebServiceClient.createJob(baseUrl, WIKI_TRAFFIC_JOB_CONFIG);
		if (location == null)
		{
			s_Logger.error("No Location returned in by create job");
			test(location != null);
		}
		
		// get job by location, verify
		SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(location);
		if (doc.isExists() == false)
		{
			s_Logger.error("No Job at URL " + location);
		}
		JobDetails job = doc.getDocument();
		
		AnalysisConfig.Detector d = new AnalysisConfig.Detector();
		d.setFieldName("hitcount");
		d.setByFieldName("url");
		AnalysisConfig ac = new AnalysisConfig();
		ac.setBucketSpan(86400L);
		ac.setDetectors(Arrays.asList(d));
		
		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter("\t");
		

		test(ac.equals(job.getAnalysisConfig()));
		test(dd.equals(job.getDataDescription()));
		test(job.getAnalysisOptions() == null);
				
		test(job.getLocation().toString().equals(location));
		test(job.getResultsEndpoint().toString().equals(location + "/results"));
		test(job.getStreamingEndpoint().toString().equals(location + "/streaming"));
		
		test(job.getLastDataTime() == null);
		test(job.getFinishedTime() == null);
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, -2);
		Date twoMinsAgo = cal.getTime();
		cal.add(Calendar.MINUTE, 4);
		Date twoMinsInFuture = cal.getTime();
		
		test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));
		
		return location;
	}	
	
	/**
	 * Creates a job using the FlightCentre configuration then 
	 * reads it back verifying all the correct properties are set. 
	 * 
	 * @param baseUrl The URL of the REST API i.e. an URL ending with 
	 * 	<code>api/jobs</code>
	 * 
	 * @return The url to the created job
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String createFlightCentreJobTest(String baseUrl) 
	throws ClientProtocolException, IOException
	{	
		String location = m_WebServiceClient.createJob(baseUrl, FLIGHT_CENTRE_JOB_CONFIG);
		if (location == null)
		{
			s_Logger.error("No Location returned in by create job");
			test(location != null);
		}
		
		// get job by location, verify
		SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(location);
		if (doc.isExists() == false)
		{
			s_Logger.error("No Job at URL " + location);
		}
		JobDetails job = doc.getDocument();
		
		AnalysisConfig.Detector d = new AnalysisConfig.Detector();
		d.setFieldName("responsetime");
		d.setByFieldName("airline");
		AnalysisConfig ac = new AnalysisConfig();
		ac.setBucketSpan(3600L);
		ac.setDetectors(Arrays.asList(d));
		
		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter(",");
		

		test(ac.equals(job.getAnalysisConfig()));
		test(dd.equals(job.getDataDescription()));
		test(job.getAnalysisOptions() == null);
				
		test(job.getLocation().toString().equals(location));
		test(job.getResultsEndpoint().toString().equals(location + "/results"));
		test(job.getStreamingEndpoint().toString().equals(location + "/streaming"));
		
		test(job.getLastDataTime() == null);
		test(job.getFinishedTime() == null);
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, -2);
		Date twoMinsAgo = cal.getTime();
		cal.add(Calendar.MINUTE, 4);
		Date twoMinsInFuture = cal.getTime();
		
		test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));
		
		return location;
	}
	
	/**
	 * Creates a job using the Farequote ISO 8601 time format configuration 
	 * then reads it back verifying all the correct properties are set. 
	 * 
	 * @param baseUrl The URL of the REST API i.e. an URL ending with 
	 * 	<code>api/jobs</code>
	 * 
	 * @return The url to the created job
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String createFareQuoteTimeFormatJobTest(String baseUrl) 
	throws ClientProtocolException, IOException
	{	
		String location = m_WebServiceClient.createJob(baseUrl, FARE_QUOTE_TIME_FORMAT_CONFIG);
		if (location == null)
		{
			s_Logger.error("No Location returned in by create job");
			test(location != null);
		}
		
		// get job by location, verify
		SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(location);
		if (doc.isExists() == false)
		{
			s_Logger.error("No Job at URL " + location);
		}
		JobDetails job = doc.getDocument();
		
		verifyFareQuoteTimeFormatJobTest(job, location);
		
		return location;
	}
	
	
	/**
	 * Create a new job base on configuration for job <code>refJobId</code>.
	 * 
	 * @param baseUrl The URL of the REST API i.e. an URL ending with 
	 * 	<code>api/jobs</code>
	 * @param refJobId The Job Id to use as the configuration for this
	 * new job
	 * 
	 * @return The url to the created job
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String createJobFromFareQuoteTimeFormatRefId(String baseUrl, String refJobId) 
	throws ClientProtocolException, IOException
	{
		String config = String.format(JOB_REFERENCE_CONFIG, refJobId);
		String location = m_WebServiceClient.createJob(baseUrl, config);
		if (location == null)
		{
			s_Logger.error("No Location returned in by create job");
			test(location != null);
		}
		
		// get job by location, verify
		SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(location);
		if (doc.isExists() == false)
		{
			s_Logger.error("No Job at URL " + location);
		}
		JobDetails job = doc.getDocument();
		
		verifyFareQuoteTimeFormatJobTest(job, location);
		
		test(location.equals(job.getLocation().toString()));
		
		return location;		
	}
	
	
	private void verifyFareQuoteTimeFormatJobTest(JobDetails job, String location) 
	{
		AnalysisConfig.Detector d = new AnalysisConfig.Detector();
		d.setFieldName("responsetime");
		d.setByFieldName("airline");
		AnalysisConfig ac = new AnalysisConfig();
		ac.setBucketSpan(3600L);
		ac.setDetectors(Arrays.asList(d));
		
		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter(",");
		dd.setTimeField("time");
		dd.setTimeFormat("yyyy-MM-dd HH:mm:ss");

		test(ac.equals(job.getAnalysisConfig()));
		test(dd.equals(job.getDataDescription()));
		test(job.getAnalysisOptions() == null);
				
		test(job.getLocation().toString().equals(location));
		test(job.getResultsEndpoint().toString().equals(location + "/results"));
		test(job.getStreamingEndpoint().toString().equals(location + "/streaming"));
		
		test(job.getLastDataTime() == null);
		test(job.getFinishedTime() == null);
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, -2);
		Date twoMinsAgo = cal.getTime();
		cal.add(Calendar.MINUTE, 4);
		Date twoMinsInFuture = cal.getTime();
		
		test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));
	}
	
	/**
	 * Creates a job using the FlightCentre Json configuration then 
	 * reads it back verifying all the correct properties are set. 
	 * 
	 * @param baseUrl The URL of the REST API i.e. an URL ending with 
	 * 	<code>api/jobs</code>
	 * 
	 * @return The url to the created job
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String createFlightCentreJsonJobTest(String baseUrl) 
	throws ClientProtocolException, IOException
	{	
		String location = m_WebServiceClient.createJob(baseUrl, 
				FLIGHT_CENTRE_JSON_JOB_CONFIG);
		if (location == null)
		{
			s_Logger.error("No Location returned in by create job");
			test(location != null);
		}
		
		// get job by location, verify
		SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(location);
		if (doc.isExists() == false)
		{
			s_Logger.error("No Job at URL " + location);
		}
		JobDetails job = doc.getDocument();
		
		AnalysisConfig.Detector d = new AnalysisConfig.Detector();
		d.setFieldName("responsetime");
		d.setByFieldName("airline");
		AnalysisConfig ac = new AnalysisConfig();
		ac.setBucketSpan(3600L);
		ac.setDetectors(Arrays.asList(d));
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("timestamp");
		
		test(ac.equals(job.getAnalysisConfig()));
		test(dd.equals(job.getDataDescription()));
		test(job.getAnalysisOptions() == null);
				
		test(job.getLocation().toString().equals(location));
		test(job.getResultsEndpoint().toString().equals(location + "/results"));
		test(job.getStreamingEndpoint().toString().equals(location + "/streaming"));
		
		test(job.getLastDataTime() == null);
		test(job.getFinishedTime() == null);
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, -2);
		Date twoMinsAgo = cal.getTime();
		cal.add(Calendar.MINUTE, 4);
		Date twoMinsInFuture = cal.getTime();
		
		test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));
		
		return location;
	}

	/**
	 * Slowly upload the contents of <code>dataFile</code> to the server.
	 * Starts a background thread to write the data and sleep between uploads. 
	 * </br>
	 * This is to show that a slow streaming server works the 
	 * same as a faster local server. 
	 * 
	 * @param jobUrl The full url to the job
	 * @param dataFile Should match the data configuration format of the job
	 * @param compressed Is the data gzipped compressed?
	 * @throws IOException
	 */
	public void slowUpload(String jobUrl, final File dataFile) 
	throws IOException
	{
		final PipedInputStream pipedIn = new PipedInputStream();
		final PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				int n;
				byte [] buf = new byte[131072];
				try
				{
					FileInputStream fs;
					try 
					{
						fs = new FileInputStream(dataFile);
					} 
					catch (FileNotFoundException e) 
					{
						e.printStackTrace();
						return;
					}

					while((n = fs.read(buf)) > -1) 
					{
						pipedOut.write(buf, 0, n);
						Thread.sleep(100);
					}	
					fs.close();

					pipedOut.close();
				}
				catch (IOException e)
				{
					s_Logger.info(e);
				}
				catch (InterruptedException e) 
				{
					s_Logger.info(e);
				}				
			}
		}).start();
		
		m_WebServiceClient.streamingUpload(jobUrl, pipedIn, false);
	}

	
	/**
	 * Upload the contents of <code>dataFile</code> to the server.
	 * 
	 * @param jobUrl The full url to the job
	 * @param dataFile Should match the data configuration format of the job
	 * @param compressed Is the data gzipped compressed?
	 * @throws IOException
	 */
	public void uploadData(String jobUrl, File dataFile, boolean compressed) 
	throws IOException
	{
		FileInputStream stream = new FileInputStream(dataFile);
		boolean success = m_WebServiceClient.streamingUpload(jobUrl, stream, compressed);		
		test(success);
	}
	
	
	/**
	 * Finish the job (as all data has been uploaded).
	 * 
	 * @param jobUrl The full url to the job
	 * @return
	 * @throws IOException 
	 */
	public boolean finishJob(String jobUrl) 
	throws IOException
	{
		boolean closed = m_WebServiceClient.closeStreamingJob(jobUrl);
		test(closed);
		return closed;
	}
	
	/**
	 * Read the job results from the Url and compare bucket anomaly scores 
	 * with the known set of results.
	 * 
	 * @param jobUrl The url of a job created with the Flight Centre  
	 * configuration ({@linkplain JobsTest#FLIGHT_CENTRE_JOB_CONFIG}
	 * @param knownResults List of buckets as an uptyped map
	 * @param take The max number of buckets to return
	 * 
	 * @throws IOException 
	 */
	public void verifyJobResults(String jobUrl, long take) 
	throws IOException
	{
		s_Logger.debug("Verifying results for job " + jobUrl);
		Pagination<Bucket> buckets = m_WebServiceClient.getBuckets(jobUrl, false, 0L, take);
		
		test(buckets.getDocumentCount() <= take);
		for (Bucket b : buckets.getDocuments())
		{			
			test(b.getAnomalyScore() >= 0.0);
			test(b.getRecordCount() > 0);
			test(b.getRecords().size() == 0);
			test(b.getDetectors().size() == 0);
			test(b.getId() != null && b.getId().isEmpty() == false);			
			long epoch = Long.parseLong(b.getId()); // will throw if not a number
			Date date = new Date(epoch * 1000);
			
			// sanity check, the data may be old but it should be newer than 2010
			final long firstJan2010 = 1262304000000L;
			test(date.after(new Date(firstJan2010)));
			test(b.getTimestamp().after(new Date(firstJan2010)));
			// epoch and timestamp should be the same
			test(date.equals(b.getTimestamp()));			
		}
		
		// this time get the anomaly records at the same time
		buckets = m_WebServiceClient.getBuckets(jobUrl, true, 0L, take);				
		test(buckets.getDocumentCount() <= take);		
		for (Bucket b : buckets.getDocuments())
		{
			test(b.getRecordCount() > 0);
			test(b.getDetectors().size() == 0);
			test(b.getId() != null && b.getId().isEmpty() == false);			
			long epoch = Long.parseLong(b.getId()); // will throw if not a number
			Date date = new Date(epoch * 1000);
			
			// sanity check, the data may be old but it should be newer than 2010
			final long firstJan2010 = 1262304000000L;
			test(date.after(new Date(firstJan2010)));
			test(b.getTimestamp().after(new Date(firstJan2010)));
			// epoch and timestamp should be the same
			test(date.equals(b.getTimestamp()));			

			for (AnomalyRecord r: b.getRecords())
			{
				// at a minimum all records should have these fields
				test(r.getActual() != null);
				test(r.getProbability() != null);
				test(r.getAnomalyScore() != null);
				test(r.getFunction() != null);
			}
		}
	}
	
	
	public void testDateFilters(String jobUrl, Date start, Date end) 
	throws IOException
	{
		final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
		final String ISO_8601_DATE_FORMAT_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
		
		// test 3 date formats
		Long epochStart = start.getTime() / 1000;
		Long epochEnd = end.getTime() / 1000;
		String dateStart = new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(start);
		String dateEnd = new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(end);
		String dateStartMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS).format(start);
		String dateEndMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS).format(end);
		
		// query with the 3 date formats
		Pagination<Bucket> buckets = m_WebServiceClient.getBuckets(jobUrl, false, 
				null, null, epochStart, epochEnd);		
		test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);
		test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
				.getTimestamp().compareTo(end) <= 0);
		
		buckets = m_WebServiceClient.getBuckets(jobUrl, false, 
				null, null, dateStart, dateEnd);		
		test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);
		test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
				.getTimestamp().compareTo(end) <= 0);
		
		buckets = m_WebServiceClient.getBuckets(jobUrl, false, 
				null, null, dateStartMs, dateEndMs);		
		test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);
		test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
				.getTimestamp().compareTo(end) <= 0);		
		
		
		// just a start date
		buckets = m_WebServiceClient.getBuckets(jobUrl, false, 
				0L, 100L, dateStart, null);		
		test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);

		buckets = m_WebServiceClient.getBuckets(jobUrl, false, 
				0L, 100L, epochStart, null);		
		test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);
		
		
		// just an end date
		buckets = m_WebServiceClient.getBuckets(jobUrl, false, 
				null, null, null, dateEndMs);		
		test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
				.getTimestamp().compareTo(end) <= 0);
		
		buckets = m_WebServiceClient.getBuckets(jobUrl, false, 
				null, null, null, dateEnd);		
		test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
				.getTimestamp().compareTo(end) <= 0);
		
		
		// Test paging from the start date
		buckets = m_WebServiceClient.getBuckets(jobUrl, false,  0L, 5L, dateStart, null);
		test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);	
		
		Date lastDate = buckets.getDocuments().get(buckets.getDocumentCount() -1)
				.getTimestamp();

		int bucketCount = 0;
		while (buckets.getNextPage() != null)
		{
			String url = buckets.getNextPage().toString();
			buckets = m_WebServiceClient.<Pagination<Bucket>>get(url,
						new TypeReference<Pagination<Bucket>>() {});
			
			Date firstDate = buckets.getDocuments().get(0).getTimestamp();
			
			test(firstDate.compareTo(lastDate) >= 0);
			
			lastDate = buckets.getDocuments().get(buckets.getDocumentCount() -1)
					.getTimestamp();
			bucketCount++;
		}
		
		// and page backwards
		while (buckets.getPreviousPage() != null)
		{
			String url = buckets.getPreviousPage().toString();
			buckets = m_WebServiceClient.<Pagination<Bucket>>get(url,
						new TypeReference<Pagination<Bucket>>() {});
			
			Date date = buckets.getDocuments().get(buckets.getDocumentCount() -1)
					.getTimestamp();
			
			test(date.compareTo(lastDate) <= 0);
			
			lastDate = buckets.getDocuments().get(0).getTimestamp();
			bucketCount--;
		}
		
		test(bucketCount == 0);
	}
	

	/**
	 * Delete all the jobs in the list
	 * 
	 * @param jobUrls The list of full URLs to the jobs to delete
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void deleteJobsTest(List<String> jobUrls) 
	throws IOException, InterruptedException
	{
		for (String jobUrl : jobUrls)
		{
			s_Logger.debug("Deleting job " + jobUrl);
			
			boolean success = m_WebServiceClient.deleteJob(jobUrl); 
			if (success == false)
			{
				s_Logger.error("Error deleting job " + jobUrl);
			}
		}
		
		// Sleep for a second to give ElasticSearch a chance to catch up.
		Thread.sleep(1200);
		
		for (String jobUrl : jobUrls)
		{
			SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobUrl);
			
			test(doc.isExists() == false);
		}
	}
	
	/**
	 * Read in the Json results file and return as a list of map objects.
	 * 
	 * @param resultsFile
	 * @return
	 * @throws IOException 
	 * @throws JsonParseException 
	 */
	public List<Map<String,Object>> readResultsFromFile(File resultsFile) 	
	throws JsonParseException, IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String,Object>> standardBuckets = mapper.readValue(resultsFile, 
				new TypeReference<List<Map<String,Object>>>() {});

		return standardBuckets;
	}
	
	
	/**
	 * Throws an exception if <code>condition</code> is false.
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
	 * The program takes one argument which is the 
	 * If no arguments are given then {@value #JOBS_URL} is used. 
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
				
		String baseUrl = JOBS_URL;
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
		
				
		JobsTest test = new JobsTest();	
		List<String> jobUrls = new ArrayList<>();
		
		//=================
		// CSV & Gzip test 
		//
		String flightCentreJobUrl = test.createFlightCentreJobTest(baseUrl);
		test.getJobsTest(baseUrl);

		File flightCentreData = new File(prelertTestDataHome + 
				"/engine_api_integration_test/flightcentre.csv.gz");
		test.uploadData(flightCentreJobUrl, flightCentreData, true);
		test.finishJob(flightCentreJobUrl);

		// Give ElasticSearch a chance to index
		Thread.sleep(1500);

		test.verifyJobResults(flightCentreJobUrl, 100);
		jobUrls.add(flightCentreJobUrl);		

		//=================
		// JSON test
		//
		String flightCentreJsonJobUrl = test.createFlightCentreJsonJobTest(baseUrl);
		test.getJobsTest(baseUrl);

		flightCentreData = new File(prelertTestDataHome + 
				"/engine_api_integration_test/flightcentre.json");
		test.uploadData(flightCentreJsonJobUrl, flightCentreData, false);
		test.finishJob(flightCentreJsonJobUrl);

		// Give ElasticSearch a chance to index
		Thread.sleep(1500);

		test.verifyJobResults(flightCentreJsonJobUrl, 100);
		jobUrls.add(flightCentreJsonJobUrl);	
			
		//=================
		// Time format test
		//
		String farequoteTimeFormatJobUrl = test.createFareQuoteTimeFormatJobTest(baseUrl);
		test.getJobsTest(baseUrl);

		File fareQuoteData = new File(prelertTestDataHome + 
				"/engine_api_integration_test/farequote_ISO_8601.csv");
		test.slowUpload(farequoteTimeFormatJobUrl, fareQuoteData);
		test.finishJob(farequoteTimeFormatJobUrl);

		// Give ElasticSearch a chance to index
		Thread.sleep(1500);

		test.verifyJobResults(farequoteTimeFormatJobUrl, 150);
		jobUrls.add(farequoteTimeFormatJobUrl);		
					
		// known dates for the farequote data
		Date start = new Date(1359406800000L);
		Date end = new Date(1359662400000L);
		test.testDateFilters(farequoteTimeFormatJobUrl, start, end);
		
		
		//==========================
		// Create another test based on
		// the job config used above
		//
		JobDetails job = test.getJob(farequoteTimeFormatJobUrl);
		String refJobUrl = test.createJobFromFareQuoteTimeFormatRefId(baseUrl, job.getId());
		
		test.uploadData(refJobUrl, fareQuoteData, false);
		test.finishJob(refJobUrl);

		// Give ElasticSearch a chance to index
		Thread.sleep(1500);

		test.verifyJobResults(refJobUrl, 150);
		jobUrls.add(refJobUrl);		
		
		// Clean up test jobs
		test.deleteJobsTest(jobUrls);		
		test.close();
	}

}
