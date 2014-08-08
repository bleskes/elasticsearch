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
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.job.alert.Alert;
import com.prelert.job.usage.Usage;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Quantiles;

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
	 * Name of the field used to store the timestamp in Elasticsearch. 
	 * Note the field name is different to {@link com.prelert.rs.data.Bucket#TIMESTAMP} used by the 
	 * API Bucket Resource, and is chosen for consistency with the default field name used by
	 * Logstash and Kibana.
	 */
	static final public String ES_TIMESTAMP = "@timestamp";
	
	
	/**
	 * Create the ElasticSearch mapping for {@linkplain com.prelert.job.JobDetails}.
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
						.startObject(JobDetails.DESCRIPTION)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()						
						.startObject(JobDetails.STATUS)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(JobDetails.CREATE_TIME)
							.field("type", "date")
						.endObject()
						.startObject(JobDetails.FINISHED_TIME)
							.field("type", "date")
						.endObject()
						.startObject(JobDetails.LAST_DATA_TIME)
							.field("type", "date")
						.endObject()		
						.startObject(JobDetails.COUNTS)
							.field("type", "object")
							.startObject("properties")
								.startObject(JobDetails.PROCESSED_RECORD_COUNT)
									.field("type", "long")
								.endObject()
								.startObject(JobDetails.PROCESSED_BYTES)
								.field("type", "long")
								.endObject()								
								.startObject(JobDetails.INVALID_DATE_COUNT)
									.field("type", "long")
								.endObject()
								.startObject(JobDetails.MISSING_FIELD_COUNT)
									.field("type", "long")
								.endObject()
								.startObject(JobDetails.OUT_OF_ORDER_TIME_COUNT)
									.field("type", "long")
								.endObject()
							.endObject()
						.endObject()
						.startObject(JobDetails.TIMEOUT)
							.field("type", "long").field(INDEX, NO)
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
								.startObject(AnalysisConfig.DETECTORS)
									.startObject("properties")
										.startObject(Detector.FUNCTION)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()									
										.startObject(Detector.FIELD_NAME)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()	
										.startObject(Detector.BY_FIELD_NAME)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()	
										.startObject(Detector.OVER_FIELD_NAME)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()		
										.startObject(Detector.PARTITION_FIELD_NAME)
											.field("type", "string").field(INDEX, NOT_ANALYZED)
										.endObject()
										.startObject(Detector.USE_NULL)
											.field("type", "boolean").field(INDEX, NOT_ANALYZED)
										.endObject()	
									.endObject()
								.endObject()
							.endObject()
						.endObject()						
						.startObject(JobDetails.ANALYSIS_LIMITS)
							.field("type", "object")
							.startObject("properties")
								.startObject(AnalysisLimits.MAX_FIELD_VALUES)
										.field("type", "long").field(INDEX, NO)
								.endObject()
								.startObject(AnalysisLimits.MAX_TIME_BUCKETS)
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
	 * Create the ElasticSearch mapping for {@linkplain com.prelert.rs.data.Bucket}.
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
							.field("type", "string")
						.endObject()
						.startObject(ES_TIMESTAMP)
							.field("type", "date")
						.endObject()						
						.startObject(Bucket.RAW_ANOMALY_SCORE)
							.field("type", "double")
						.endObject()
						.startObject(Bucket.ANOMALY_SCORE)
							.field("type", "double")
						.endObject()
						.startObject(Bucket.UNUSUAL_SCORE)
							.field("type", "double")
						.endObject()
						.startObject(Bucket.RECORD_COUNT)
							.field("type", "long")
						.endObject()	
					.endObject()
				.endObject()
		.endObject();
			
		return mapping;
	}
	
	
	/**
	 * Create the ElasticSearch mapping for {@linkplain com.prelert.rs.data.Detector}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 * 
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder detectorMapping() 
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(com.prelert.rs.data.Detector.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("properties")
						.startObject(com.prelert.rs.data.Detector.NAME)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()					
					.endObject()
				.endObject()
			.endObject();
			
		return mapping;
	}
	
	
	/**
	 * Create the ElasticSearch mapping for {@linkplain com.prelert.rs.data.Detector}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
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
						.startObject(ES_TIMESTAMP)
							.field("type", "date")
						.endObject()
						.startObject(AnomalyRecord.ACTUAL)
							.field("type", "double").field(INDEX, NOT_ANALYZED)
						.endObject()						
						.startObject(AnomalyRecord.TYPICAL)
							.field("type", "double").field(INDEX, NOT_ANALYZED)
						.endObject()						
						.startObject(AnomalyRecord.PROBABILITY)
							.field("type", "double")
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
							.field("type", "boolean")
						.endObject()
						.startObject(AnomalyRecord.IS_SIMPLE_COUNT)
							.field("type", "boolean")
						.endObject()
						.startObject(Bucket.ANOMALY_SCORE)
							.field("type", "double")
						.endObject()
						.startObject(Bucket.UNUSUAL_SCORE)
							.field("type", "double")
						.endObject()
					.endObject()
				.endObject()
			.endObject();

		return mapping;
	}


	/**
	 * Create the ElasticSearch mapping for {@linkplain Quantiles}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 *
	 * The quantile state string is not searchable (index = 'no') as it could be
	 * very large.
	 *
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder quantilesMapping()
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(Quantiles.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("properties")
						.startObject(Quantiles.QUANTILE_STATE)
							.field("type", "string").field(INDEX, NO)
						.endObject()
					.endObject()
				.endObject()
			.endObject();

		return mapping;
	}


	/**
	 * Create the ElasticSearch mapping for {@linkplain DetectorState}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
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
	
	/**
	 * The Elasticsearch mappings for the usage documents
	 * 
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder usageMapping() 
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(Usage.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("properties")
						.startObject(Usage.TIMESTAMP)
							.field("type", "date")
						.endObject()	
						.startObject(Usage.VOLUME)
							.field("type", "long")
						.endObject()					
					.endObject()
				.endObject()
			.endObject();
			
		return mapping;
	}
	
	
	/**
	 * The Elasticsearch mappings for {@link Alert}s 
	 * 
	 * @return
	 * @throws IOException
	 */
	static public XContentBuilder alertMapping() 
	throws IOException
	{
		XContentBuilder mapping = jsonBuilder()
			.startObject()
				.startObject(Alert.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("properties")	
						.startObject(Alert.ID)
							.field("type", "string").field(INDEX, NOT_ANALYZED)
						.endObject()					
						.startObject(Alert.JOB_ID)
						 	.field("type", "string")
						.endObject()
						.startObject(Alert.SEVERTIY)
							.field("type", "string")
						.endObject()					
						.startObject(Alert.TIMESTAMP)
						    .field("type", "date")
						.endObject()					
						.startObject(Alert.REASON)
						    .field("type", "string")
						.endObject()									
					.endObject()
				.endObject()
			.endObject();
			
		return mapping;
	}
}
