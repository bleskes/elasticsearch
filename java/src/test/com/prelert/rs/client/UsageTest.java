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

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

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
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.JobDetails;
import com.prelert.job.JobDetails.Counts;
import com.prelert.job.usage.Usage;
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
	
	static final public String ES_CLUSTER_NAME = "davekprelert";  // TODO
	
	
	static final long FLIGHTCENTRE_NUM_BUCKETS = 296;
	static final long FLIGHTCENTRE_NUM_RECORDS = 175910;
	static final long FLIGHTCENTRE_NUM_FIELDS = FLIGHTCENTRE_NUM_RECORDS * 3;
	static final long FLIGHTCENTRE_NUM_PROCESSED_FIELDS = FLIGHTCENTRE_NUM_RECORDS * 2;
	static final long FLIGHTCENTRE_INPUT_BYTES_CSV = 5724086;
			
	static final long FLIGHTCENTRE_INPUT_BYTES_JSON = 17510020;

	
	static public final Settings LOCAL_SETTINGS;
	static
	{
		LOCAL_SETTINGS = ImmutableSettings.settingsBuilder()
				.put("http.enabled", "false")
				.put("discovery.zen.ping.multicast.enabled", "false")
				.put("discovery.zen.ping.unicast.hosts", "localhost")
				.build();
	}
	
	
	private EngineApiClient m_WebServiceClient;
	private Client m_EsClient;
		
	public UsageTest(String elasticSearchClusterName)
	{
		m_WebServiceClient = new EngineApiClient();
		
		Node node = nodeBuilder().settings(LOCAL_SETTINGS).client(true)
				.clusterName(elasticSearchClusterName).node();
		
		m_EsClient = node.client();
		
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
	
	
	public void validateFlightCentreUsage(String jobId, boolean isJson)
	{
		String [] indexes = {"prelert-usage", jobId};
		for (String index : indexes)
		{
			FilterBuilder fb = FilterBuilders.matchAllFilter();

			SearchResponse response = m_EsClient.prepareSearch(index) 
					.setTypes(Usage.TYPE)
					.setPostFilter(fb)
					.get();
			
			
			long bytes = 0;
			long fields = 0; 
			long records = 0;
			for (SearchHit hit : response.getHits().getHits())
			{
				bytes += ((Number)hit.getSource().get(Usage.INPUT_BYTES)).longValue();
				fields += ((Number)hit.getSource().get(Usage.INPUT_FIELD_COUNT)).longValue();
				records += ((Number)hit.getSource().get(Usage.INPUT_RECORD_COUNT)).longValue();
			}
			
			
			if (index.equals(jobId))
			{
				test(records == FLIGHTCENTRE_NUM_RECORDS);
				test(fields == FLIGHTCENTRE_NUM_FIELDS);

				if (isJson)
				{
					test(bytes == FLIGHTCENTRE_INPUT_BYTES_JSON);
				}
				else
				{
					// the gzipped data is 1 byte smaller (assuming this is a new line)
					test(bytes == FLIGHTCENTRE_INPUT_BYTES_CSV || 
							bytes == FLIGHTCENTRE_INPUT_BYTES_CSV -1);
				}
			}
			else
			{
				// prelert usage index is the total so should have a
				// a minimum of these values
				test(records >= FLIGHTCENTRE_NUM_RECORDS);
				test(fields >= FLIGHTCENTRE_NUM_FIELDS);

				if (isJson)
				{
					test(bytes >= FLIGHTCENTRE_INPUT_BYTES_JSON);
				}
				else
				{
					// the gzipped data is 1 byte smaller (assuming this is a new line)
					test(bytes >= FLIGHTCENTRE_INPUT_BYTES_CSV || 
							bytes >= FLIGHTCENTRE_INPUT_BYTES_CSV -1);
				}
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
			test.validateFlightCentreUsage(jobId, isJson);
			
			isCompressed = true;
			jobId = test.runFarequoteJob(baseUrl, flightCenterDataCsvGzip, isJson, isCompressed);
			test.validateFlightCentreCounts(baseUrl, jobId, isJson);
			test.validateFlightCentreUsage(jobId, isJson);
						
			isJson = true;
			isCompressed = false;
			jobId = test.runFarequoteJob(baseUrl, flightCentreDataJson, isJson, isCompressed);
			test.validateFlightCentreCounts(baseUrl, jobId, isJson);
			test.validateFlightCentreUsage(jobId, isJson);
			
			jobs.add(jobId);
			
			
			
			for (String id : jobs)
			{
				test.m_WebServiceClient.deleteJob(baseUrl, id);
			}
		}
		
		s_Logger.info("All tests passed Ok");
		
	}
}


