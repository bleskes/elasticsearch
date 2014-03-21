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

import java.io.UnsupportedEncodingException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilderException;

import org.apache.log4j.Logger;

import com.prelert.job.JobManager;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.Pagination;

/**
 * List all the detectors in this job.
 */
@Path("/detectors")
public class Detectors extends ResourceWithJobManager
{
	static final private Logger s_Logger = Logger.getLogger(Detectors.class);
	
	/**
	 * The name of this endpoint
	 */
	static public final String ENDPOINT = "detectors";
	
	
	@Path("/{jobId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Pagination<Detector> detectors(
			@PathParam("jobId") String jobId,
			@DefaultValue("0") @QueryParam("skip") int skip,
			@DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take) 
	throws IllegalArgumentException, UriBuilderException, UnsupportedEncodingException
	{	
		s_Logger.debug(String.format("Get detectors for job %s", jobId));
		
		JobManager manager = jobManager();
		Pagination<Detector> detectors = manager.detectors(jobId, skip, take);
		
    	
		// paging
    	if (detectors.isAllResults() == false)
    	{
    		String path = new StringBuilder()
								.append(Jobs.ENDPOINT)
								.append(jobId)
								.append(ENDPOINT)
								.toString();
    		
    		setPagingUrls(path, detectors);
    	}		
			
		s_Logger.debug(String.format("Return %d detectors for job %s", 
				detectors.getDocuments().size(), jobId));
		
		return detectors;
	}	
}
