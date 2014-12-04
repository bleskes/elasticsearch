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

package com.prelert.job.persistence.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelState;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.usage.Usage;
import com.prelert.rs.data.AnomalyCause;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;

/**
 * Static methods to create Elasticsearch mappings for the autodetect
 * persisted objects/documents
  */
public class ElasticsearchMappings
{
	/**
	 * String constants used in mappings
	 */
	public static final String NOT_ANALYZED = "not_analyzed";
	public static final String INDEX = "index";
	public static final String NO = "no";

	/**
	 * Name of the field used to store the timestamp in Elasticsearch.
	 * Note the field name is different to {@link com.prelert.rs.data.Bucket#TIMESTAMP} used by the
	 * API Bucket Resource, and is chosen for consistency with the default field name used by
	 * Logstash and Kibana.
	 */
	public static final String ES_TIMESTAMP = "@timestamp";

	private ElasticsearchMappings()
	{
	}

	/**
	 * Create the Elasticsearch mapping for {@linkplain com.prelert.job.JobDetails}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 *
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder jobMapping()
	throws IOException
	{
		return jsonBuilder()
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
								.startObject(JobDetails.BUCKET_COUNT)
									.field("type", "long")
								.endObject()
								.startObject(JobDetails.PROCESSED_RECORD_COUNT)
									.field("type", "long")
								.endObject()
								.startObject(JobDetails.PROCESSED_FIELD_COUNT)
									.field("type", "long")
								.endObject()
								.startObject(JobDetails.INPUT_BYTES)
								.field("type", "long")
								.endObject()
								.startObject(JobDetails.INPUT_RECORD_COUNT)
								.field("type", "long")
								.endObject()
								.startObject(JobDetails.INPUT_FIELD_COUNT)
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
						.startObject(ModelSizeStats.TYPE)
							.field("type", "object")
							.startObject("properties")
								.startObject(ModelSizeStats.MODEL_BYTES)
									.field("type", "long")
								.endObject()
								.startObject(ModelSizeStats.TOTAL_BY_FIELD_COUNT)
									.field("type", "long")
								.endObject()
								.startObject(ModelSizeStats.TOTAL_OVER_FIELD_COUNT)
									.field("type", "long")
								.endObject()
								.startObject(ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT)
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
								.startObject(AnalysisLimits.MODEL_MEMORY_LIMIT)
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
	}


	/**
	 * Create the Elasticsearch mapping for {@linkplain com.prelert.rs.data.Bucket}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 *
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder bucketMapping()
	throws IOException
	{
		return jsonBuilder()
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
						.startObject(Bucket.MAX_NORMALIZED_PROBABILITY)
							.field("type", "double")
						.endObject()
						.startObject(Bucket.RECORD_COUNT)
							.field("type", "long")
						.endObject()
						.startObject(Bucket.EVENT_COUNT)
							.field("type", "long")
						.endObject()
					.endObject()
				.endObject()
		.endObject();
	}


	/**
	 * Create the Elasticsearch mapping for {@linkplain com.prelert.rs.data.Detector}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 *
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder detectorMapping()
	throws IOException
	{
		return jsonBuilder()
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
	}


	/**
	 * Create the Elasticsearch mapping for {@linkplain com.prelert.rs.data.Detector}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 * Records have a _parent mapping to a {@linkplain com.prelert.rs.data.Bucket}.
	 *
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder recordMapping()
	throws IOException
	{
		return jsonBuilder()
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
						.startObject(AnomalyRecord.CAUSES)
							.startObject("properties")
								.startObject(AnomalyCause.ACTUAL)
									.field("type", "double").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.TYPICAL)
									.field("type", "double").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.PROBABILITY)
									.field("type", "double")
								.endObject()
								.startObject(AnomalyCause.FUNCTION)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.BY_FIELD_NAME)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.BY_FIELD_VALUE)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.FIELD_NAME)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.PARTITION_FIELD_NAME)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.PARTITION_FIELD_VALUE)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.OVER_FIELD_NAME)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
								.startObject(AnomalyCause.OVER_FIELD_VALUE)
									.field("type", "string").field(INDEX, NOT_ANALYZED)
								.endObject()
							.endObject()
						.endObject()
						.startObject(AnomalyRecord.ANOMALY_SCORE)
							.field("type", "double")
						.endObject()
						.startObject(AnomalyRecord.NORMALIZED_PROBABILITY)
							.field("type", "double")
						.endObject()
					.endObject()
				.endObject()
			.endObject();
	}


	/**
	 * Create the Elasticsearch mapping for {@linkplain Quantiles}.
	 * The '_all' field is disabled as the document isn't meant to be searched.
	 *
	 * The quantile state string is not searchable (index = 'no') as it could be
	 * very large.
	 *
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder quantilesMapping()
	throws IOException
	{
		return jsonBuilder()
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
	}


	/**
	 * Create the Elasticsearch mapping for {@linkplain ModelState}.
	 * The model state could potentially be huge (over a gigabyte in size)
	 * so all analysis by Elasticsearch is disabled.  The only way to
	 * retrieve the model state is by knowing the ID of a particular
	 * document or by searching for all documents of this type.
	 *
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder modelStateMapping()
	throws IOException
	{
		return jsonBuilder()
			.startObject()
				.startObject(ModelState.TYPE)
					.field("enabled", false)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
				.endObject()
			.endObject();
	}


	/**
	 * Create the Elasticsearch mapping for {@linkplain ModelSizeStats}.
	 * TODO
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder modelSizeStatsMapping()
	throws IOException
	{
		return jsonBuilder()
			.startObject()
				.startObject(ModelSizeStats.TYPE)
					.startObject("_all")
						.field("enabled",  false)
					.endObject()
					.startObject("properties")
						.startObject(ModelSizeStats.MODEL_BYTES)
						    .field("type", "long")
						.endObject()
						.startObject(ModelSizeStats.TOTAL_BY_FIELD_COUNT)
							.field("type", "long")
						.endObject()
    					.startObject(ModelSizeStats.TOTAL_OVER_FIELD_COUNT)
							.field("type", "long")
						.endObject()
						.startObject(ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT)
							.field("type", "long")
						.endObject()
					.endObject()
				.endObject()
			.endObject();
	}


	/**
	 * The Elasticsearch mappings for the usage documents
	 *
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder usageMapping()
	throws IOException
	{
		return jsonBuilder()
			.startObject()
				.startObject(Usage.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("properties")
						.startObject(Usage.TIMESTAMP)
							.field("type", "date")
						.endObject()
						.startObject(Usage.INPUT_BYTES)
							.field("type", "long")
						.endObject()
						.startObject(Usage.INPUT_FIELD_COUNT)
							.field("type", "long")
						.endObject()
						.startObject(Usage.INPUT_RECORD_COUNT)
							.field("type", "long")
						.endObject()
					.endObject()
				.endObject()
			.endObject();
	}


	/**
	 * Mapping for the saved data records
	 *
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder inputDataMapping()
	throws IOException
	{
		return jsonBuilder()
			.startObject()
				.startObject(ElasticsearchJobDataPersister.TYPE)
					.startObject("_all")
						.field("enabled", false)
					.endObject()
					.startObject("properties")
						.startObject("epoch")
							.field("type", "date")
						.endObject()
						.startObject(ElasticsearchJobDataPersister.FIELDS)
							.field("type", "string")
							.field("index_name", "field")
							.field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(ElasticsearchJobDataPersister.BY_FIELDS)
							.field("type", "string")
							.field("index_name", "byField")
							.field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(ElasticsearchJobDataPersister.OVER_FIELDS)
							.field("type", "string")
							.field("index_name", "overField")
							.field(INDEX, NOT_ANALYZED)
						.endObject()
						.startObject(ElasticsearchJobDataPersister.PARTITION_FIELDS)
							.field("type", "string")
							.field("index_name", "partitionField")
							.field(INDEX, NOT_ANALYZED)
						.endObject()
					.endObject()
				.endObject()
			.endObject();
	}

}
