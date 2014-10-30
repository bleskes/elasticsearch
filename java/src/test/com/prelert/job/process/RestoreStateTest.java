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
package com.prelert.job.process;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobConfigurationException;
import com.prelert.job.JobDetails;
import com.prelert.job.JobInUseException;
import com.prelert.job.TooManyJobsException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Pagination;


public class RestoreStateTest 
{
	private static final Logger s_Logger = Logger.getLogger(RestoreStateTest.class);
	
	public static final String DEFAULT_CLUSTER_NAME = "prelert";
	
	/**
	 * Elasticsearch must be running for this test.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws UnconfiguredJobException 
	 * @throws NativeProcessRunException 
	 * @throws InterruptedException 
	 * @throws UnknownJobException 
	 * @throws MissingFieldException
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 * @throws JobConfigurationException If the license is violated
	 * @throws TooManyJobsException If the license is violated
	 * @throws JobIdAlreadyExistsException 
	 */
	public static void main(String[] args)
	throws IOException, NativeProcessRunException, UnknownJobException,
		InterruptedException, JobInUseException, MissingFieldException,
		HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
		JobConfigurationException, TooManyJobsException, JobIdAlreadyExistsException
	{
		final String prelertSrcHome = System.getProperty("prelert.src.home");
		if (prelertSrcHome == null)
		{
			s_Logger.error("Error property prelert.src.home is not set");
			return;
		}
		// configure log4j
		ConsoleAppender console = new ConsoleAppender(); 		
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n")); 
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
		
		
		Detector detector = new Detector();
		detector.setFieldName("responsetime");
		detector.setByFieldName("airline");
		List<Detector> d = new ArrayList<>();
		d.add(detector);
		AnalysisConfig config = new AnalysisConfig();
		config.setDetectors(d);
		config.setBucketSpan(3600L);
		
		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter(',');
				
		JobConfiguration jobConfig = new JobConfiguration.JobConfigurationBuilder(config)
				.dataDescription(dd)
				.build();		
		
		String clusterName = DEFAULT_CLUSTER_NAME;
		if (args.length > 0)
		{
			clusterName = args[0];
		}
		s_Logger.info("Using Elasticsearch cluster " + clusterName);
		
		ElasticsearchJobProvider esJob = new ElasticsearchJobProvider(clusterName);
		JobManager jobManager = new JobManager(esJob, null, null, null, null);
		JobDetails job = jobManager.createJob(jobConfig);
		
		s_Logger.info("Created job " + job.getId());
		
		try 
		{
			String input_part_1 = prelertSrcHome + "/gui/apps/autodetectAPI/test_data/flightcentre_forwards_1.csv";
			String input_part_2 = prelertSrcHome + "/gui/apps/autodetectAPI/test_data/flightcentre_forwards_2.csv";

			InputStream fs = new FileInputStream(new File(input_part_1));
			jobManager.dataToJob(job.getId(), fs);
			jobManager.finishJob(job.getId());

			Thread.sleep(2000);

			// now send the next part
			fs = new FileInputStream(new File(input_part_2));
			jobManager.dataToJob(job.getId(), fs);
			jobManager.finishJob(job.getId());
			
			Thread.sleep(1000);

			Pagination<Bucket> buckets = 
					jobManager.buckets(job.getId(), false, 0, 100, 0.0, 0.0);

			List<Double> anomalyScores = new ArrayList<>();
			for (Bucket bucket : buckets.getDocuments())
			{
				anomalyScores.add(bucket.getAnomalyScore());			
			}

			String testResults = prelertSrcHome + "/gui/apps/autodetectAPI/test_data/engine_api_integration_test/flightcentre_split_results.json";

			ObjectMapper mapper = new ObjectMapper();
			List<Map<String,Object>> standardBuckets = mapper.readValue(new File(testResults), 
					new TypeReference<List<Map<String,Object>>>() {});

			if (standardBuckets.size() != anomalyScores.size())
			{
				s_Logger.error(String.format("Number of buckets returned (%d) does not match the size of " 
						+ "saved results (%d)", anomalyScores.size(), standardBuckets.size()));
							return;
			}

			int i=0;
			for (Map<String, Object> bucket : standardBuckets)
			{
				Number score = (Number)bucket.get("anomalyScore");			
				double diff = Math.abs(score.doubleValue() - anomalyScores.get(i));
				if (diff > 0.01)
				{
					s_Logger.error(String.format("Anomaly score does not equal "
							+ "expected anomaly score %d !+ %d", anomalyScores.get(i), score));

					return;
				}

				i++;
			}

		}
		finally
		{
			jobManager.deleteJob(job.getId());
			jobManager.stop();
		}
	}	
}
