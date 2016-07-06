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

package com.prelert.master.rs.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.master.streaming.HttpDataStreamer;
import com.prelert.master.superjob.DataForker;
import com.prelert.master.superjob.JobRouter;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.settings.PrelertSettings;

@Path("/data")
public class Data
{
    private static final Logger LOGGER = Logger.getLogger(Data.class);


    /** Parameter to specify start time of buckets to be reset */
    static final String RESET_START_PARAM = "resetStart";

    /** Parameter to specify end time of buckets to be reset */
    static final String RESET_END_PARAM = "resetEnd";

    /** Ignore downtime if the job is restarting */
    static final String IGNORE_DOWNTIME_PARAM = "ignoreDowntime";


    private String [] m_EngineUrls;
    private String m_PartitionField;
    private int m_JobsPerNode;

    public Data()
    {
        readSettings();
    }

    /**
     * Data upload endpoint.
     *
     * @param headers
     * @param jobId
     * @param input
     * @param resetStart Optional parameter to specify start of range for bucket resetting
     * @param resetEnd Optional parameter to specify end of range for bucket resetting
     * @return
     * @throws IOException
     */
    @POST
    @Path("/{jobId}")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_OCTET_STREAM})
    @Produces(MediaType.APPLICATION_JSON)
    public Response streamData(@Context HttpHeaders headers,
            @PathParam("jobId") String jobId, InputStream input,
            @DefaultValue("") @QueryParam(RESET_START_PARAM) String resetStart,
            @DefaultValue("") @QueryParam(RESET_END_PARAM) String resetEnd,
            @DefaultValue("false") @QueryParam(IGNORE_DOWNTIME_PARAM) boolean ignoreDowntime)
    throws IOException
    {
        LOGGER.debug("Post data to job " + jobId);

        List<HttpDataStreamer> streamers = createStreamers();
        List<OutputStream> outputStreams = new ArrayList<>();
        int subJobIndex = 1;
        try
        {
            for (HttpDataStreamer streamer : streamers)
            {
                for (int i=0; i<m_JobsPerNode; i++)
                {
                    String subJobId = subJobId(jobId, subJobIndex++);
                    outputStreams.add(streamer.openStream(subJobId));
                }
            }

            JobRouter router = new JobRouter(outputStreams);
            DataForker forker = new DataForker(m_PartitionField, router);
            forker.forkData(input);
        }
        finally
        {
            for (OutputStream out : outputStreams)
            {
                try
                {
                    out.close();
                }
                catch (IOException e)
                {
                    LOGGER.warn("Error closing forked output stream", e);
                }
            }
            streamers.forEach(s -> s.close());
        }

        return Response.ok().build();

    }

    @Path("/{jobId}/close")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response commitUpload(@PathParam("jobId") String jobId)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Post to close data upload for job " + jobId);

        List<HttpDataStreamer> streamers = createStreamers();
        int subJobIndex = 1;
        try
        {
            for (HttpDataStreamer streamer : streamers)
            {
                for (int i=0; i<m_JobsPerNode; i++)
                {
                    String subJobId = subJobId(jobId, subJobIndex++);
                    try
                    {
                        streamer.closeJob(subJobId);
                    }
                    catch (IOException e)
                    {
                        LOGGER.warn("Error closing job", e);
                    }
                }
            }
        }
        finally
        {
            streamers.forEach(s -> s.close());
        }


        LOGGER.debug("Process finished successfully, Job Id = '" + jobId + "'");
        return Response.ok().entity(new Acknowledgement()).build();
    }


    private List<HttpDataStreamer> createStreamers()
    {
        List<HttpDataStreamer> streamers = new ArrayList<>();
        for (String baseUrl : m_EngineUrls)
        {
            streamers.add(new HttpDataStreamer(baseUrl));
        }
        return streamers;
    }

    private void readSettings()
    {
        String commaSeparatedUrls = PrelertSettings.getSettingOrDefault("engine.urls", "");
        if (commaSeparatedUrls.isEmpty())
        {
            return;
        }

        m_EngineUrls = commaSeparatedUrls.split(",");

        m_JobsPerNode = PrelertSettings.getSettingOrDefault("jobs.per.node", 2);
        m_PartitionField = PrelertSettings.getSettingOrDefault("partition.field", "");
    }

    private String subJobId(String jobId, int index)
    {
        return String.format("%s-%03d", jobId, index);
    }
}
