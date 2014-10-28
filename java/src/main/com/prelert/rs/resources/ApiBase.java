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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.prelert.job.manager.JobManager;



/**
 * API base resource  
 *
 */
@Path("")
public class ApiBase extends ResourceWithJobManager
{	
	private final Logger s_Logger = Logger.getLogger(ApiBase.class);
	
	private static final String VERSION_HTML = 
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "<head><title>Prelert Engine</title></head>\n"
			+ "<body>\n"
			+ "<h1>Prelert Engine REST API</h1>\n"
			+ "<h2>Analytics Version:</h2>\n"
			+ "<p>%s</p>\n"
			+ "</body>\n"
			+ "</html>";
		
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String version() 
    {      
    	s_Logger.debug("Get API Base document");
    	
    	JobManager manager = jobManager();
    	String version = manager.getAnalyticsVersion();
    	version = version.replace("\n", "<br/>");

    	return String.format(VERSION_HTML, version);
    }
}
