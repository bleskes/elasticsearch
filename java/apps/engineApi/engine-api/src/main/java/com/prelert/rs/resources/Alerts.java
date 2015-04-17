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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.prelert.job.alert.Alert;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.data.Pagination;

/**
 * The alerts endpoint.
 * Query for all alerts, alerts by job id and between 2 dates
 * or subscribe to the long_poll endpoint.
 */
@Path("/alerts")
public class Alerts extends ResourceWithJobManager
{
    private static final Logger LOGGER = Logger.getLogger(Alerts.class);

    /**
     * The name of this endpoint
     */
    public static final String ENDPOINT = "alerts";

    /**
     * The severity query parameter
     */
    public static final String SEVERITY_QUERY_PARAM = "severity";
    /**
     * The anomaly score query parameter
     */
    public static final String ANOMALY_SCORE_QUERY_PARAM = "score";

    private static class AlertsParams
    {
        boolean expand;
        int skip;
        int take;
        String start;
        String end;
        String severity;
        double anomalyScore;

        public AlertsParams(boolean expand, int skip, int take, String start, String end,
                String severity, double anomalyScore)
        {
            this.expand = expand;
            this.skip = skip;
            this.take = take;
            this.start = start;
            this.end = end;
            this.severity = severity;
            this.anomalyScore = anomalyScore;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<Alert> alerts(
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
            @DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
            @DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
            @DefaultValue("") @QueryParam(SEVERITY_QUERY_PARAM) String severity,
            @DefaultValue("0.0") @QueryParam(ANOMALY_SCORE_QUERY_PARAM) double anomalyScore)
    {
        AlertsParams params = new AlertsParams(true, skip, take, start, end, severity, anomalyScore);
        Pagination<Alert> alerts = paginateAlerts(params, ENDPOINT);
        LOGGER.debug(String.format("Return %d alerts", alerts.getDocuments().size()));
        return alerts;
    }

    private Pagination<Alert> paginateAlerts(AlertsParams params, String path)
    {
        LOGGER.debug(String.format("Get %s alerts, skip = %d, take = %d"
                + " start = '%s', end='%s'",
                params.expand?"expanded ":"", params.skip, params.take, params.start, params.end));

        long epochStart = paramToEpochIfValidOrThrow(START_QUERY_PARAM, params.start, LOGGER);
        long epochEnd = paramToEpochIfValidOrThrow(END_QUERY_PARAM, params.end, LOGGER);

        Pagination<Alert> alerts = new Pagination<>();
        //AlertManager manager = alertManager();
        //Pagination<Alert> alerts = manager.alerts(skip, take, epochStart, epochEnd);

        // paging
        if (alerts.isAllResults() == false)
        {
            List<ResourceWithJobManager.KeyValue> queryParams = new ArrayList<>();
            if (epochStart > 0)
            {
                queryParams.add(this.new KeyValue(START_QUERY_PARAM, params.start));
            }
            if (epochEnd > 0)
            {
                queryParams.add(this.new KeyValue(END_QUERY_PARAM, params.end));
            }

            queryParams.add(this.new KeyValue(ANOMALY_SCORE_QUERY_PARAM,
                                Double.toString(params.anomalyScore)));
            queryParams.add(this.new KeyValue(SEVERITY_QUERY_PARAM, params.severity));

            setPagingUrls(path, alerts, queryParams);
        }
        return alerts;
    }

    @GET
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Pagination<Alert> jobAlerts(
            @PathParam("jobId") String jobId,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue(JobManager.DEFAULT_PAGE_SIZE_STR) @QueryParam("take") int take,
            @DefaultValue("") @QueryParam(START_QUERY_PARAM) String start,
            @DefaultValue("") @QueryParam(END_QUERY_PARAM) String end,
            @DefaultValue("") @QueryParam(SEVERITY_QUERY_PARAM) String severity,
            @DefaultValue("0.0") @QueryParam(ANOMALY_SCORE_QUERY_PARAM) double anomalyScore)
    throws UnknownJobException
    {
        AlertsParams params = new AlertsParams(true, skip, take, start, end, severity, anomalyScore);
        String path = new StringBuilder().append(ENDPOINT).append("/").append(jobId).toString();
        Pagination<Alert> alerts = paginateAlerts(params, path);
        LOGGER.debug(String.format("Return %d alerts for job %s",
                alerts.getDocumentCount(), jobId));
        return alerts;
    }
}
