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

package com.prelert.rs.resources;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.elasticsearch.ElasticsearchMappings;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.provider.RestApiException;

/**
 * API record results end point.
 * Access anomaly records filtered by date with various sort options
 */
@Path("/results")
public class Records extends ResourceWithJobManager
{
	private static final Logger LOGGER = Logger.getLogger(Records.class);
	
	/**
	 * The name of the records endpoint
	 */
	public static final String ENDPOINT = "records";
	
	/**
	 * Sort field query parameter
	 */
	public static final String SORT_QUERY_PARAM = "sort";
	/**
	 * Sort direction
	 */
	public static final String DESCENDING_ORDER = "desc";
	

	private static final DateFormat s_DateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT); 
	private static final DateFormat s_DateFormatWithMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS); 
	
	private static final DateFormat [] s_DateFormats = new DateFormat [] {
		s_DateFormat, s_DateFormatWithMs};
	
	
	/**
	 * Get all the records (in pages) for the job optionally filtered 
	 * by date.
	 * 
	 * @param jobId
	 * @param skip
	 * @param take
	 * @param start The filter start date see {@linkplain #paramToEpoch(String)}
	 * for the format the date string should take
	 * @param end The filter end date see {@linkplain #paramToEpoch(String)}
	 * for the format the date string should take
	 * @return
	 */
	@GET
	@Path("/{jobId}/records")
	@Produces(MediaType.APPLICATION_JSON)
	public Pagination<AnomalyRecord> records(
			@PathParam("jobId") String jobId,
			@DefaultValue("0") @QueryParam("skip") int skip,
			@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
			@DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
			@DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
			@DefaultValue(AnomalyRecord.NORMALIZED_PROBABILITY) @QueryParam(SORT_QUERY_PARAM) String sort,
			@DefaultValue("true") @QueryParam(DESCENDING_ORDER) boolean descending,
			@DefaultValue("0.0") @QueryParam(AnomalyRecord.ANOMALY_SCORE) double anomalySoreFilter,
			@DefaultValue("0.0") @QueryParam(AnomalyRecord.NORMALIZED_PROBABILITY) double normalizedProbabilityFilter)
	throws NativeProcessRunException, UnknownJobException
	{	
		LOGGER.debug(String.format("Get records for job %s. skip = %d, take = %d"
				+ " start = '%s', end='%s', sort='%s' descending=%b"  
				+ ", anomaly score filter=%f, unsual score filter= %f", 
				jobId, skip, take, start, end, sort, descending,
				normalizedProbabilityFilter, anomalySoreFilter));
		
		long epochStartMs = 0;
		if (start.isEmpty() == false)
		{
			epochStartMs = paramToEpoch(start, s_DateFormats);	
			if (epochStartMs == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, start);
				LOGGER.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}
		}
		
		long epochEndMs = 0;
		if (end.isEmpty() == false)
		{
			epochEndMs = paramToEpoch(end, s_DateFormats);	
			if (epochEndMs == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, end);
				LOGGER.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}			
		}
			
		JobManager manager = jobManager();
		Pagination<AnomalyRecord> records;


		// HACK - the API renames @timestamp to timestamp
		// but it is @timestamp in the database for Kibana
		if (Bucket.TIMESTAMP.equals(sort))
		{
			sort = ElasticsearchMappings.ES_TIMESTAMP;
		}
		
		if (epochStartMs > 0 || epochEndMs > 0)
		{
			records = manager.records(jobId, skip, take, epochStartMs, epochEndMs, sort,
					descending, anomalySoreFilter, normalizedProbabilityFilter);
		}
		else
		{
			records = manager.records(jobId, skip, take, sort, descending,
					anomalySoreFilter, normalizedProbabilityFilter);
		}

		// paging
    	if (records.isAllResults() == false)
    	{
    		String path = new StringBuilder()
								.append("/results/")
								.append(jobId)
								.append("/records/")
								.toString();
    		
    		List<ResourceWithJobManager.KeyValue> queryParams = new ArrayList<>();
    		if (epochStartMs > 0)
    		{
    			queryParams.add(this.new KeyValue(START_QUERY_PARAM, start));
    		}
    		if (epochEndMs > 0)
    		{
    			queryParams.add(this.new KeyValue(END_QUERY_PARAM, end));
    		}
    		queryParams.add(this.new KeyValue(SORT_QUERY_PARAM, sort));
    		queryParams.add(this.new KeyValue(DESCENDING_ORDER, Boolean.toString(descending)));
    		queryParams.add(this.new KeyValue(AnomalyRecord.ANOMALY_SCORE, String.format("%2.1f", anomalySoreFilter)));
    		queryParams.add(this.new KeyValue(AnomalyRecord.NORMALIZED_PROBABILITY, String.format("%2.1f", normalizedProbabilityFilter)));
    		
    		setPagingUrls(path, records, queryParams);
    	}		
			
		LOGGER.debug(String.format("Return %d records for job %s", 
				records.getDocumentCount(), jobId));
		
		return records;
	}
		
}
