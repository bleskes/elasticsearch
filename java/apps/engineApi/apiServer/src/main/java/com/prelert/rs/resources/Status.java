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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.prelert.job.ModelSizeStats;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.rs.data.EngineStatus;
import com.prelert.settings.PrelertSettings;



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

        EngineStatus status = new EngineStatus();

        status.setAverageCpuLoad(
                ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        status.setJvmHeapMemoryUsage(
                        ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());

        status.setStartedScheduledJobs(jobManager().getStartedScheduledJobs());

        Map<String, EngineStatus.JobStats> memoryStats = new HashMap<>();
        for (String jobId : jobManager().getRunningJobIds())
        {
            Optional<ModelSizeStats> stats = jobReader().modelSizeStats(jobId);
            if (stats.isPresent())
            {
                memoryStats.put(jobId, new EngineStatus.JobStats(
                                                    stats.get().getModelBytes(),
                                                    stats.get().getMemoryStatus(),
                                                    jobManager().jobUptime(jobId)
                                                    ));
            }
            else
            {
                memoryStats.put(jobId, new EngineStatus.JobStats());
            }
        }

        status.setRunningJobs(memoryStats);
        status.setDataStoreConnection(datastoreConnection());
        status.setEngineHosts(engineHosts().engineApiHosts());
        status.setHostByJob(engineHosts().hostByJob());

        return status;
    }

    private Map<String, String> datastoreConnection()
    {
        Map<String, String> params = new HashMap<>();
        params.put(ProcessCtrl.ES_HOST_PROP,
                PrelertSettings.getSettingOrDefault(ProcessCtrl.ES_HOST_PROP,
                                            ProcessCtrl.DEFAULT_ES_HOST));
        params.put(PrelertWebApp.ES_CLUSTER_NAME_PROP,
                PrelertSettings.getSettingOrDefault(PrelertWebApp.ES_CLUSTER_NAME_PROP,
                                            PrelertWebApp.DEFAULT_CLUSTER_NAME));
        params.put(PrelertWebApp.ES_TRANSPORT_PORT_RANGE,
                PrelertSettings.getSettingOrDefault(PrelertWebApp.ES_TRANSPORT_PORT_RANGE,
                                            PrelertWebApp.DEFAULT_ES_TRANSPORT_PORT_RANGE));

        return params;
    }
}
