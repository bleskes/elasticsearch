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

import com.prelert.job.manager.JobManager;
import com.prelert.job.normalisation.NormalizationType;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.provider.RestApiException;

/**
 * API results end point.
 * Access buckets and anomaly records, use the <pre>expand</pre> query argument
 * to get buckets and anomaly records in one query. 
 * Buckets can be filtered by date. 
 */
@Path("/records")
public class Records extends ResourceWithJobManager
{
	static private final Logger s_Logger = Logger.getLogger(Records.class);
	
	/**
	 * The name of the records endpoint
	 */
	static public final String ENDPOINT = "records";
	
	/**
	 * Sort order query parameter
	 */
	static public final String SORT_QUERY_PARAM = "sort";
	
	static public final String NORMALISATION_QUERY_PARAM = "norm";
	
	/**
	 * Possible arguments to the sort parameter
	 */
	static public final String PROB_SORT_VALUE = "prob";
	//static public final String DATE_SORT_VALUE = "date";
	
	
	static private final DateFormat s_DateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT); 
	static private final DateFormat s_DateFormatWithMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS); 
	
	static private final DateFormat [] s_DateFormats = new DateFormat [] {
		s_DateFormat, s_DateFormatWithMs};
	
	
	/**
	 * Get all the records (in pages) for the job optionally filtered 
	 * by date.
	 * 
	 * @param jobId
	 * @param expand Return anomaly records in-line with the results,
	 *  default is false
	 * @param skip
	 * @param take
	 * @param start The filter start date see {@linkplain #paramToEpoch(String)}
	 * for the format the date string should take
	 * @param end The filter end date see {@linkplain #paramToEpoch(String)}
	 * for the format the date string should take
	 * @return
	 */
	@GET
	@Path("/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Pagination<AnomalyRecord> records(
			@PathParam("jobId") String jobId,
			@DefaultValue("0") @QueryParam("skip") int skip,
			@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
			@DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
			@DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
			@DefaultValue(PROB_SORT_VALUE) @QueryParam(SORT_QUERY_PARAM) String sort,
			@DefaultValue("both") @QueryParam(NORMALISATION_QUERY_PARAM) String norm)
	throws NativeProcessRunException
	{	
		s_Logger.debug(String.format("Get records for job %s. skip = %d, take = %d"
				+ " start = '%s', end='%s', sort='%s', norm='%s'", 
				jobId, skip, take, start, end, sort, norm));
		
		long epochStart = 0;
		if (start.isEmpty() == false)
		{
			epochStart = paramToEpoch(start, s_DateFormats);	
			if (epochStart == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, start);
				s_Logger.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}
		}
		
		long epochEnd = 0;
		if (end.isEmpty() == false)
		{
			epochEnd = paramToEpoch(end, s_DateFormats);	
			if (epochEnd == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, end);
				s_Logger.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}			
		}
		
		// only sort by probability for now
		if (!sort.equals(PROB_SORT_VALUE))
		{
			String msg = String.format(String.format("'%s is not a valid value "
					+ "for the sort query parameter", sort));
			s_Logger.warn(msg);
			throw new RestApiException(msg, ErrorCode.INVALID_SORT_FIELD,
					Response.Status.BAD_REQUEST);
		}
		
		NormalizationType normType;
		try
		{
			normType = NormalizationType.fromString(norm);
		}
		catch (IllegalArgumentException e)
		{
			String msg = String.format(String.format("'%s is not a valid value "
					+ "for the normalisation query parameter", norm));
			s_Logger.warn(msg);
			throw new RestApiException(msg, ErrorCode.INVALID_NORMALIZATION_ARG,
					Response.Status.BAD_REQUEST);
		}
		
		long start_ms = System.currentTimeMillis();
		
		JobManager manager = jobManager();
		Pagination<AnomalyRecord> records;

		if (epochStart > 0 || epochEnd > 0)
		{
			records = manager.records(jobId, skip, take, epochStart, epochEnd, sort, normType);
		}
		else
		{
			records = manager.records(jobId, skip, take, sort, normType);
		}
		
		System.out.println(String.format("Normalised records in %d ms",
				System.currentTimeMillis() - start_ms));

		
		// paging
    	if (records.isAllResults() == false)
    	{
    		String path = new StringBuilder()
								.append("/results/")
								.append(jobId)
								.toString();
    		
    		List<ResourceWithJobManager.KeyValue> queryParams = new ArrayList<>();
    		if (epochStart > 0)
    		{
    			queryParams.add(this.new KeyValue(START_QUERY_PARAM, start));
    		}
    		if (epochEnd > 0)
    		{
    			queryParams.add(this.new KeyValue(END_QUERY_PARAM, end));
    		}
    		
    		setPagingUrls(path, records, queryParams);
    	}		
			
		s_Logger.debug(String.format("Return %d records for job %s", 
				records.getDocumentCount(), jobId));
		
		return records;
	}
		
}
