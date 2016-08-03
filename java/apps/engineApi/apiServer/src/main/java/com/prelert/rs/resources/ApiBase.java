/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
import com.prelert.server.info.ServerInfo;
import com.prelert.server.info.ServerInfoFactory;


/**
 * API base resource
 */
@Path("")
public class ApiBase extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(ApiBase.class);

    private static final String VERSION_HTML =
            "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head><title>Prelert Engine</title></head>\n"
            + "<body>\n"
            + "<h1>Prelert Engine API %s</h1>\n"
            + "<h2>Analytics Version:</h2>\n"
            + "<p>%s</p>\n"
            + "<div>%s</div>"
            + "</body>\n"
            + "</html>";


    private static final String SERVER_INFO_TABLE =
            "<table>"
            + "<tr><td>Hostname</td><td>%s</td></tr>"
            + "<tr><td>OS Name</td><td>%s</td></tr>"
            + "<tr><td>OS Version</td><td>%s</td></tr>"
            + "<tr><td>Total Memory Size MB</td><td>%d</td></tr>"
            + "<tr><td>Total Disk MB</td><td>%d</td></tr>"
            + "<tr><td>Available Disk MB</td><td>%d</td></tr>"
            + "</table>";



    @GET
    @Produces(MediaType.TEXT_HTML)
    public String version()
    {
        LOGGER.debug("Get API Base document");

        JobManager manager = jobManager();
        String apiVersion = manager.apiVersion();
        String analyticsVersion = manager.getAnalyticsVersion();
        analyticsVersion = analyticsVersion.replace("\n", "<br>");

        ServerInfoFactory serverInfoFactory = serverInfo();

        ServerInfo serverInfo = serverInfoFactory.serverInfo();
        String serverTable = String.format(SERVER_INFO_TABLE,
                                            serverInfo.getHostname(),
                                            serverInfo.getOsName(),
                                            serverInfo.getOsVersion(),
                                            serverInfo.getTotalMemoryMb(),
                                            serverInfo.getTotalDiskMb(),
                                            serverInfo.getAvailableDiskMb());

        return String.format(VERSION_HTML, apiVersion, analyticsVersion, serverTable);
    }
}
