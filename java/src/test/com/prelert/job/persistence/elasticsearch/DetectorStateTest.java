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

package com.prelert.job.persistence.elasticsearch;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;

import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.parsing.AutoDetectParseException;
import com.prelert.rs.data.parsing.AutoDetectResultsParser;


/**
 * ElasticSearch store detector state integration test.<br/>
 * Parses the state out of a file and writes the parsed detector state to 
 * ElasticSearch then queries it back. Note it has to sleep for 1 second 
 * between the write and query to give ES time to index the doc.<br/>
 *  
 * If a file path is passed as an argument that will be used else the default 
 * file PRELERT_TEST_DATA_HOME/engine_api_integration_test/model.json is used.
 * The system property 'prelert.test.data.home' must be set if no argument is passed
 */
public class DetectorStateTest 
{
	public static final String DEFAULT_FILE = "/engine_api_integration_test/model.json";
	
	private static final Logger s_Logger = Logger.getLogger(DetectorStateTest.class);

	
	public static void main(String[] args) 
	throws IOException, AutoDetectParseException, InterruptedException 
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender(); 		
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n")); 
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
		  		
		String filePath = null;
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
		}
		else
		{
			filePath = args[0];
		}
		s_Logger.info("Using file " + filePath);		
		
		final String JOBID = "test_detector_state";
		
		// setup elastic search
		Node node = nodeBuilder().client(true).node();
		Client client = node.client();
		ElasticSearchPersister es = new ElasticSearchPersister(JOBID, client);
		
		try
		{
			IndicesExistsResponse existsResponse = 
				client.admin().indices().exists(new IndicesExistsRequest(JOBID)).get();
			if (existsResponse.isExists())
			{
				deleteIndex(client, JOBID);
			}
		}
		catch (ExecutionException|InterruptedException e)
		{
			s_Logger.error(e);
		} 
		
		// Create a ElasticSearch index and write results to it		
		createIndex(client, JOBID);		
		try
		{
			InputStream fs = new FileInputStream(new File(filePath));
			AutoDetectResultsParser.parseResults(fs, es);
			
			
			FilterBuilder fb = FilterBuilders.matchAllFilter();
			
			SearchRequestBuilder searchBuilder = client.prepareSearch(JOBID)
					.setTypes(DetectorState.TYPE)		
					.setFilter(fb)
					.setFrom(0).setSize(50)
					.addField(DetectorState.DETECTOR_NAME);
			
			// Must sleep for a second to be sure the documents are indexed
			Thread.sleep(1000);
			
			SearchResponse searchResponse = searchBuilder.get();

			
			for (SearchHit hit : searchResponse.getHits().getHits())
			{
				s_Logger.info(hit.field(DetectorState.DETECTOR_NAME).getName());
			}
		}
		finally
		{
			deleteIndex(client, JOBID);
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
			
		client.admin().indices()
				.prepareCreate(index)					
				.addMapping(JobDetails.TYPE, jobMapping)
				.addMapping(Bucket.TYPE, bucketMapping)
				.addMapping(Detector.TYPE, detectorMapping)
				.addMapping(AnomalyRecord.TYPE, recordMapping)
				.addMapping(DetectorState.TYPE, detectorStateMapping)
				.get();
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
