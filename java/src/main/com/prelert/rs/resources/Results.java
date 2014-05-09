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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.manager.JobManager;
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
	
	/**
	 * The bucket filter 'start' query parameter
	 */
	static public final String START_QUERY_PARAM = "start";
	
	/**
	 * The bucket filter 'end' query parameter
	 */
	static public final String END_QUERY_PARAM = "end";
	
	/**
	 * Date query param format
	 */
	static private final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
	/**
	 * Date query param format
	 */
	static private final String ISO_8601_DATE_FORMAT_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
	
	static private final DateFormat s_DateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT); 
	static private final DateFormat s_DateFormatWithMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS); 
	
	
	
	static private final String BAD_DATE_FROMAT_MSG = "Error: Query param '%s' with value" 
						+ " '%s' cannot be parsed as a date or converted to a number (epoch)";
	
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
	public Pagination<Map<String, Object>> buckets(
			@PathParam("jobId") String jobId,
			@DefaultValue("false") @QueryParam("expand") boolean expand,
			@DefaultValue("0") @QueryParam("skip") int skip,
			@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
			@DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
			@DefaultValue("") @QueryParam(END_QUERY_PARAM) String end)
	{	
		s_Logger.debug(String.format("Get %sbuckets for job %s. skip = %d, take = %d"
				+ " start = '%s', end='%s'", 
				expand?"expanded ":"", jobId, skip, take, start, end));
		
		long epochStart = 0;
		if (start.isEmpty() == false)
		{
			epochStart = paramToEpoch(start);	
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
			epochEnd = paramToEpoch(end);	
			if (epochEnd == 0) // could not be parsed
			{
				String msg = String.format(BAD_DATE_FROMAT_MSG, START_QUERY_PARAM, end);
				s_Logger.info(msg);
				throw new RestApiException(msg, ErrorCode.UNPARSEABLE_DATE_ARGUMENT,
						Response.Status.BAD_REQUEST);
			}			
		}		
		
		JobManager manager = jobManager();
		Pagination<Map<String, Object>> buckets;
		
		if (epochStart > 0 || epochEnd > 0)
		{
			buckets = manager.buckets(jobId, expand, skip, take, epochStart, epochEnd);
		}
		else
		{
			buckets = manager.buckets(jobId, expand, skip, take);
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
				buckets.getDocuments().size(), jobId));
		
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
	public SingleDocument<Map<String, Object>> bucket(@PathParam("jobId") String jobId,
			@PathParam("bucketId") String bucketId,
			@DefaultValue("false") @QueryParam("expand") boolean expand)
	{
		s_Logger.debug(String.format("Get %sbucket %s for job %s", 
				expand?"expanded ":"", bucketId, jobId));
		
		JobManager manager = jobManager();
		SingleDocument<Map<String, Object>> bucket = manager.bucket(jobId, bucketId, expand);
		
		if (bucket.isExists())
		{
			s_Logger.debug(String.format("Returning bucket %s for job %s", 
					bucketId, jobId));
		}
		else
		{
			s_Logger.debug(String.format("Cannot find bucket %s for job %s", 
					bucketId, jobId));
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
					
		return bucket;
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
	public Pagination<Map<String, Object>> bucketRecords(
			@PathParam("jobId") String jobId,
			@PathParam("bucketId") String bucketId,
			@DefaultValue("0") @QueryParam("skip") int skip,
			@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take)
	{
		s_Logger.debug(String.format("Get records for job %s, bucket %s", 
				jobId, bucketId));
		
		JobManager manager = jobManager();
		Pagination<Map<String, Object>> records = manager.records(
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
	
	@Path("/{jobId}/{bucketId}/{detectorId}/records")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Pagination<Map<String, Object>> bucketDetectorRecords(@PathParam("jobId") String jobId,
			@PathParam("bucketId") String bucketId,
			@PathParam("detectorId") String detectorId,
			@DefaultValue("0") @QueryParam("skip") int skip,
			@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take)
	{
		s_Logger.debug(String.format("Get records for job %s detector %s" +
				" and bucket %s", jobId, detectorId, bucketId));
		
		JobManager manager = jobManager();
		Pagination<Map<String, Object>> records = manager.records(jobId, 
				bucketId, detectorId, skip, take);
		
		// paging
    	if (records.isAllResults() == false)
    	{
    		String path = new StringBuilder()
    							.append("/results/")
								.append(jobId)
								.append('/')
								.append(bucketId)
								.append("/")
								.append(detectorId)
								.append("/records")
								.toString();
    		
    		setPagingUrls(path, records);
    	}		
		
		s_Logger.debug(String.format("Returning records for job %s, bucket %s"
				+ " and detector %s", jobId, bucketId, detectorId));
					
		return records;
	}
	
	
	/**
	 * First tries to parse the date first as a Long and convert that 
	 * to an epoch time, if that fails it tries to parse the string 
	 * in ISO 8601 format {@value #ISO_8601_DATE_FORMAT} then in ISO 8601 
	 * format with milliseconds {@value #ISO_8601_DATE_FORMAT_WITH_MS}
	 * 
	 * If the date string cannot be parsed 0 is returned. 
	 * 
	 * @param date
	 * @return The epoch time in seconds or 0 if the date cannot be parsed.
	 */
	private long paramToEpoch(String date)
	{
		try 
		{
			long epoch = Long.parseLong(date);
			Date d = new Date(epoch * 1000);
			// TODO validate date
			return d.getTime() / 1000;
		}
		catch (NumberFormatException nfe)
		{
			// not a number
		}
		
		// try parsing as a date string
		try 
		{
			Date d = s_DateFormat.parse(date);
			// TODO validate date
			return d.getTime() / 1000;
		}
		catch (ParseException pe)
		{
			// not a date
		}
		
		// try parsing as a date string with milliseconds
		try 
		{
			Date d = s_DateFormatWithMs.parse(date);
			// TODO validate date
			return d.getTime() / 1000;
		}
		catch (ParseException pe)
		{
			// not a date
		}

		// Could not do the conversion
		return 0;
	}
}
