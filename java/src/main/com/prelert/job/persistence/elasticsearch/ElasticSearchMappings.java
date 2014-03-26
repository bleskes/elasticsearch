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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisOptions;
import com.prelert.job.DataDescription;
import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;

/**
 * Static methods to create ElasticSearch mappings for the autodetect 
 * persisted objects/documents 
  */
public class ElasticSearchMappings 
{
	/**
	 * String constants used in mappings
	 */
	static final public String NOT_ANALYZED = "not_analyzed";
	static final public String INDEX = "index";
	static final public String NO = "no";
	
	
	/**
	 * Create the ElasticSearch mapping for {@linkplain com.prelert.job.JobDetails}.</br>
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 * 
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder jobMapping()
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(JobDetails.TYPE)
					.startObject("properties")
						.startObject(JobDetails.ID)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(JobDetails.STATUS)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(JobDetails.CREATE_TIME)
							.field("type", "date").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(JobDetails.FINISHED_TIME)
							.field("type", "date").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(JobDetails.LAST_DATA_TIME)
							.field("type", "date").field(INDEX, NOT_ANALYZED)
						.endObject()						
						.startObject(JobDetails.PROCESSED_RECORD_COUNT)
							.field("type", "long").field(INDEX, NO)
						.endObject()
						.startObject(JobDetails.FILE_URLS) // is an array of string
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(JobDetails.TIMEOUT)
							.field("type", "long").field(INDEX, NO)
						.endObject()						
						.startObject(JobDetails.PERSIST_MODEL)
							.field("type", "boolean").field(INDEX, NO)
						.endObject()						
						.startObject(JobDetails.ANALYSIS_CONFIG)
							.field("type", "object")
							.startObject("properties")
								.startObject(AnalysisConfig.BUCKET_SPAN)
									.field("type", "long").field(INDEX, NO)
								.endObject()
								.startObject(AnalysisConfig.BATCH_SPAN)
									.field("type", "long").field(INDEX, NO)
								.endObject()
								.startObject(AnalysisConfig.PERIOD)
									.field("type", "long").field(INDEX, NO)
								.endObject()
								.startObject(AnalysisConfig.PARTITION_FIELD)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnalysisConfig.DETECTORS)
									.startObject("properties")
										.startObject(AnalysisConfig.FUNCTION)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()									
										.startObject(AnalysisConfig.FIELD_NAME)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()	
										.startObject(AnalysisConfig.BY_FIELD_NAME)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()	
										.startObject(AnalysisConfig.OVER_FIELD_NAME)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()		
										.startObject(AnalysisConfig.USE_NULL)
											.field("type", "boolean").field(INDEX, NOT_ANALYZED)
										.endObject()	
									.endObject()
								.endObject()
							.endObject()
						.endObject()						
						.startObject(JobDetails.ANALYSIS_OPTIONS)
							.field("type", "object")
							.startObject("properties")
								.startObject(AnalysisOptions.MAX_FIELD_VALUES)
										.field("type", "long").field(INDEX, NO)
								.endObject()
								.startObject(AnalysisOptions.MAX_TIME_BUCKETS)
										.field("type", "long").field(INDEX, NO)
								.endObject()		
							.endObject()	
						.endObject()									
						.startObject(JobDetails.DATA_DESCRIPTION)
							.field("type", "object")
							.startObject("properties")
								.startObject(DataDescription.FORMAT)
										.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(DataDescription.TIME_FIELD_NAME)
										.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()	
								.startObject(DataDescription.TIME_FORMAT)
										.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(DataDescription.FIELD_DELIMITER)
										.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(DataDescription.QUOTE_CHARACTER)
										.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()									
							.endObject()	
						.endObject()																		
					.endObject() 
					.startObject("_all")
						.field("enabled", "false")
					.endObject()								
				.endObject()
			.endObject();
		
		return mapping;
	}
	
	
	/**
	 * Create the ElasticSearch mapping for {@linkplain com.prelert.rs.data.Bucket}.</br>
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 * 
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder bucketMapping() 
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(Bucket.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()				
					.startObject("properties")
						.startObject(Bucket.ID)
							.field("type", "long").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(Bucket.TIMESTAMP)
							.field("type", "date").field(INDEX, NOT_ANALYZED)
						.endObject()						
						.startObject(Bucket.ANOMALY_SCORE)
							.field("type", "double").field(INDEX, NO)
						.endObject()
						.startObject(Bucket.RECORD_COUNT)
							.field("type", "long").field(INDEX, NO)
						.endObject()	
					.endObject()
				.endObject()
		.endObject();
			
		return mapping;
	}
	
	
	/**
	 * Create the ElasticSearch mapping for {@linkplain com.prelert.rs.data.Detector}.</br>
	 * The '_all' field is disabled as the document isn't meant to be searched.</br>
	 * 
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder detectorMapping() 
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(Detector.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("properties")
						.startObject(Detector.NAME)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()					
					.endObject()
				.endObject()
			.endObject();
			
		return mapping;
	}
	
	
	/**
	 * Create the ElasticSearch mapping for {@linkplain com.prelert.rs.data.Detector}.</br>
	 * The '_all' field is disabled as the document isn't meant to be searched.</br>
	 * 
	 * Records have a _parent mapping to a {@linkplain com.prelert.rs.data.Bucket}.
	 * 
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder recordMapping() 
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(AnomalyRecord.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("_parent")
						.field("type", Bucket.TYPE)
					.endObject()
					.startObject("properties")
						.startObject(AnomalyRecord.DETECTOR_NAME)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()	
						.startObject(AnomalyRecord.ANOMALY_SCORE)
							.field("type", "double").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(AnomalyRecord.ACTUAL)
							.field("type", "double").field(INDEX, NO)
						.endObject()						
						.startObject(AnomalyRecord.TYPICAL)
							.field("type", "double").field(INDEX, NO)
						.endObject()						
						.startObject(AnomalyRecord.PROBABILITY)
							.field("type", "double").field(INDEX, NO)
						.endObject()		
						.startObject(AnomalyRecord.FUNCTION)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(AnomalyRecord.BY_FIELD_NAME)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()	
						.startObject(AnomalyRecord.BY_FIELD_VALUE)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()		
						.startObject(AnomalyRecord.FIELD_NAME)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(AnomalyRecord.PARTITION_FIELD_NAME)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()		
						.startObject(AnomalyRecord.PARTITION_FIELD_VALUE)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(AnomalyRecord.OVER_FIELD_NAME)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()		
						.startObject(AnomalyRecord.OVER_FIELD_VALUE)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(AnomalyRecord.IS_OVERALL_RESULT)
							.field("type", "boolean").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(AnomalyRecord.IS_SIMPLE_COUNT)
							.field("type", "boolean").field(INDEX, NOT_ANALYZED)
						.endObject()
					.endObject()
				.endObject()
			.endObject();
			
		return mapping;
	}
	
	/**
	 * Create the ElasticSearch mapping for {@linkplain DetectorState}.</br>
	 * The '_all' field is disabled as the document isn't meant to be searched.</br>
	 * 
	 * The model state string is not searchable (index = 'no') as it could be
	 * very large.  
	 * 
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder detectorStateMapping() 
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(DetectorState.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("properties")
						.startObject(DetectorState.DETECTOR_NAME)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()	
						.startObject(DetectorState.SERIALISED_MODEL)
							.field("type", "string").field(INDEX, NO)
						.endObject()					
					.endObject()
				.endObject()
			.endObject();
			
		return mapping;
	}
}
