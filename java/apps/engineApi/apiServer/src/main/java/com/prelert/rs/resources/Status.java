/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.prelert.rs.data.EngineStatus;



@Path("/status")
public class Status extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Status.class);

    /**
     * The name of the endpoint
     */
    public static final String ENDPOINT = "status";


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EngineStatus status()
    {
        LOGGER.debug("Get Engine Status");

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        EngineStatus status = new EngineStatus();
        status.setActiveJobs(new ArrayList<String>(jobManager().getActiveJobIds()));
        status.setStartedScheduledJobs(jobManager().getStartedScheduledJobs());

        status.setAverageCpuLoad(osBean.getSystemLoadAverage());
        status.setHeapMemoryUsage(
                        ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());

        return status;
    }
}
