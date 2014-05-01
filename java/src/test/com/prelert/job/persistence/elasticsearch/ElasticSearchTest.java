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

package com.prelert.job.persistence.elasticsearch;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;

import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.elasticsearch.ElasticSearchPersister;
import com.prelert.job.persistence.elasticsearch.ElasticSearchMappings;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.data.parsing.AutoDetectParseException;
import com.prelert.rs.data.parsing.AutoDetectResultsParser;

/**
 * Integration test for writing the autodetect output to ElasticSearch.
 * Reads in a JSON file of autodetect results (pass the filename as the program
 * argument) and writes to the Buckets/Detectors/Records to ElasticSearch. 
 * database. The test creates a new index 'testjob', runs some queries to check 
 * the data was written then deletes the index. 
 * <br/>
 * If no filename is provided as an argument it looks for the file 
 * {@value #DEFAULT_FILE} in the directory set in the property 
 * The -Dprelert.test.data.home  
 */
public class ElasticSearchTest 
{
	public static final String DEFAULT_FILE = "/engine_api_integration_test/wiki_traffic_results.json";
	private static final Logger s_Logger = Logger.getLogger(ElasticSearchTest.class);

	
	/**
	 * Pass the name of a json results file on the command line
	 * @param args
	 * @throws IOException
	 * @throws AutoDetectParseException
	 */
	public static void main(String[] args) throws IOException, AutoDetectParseException 
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender(); 		
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n")); 
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
		  		
		String filePath;
		if (args.length < 1)
		{
			// Build the file path
			String prelertTestDataHome = System.getProperty("prelert.test.data.home");
			filePath = prelertTestDataHome + DEFAULT_FILE;
			File testFile = new File(filePath);
			
			if (testFile.exists() == false)
			{
				s_Logger.error("No file passed as an argument and the default file '" +
							testFile.getPath() + "' cannot be read.");
				s_Logger.info("prelert.test.data.home = " + prelertTestDataHome);
				
				return;
			}	

			return;
		}
		else
		{
			filePath = args[0];
		}
		
		s_Logger.info("Using file " + filePath);
		
		
		final String jobId = "testjob";
		
		// setup elastic search
		Node node = nodeBuilder().client(true).clusterName("prelert").node();
		Client client = node.client();
		ElasticSearchPersister es = new ElasticSearchPersister(jobId, client);
				
		// Create a ElasticSearch index and write results to it		
		createIndex(client, jobId);		
		try
		{
			InputStream fs = new FileInputStream(new File(args[0]));
			AutoDetectResultsParser.parseResults(fs, es, s_Logger);

			// now read back results
			JobManager jobManager = new JobManager(node);
				
			Pagination<Map<String, Object>> page = jobManager.buckets(jobId, 
					false, 0, JobManager.DEFAULT_PAGE_SIZE);
			List<Map<String, Object>> buckets = page.getDocuments();


			long bucketEpoch = 0;
			String interestingBucket = null;
			for (Map<String, Object> bucket : buckets)			
			{
				long epoch = Long.parseLong(bucket.get(Bucket.EPOCH).toString());
				if (epoch <= bucketEpoch)
				{
					s_Logger.error("Out of order");
				}			
				bucketEpoch = epoch;

				@SuppressWarnings("unused")
				int recordCount = Integer.parseInt(bucket.get(Bucket.RECORD_COUNT).toString());

				double anomalyScore = Double.parseDouble(bucket.get(Bucket.ANOMALY_SCORE).toString());
				if (anomalyScore > 0.0)
				{
					interestingBucket = bucket.get(Bucket.EPOCH).toString();
				}
			}

			if (interestingBucket == null || interestingBucket.isEmpty())
			{
				s_Logger.error("No bucket with anomaly score > 0");
				return;
			}
			
			
			Pagination<Detector> detectorPage = jobManager.detectors(
					jobId, 0, JobManager.DEFAULT_PAGE_SIZE);	 
			List<Detector> detectors = detectorPage.getDocuments();
			List<String> detectorNames = new ArrayList<>();

			String interestingDetector = null;
			for (Detector detector : detectors)
			{
				String detectorName = detector.getName();
				detectorNames.add(detectorName);
				
				if (detectorName.contains("individual count") == false)
				{
					interestingDetector = detectorName;
				}
			}
			
			Pagination<Map<String, Object>> interestingDetectorRecords = jobManager.records(jobId, 
					interestingBucket, interestingDetector, 0, 1000);
			if (interestingDetectorRecords.getDocumentCount() == 0)
			{
				throw new IllegalStateException(
						"No records for detector " + interestingDetector);
			}
			
			for (Map<String, Object> record:  interestingDetectorRecords.getDocuments())
			{
				if (record.get("detectorName").equals(interestingDetector) == false)
				{
					throw new IllegalStateException(String.format(
							"Record  detector name = '%', it should be '%'",
							record.get("detectorName"), interestingDetector));
				}
			}


			SingleDocument<Map<String, Object>> bucketDoc = jobManager.bucket(jobId, interestingBucket, true);
			Map<String, Object> bucket = bucketDoc.getDocument();

			@SuppressWarnings("unchecked")
			List<Map<String, Object>> expandedRecords = (List<Map<String, Object>>)bucket.get("records");
			s_Logger.info(String.format("%d records in bucket %s", expandedRecords.size(),
					interestingBucket));



			Pagination<Map<String, Object>> allRecordsPage = 
					jobManager.records(jobId, interestingBucket, 0, 1000); 
			List<Map<String, Object>> allRecords = allRecordsPage.getDocuments(); 
			int allRecordCount = allRecords.size();

			int sum = 0;
			for (String detectorId : detectorNames)
			{
				Pagination<Map<String, Object>> recordsPage = jobManager.records(jobId, 
						interestingBucket, detectorId, 0, 1000);

				sum += recordsPage.getDocuments().size();
			}

			if (sum != allRecordCount)
			{
				throw new IllegalStateException(
						"Total does not equal sum of each detector's records");
			}
		}
		finally
		{
			// delete the job
			deleteIndex(client, jobId);
		}
	}
	
	
	static private void createIndex(Client client, String index) 
	throws IOException
	{
		XContentBuilder jobMapping = ElasticSearchMappings.jobMapping();
		XContentBuilder bucketMapping = ElasticSearchMappings.bucketMapping();
		XContentBuilder detectorMapping = ElasticSearchMappings.detectorMapping();
		XContentBuilder recordMapping = ElasticSearchMappings.recordMapping();
		XContentBuilder detectorStateMapping = ElasticSearchMappings.detectorStateMapping();
					
		CreateIndexRequestBuilder indexBuilder = client.admin().indices()
				.prepareCreate(index)					
				.addMapping(JobDetails.TYPE, jobMapping)
				.addMapping(Bucket.TYPE, bucketMapping)
				.addMapping(Detector.TYPE, detectorMapping)
				.addMapping(AnomalyRecord.TYPE, recordMapping)
				.addMapping(DetectorState.TYPE, detectorStateMapping);
		
		indexBuilder.get();
	}
	
	
	static private void deleteIndex(Client client, String index) 
	throws IOException
	{
		// now delete the index just created
		DeleteIndexResponse deleteResponse = client.admin().indices()
				.prepareDelete(index).get();
		
		if (deleteResponse.isAcknowledged() == false)
		{
			s_Logger.error("Failed to delete index");
		}
	}

}
