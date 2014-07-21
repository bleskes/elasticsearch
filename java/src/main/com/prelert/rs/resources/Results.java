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
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.provider.RestApiException;

/**
 * API results end point.
 * Access buckets and anomaly records, use the <pre>expand</pre> query argument
 * to get buckets and anomaly records in one query. 
 * Buckets can be filtered by date. 
 */
@Path("/results")
public class Results extends ResourceWithJobManager
{
	static private final Logger s_Logger = Logger.getLogger(Results.class);
	
	/**
	 * The name of the results endpoint
	 */
	static public final String ENDPOINT = "results";
	
	
	static public final String NORMALISATION_QUERY_PARAM = "norm";
	

	static private final DateFormat s_DateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT); 
	static private final DateFormat s_DateFormatWithMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS); 
	
	static private final DateFormat [] s_DateFormats = new DateFormat [] {
		s_DateFormat, s_DateFormatWithMs};
	
	
	/**
	 * Get all the bucket results (in pages) for the job optionally filtered 
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
	public Pagination<Bucket> buckets(
			@PathParam("jobId") String jobId,
			@DefaultValue("false") @QueryParam("expand") boolean expand,
			@DefaultValue("0") @QueryParam("skip") int skip,
			@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
			@DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
			@DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
			@DefaultValue("s") @QueryParam(NORMALISATION_QUERY_PARAM) String norm)
	throws NativeProcessRunException
	{	
		s_Logger.debug(String.format("Get %s buckets for job %s. skip = %d, take = %d"
				+ " start = '%s', end='%s'", 
				expand?"expanded ":"", jobId, skip, take, start, end));
		
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
		
		//Normaliser normaliser = normaliser();
		JobManager manager = jobManager();
		Pagination<Bucket> buckets;
		
		if (epochStart > 0 || epochEnd > 0)
		{
			buckets = manager.buckets(jobId, expand, skip, take, epochStart, epochEnd, norm);
		}
		else
		{
			buckets = manager.buckets(jobId, expand, skip, take, norm);
		}
		
		// paging
    	if (buckets.isAllResults() == false)
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
    		
    		setPagingUrls(path, buckets, queryParams);
    	}		
			
		s_Logger.debug(String.format("Return %d buckets for job %s", 
				buckets.getDocumentCount(), jobId));
		
		return buckets;
	}
	
	
	/**
	 * Get an individual bucket results
	 * @param jobId
	 * @param bucketId
	 * @param expand Return anomaly records in-line with the bucket,
	 * default is false
	 * @return
	 */
	@GET
	@Path("/{jobId}/{bucketId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response bucket(@PathParam("jobId") String jobId,
			@PathParam("bucketId") String bucketId,
			@DefaultValue("false") @QueryParam("expand") boolean expand,
			@DefaultValue("s") @QueryParam(NORMALISATION_QUERY_PARAM) String norm)
	throws NativeProcessRunException
	{
		s_Logger.debug(String.format("Get %sbucket %s for job %s", 
				expand?"expanded ":"", bucketId, jobId));
		
		JobManager manager = jobManager();
		SingleDocument<Bucket> bucket = manager.bucket(jobId, bucketId, expand, norm);
		
		if (bucket.isExists())
		{
			s_Logger.debug(String.format("Returning bucket %s for job %s", 
					bucketId, jobId));
		}
		else
		{
			s_Logger.debug(String.format("Cannot find bucket %s for job %s", 
					bucketId, jobId));
			
			return Response.status(Response.Status.NOT_FOUND).entity(bucket).build();
		}
					
		return Response.ok(bucket).build();
	}
	
	
	/**
	 * Get the anomaly records for the bucket.
	 * 
	 * @param jobId
	 * @param bucketId
	 * @param skip
	 * @param take
	 * @return
	 */
	@Path("/{jobId}/{bucketId}/records")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Pagination<AnomalyRecord> bucketRecords(
			@PathParam("jobId") String jobId,
			@PathParam("bucketId") String bucketId,
			@DefaultValue("0") @QueryParam("skip") int skip,
			@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take)
	throws NativeProcessRunException
	{
		s_Logger.debug(String.format("Get records for job %s, bucket %s", 
				jobId, bucketId));
		
		JobManager manager = jobManager();
		Pagination<AnomalyRecord> records = manager.records(
				jobId, bucketId, skip, take);
		
		// paging
    	if (records.isAllResults() == false)
    	{
    		String path = new StringBuilder()
    							.append("/results/")
    							.append(jobId)
    							.append("/")
								.append(bucketId)
								.append("/records")
								.toString();
    		
    		setPagingUrls(path, records);
    	}
		
		s_Logger.debug(String.format("Returning %d records for job %s, bucket %s", 
				records.getDocuments().size(), jobId, bucketId));
					
		return records;
	}
		
}
