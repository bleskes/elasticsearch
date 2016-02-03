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

package com.prelert.job.alert.manager;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.log4j.Logger;

import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.Alert;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.alert.AlertTrigger;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.exceptions.ClosedJobException;


/**
 * Alerts are channelled through this object interested
 * parties register for alert notification using the
 * observer pattern.
 *
 * Handles Asynchronous HTTP requests
 *
 * Alert Ids are a sequence shared between all jobs starting at 1
 * each alert has a unique id. The function {@linkplain #alertsAfterCursor(String)}
 * returns a list of alerts in the sequence after the alert Id (cursor) parameter
 */
public class AlertManager implements TimeoutHandler, Feature
{
    private static final Logger LOGGER = Logger.getLogger(AlertManager.class);

    private Map<AsyncResponse, AlertObserver> m_AsyncRepsonses;

    private JobProvider m_JobProvider;
    private JobManager m_JobManager;

    public AlertManager(JobProvider jobProvider, JobManager jobManager)
    {
        m_JobProvider = jobProvider;
        m_JobManager = jobManager;
        m_AsyncRepsonses = new HashMap<>();
    }

    /**
     * Required by the Feature interface.
     */
    @Override
    public boolean configure(FeatureContext context)
    {
        return true;
    }

    /**
     * Non blocking asynchronous request for alerts by job
     *
     * @param response
     * @param jobId
     * @param timeoutSecs
     * @param anomalyScoreThreshold
     * @param normalizedProbabilityThreshold
     * @throws UnknownJobException
     */
    public void registerRequest(AsyncResponse response, String jobId, URI baseUri,
            long timeoutSecs, AlertTrigger [] alertTriggers)
    throws UnknownJobException
    {
        m_JobProvider.checkJobExists(jobId);

        response.setTimeout(timeoutSecs, TimeUnit.SECONDS);
        response.setTimeoutHandler(this);

        AsyncResponseAlertObserver listener = new AsyncResponseAlertObserver(response, this, jobId, alertTriggers, baseUri);
        registerObserver(response, listener);

        try
        {
            m_JobManager.addAlertObserver(jobId, listener);
        }
        catch (ClosedJobException e)
        {
            LOGGER.warn("Alerting on closed job " + jobId);
            deregisterResponse(response);
            response.resume(e);
        }
    }

    /**
     * AysncResponse timeout handler
     */
    @Override
    public void handleTimeout(AsyncResponse response)
    {
        Alert alert = new Alert();
        alert.setTimeout(true);

        AlertObserver observer = getObserver(response);
        if (observer != null)
        {
            alert.setJobId(observer.getJobId());
            deregisterResponse(response);
        }

        response.resume(alert);
    }

    private AlertObserver getObserver(AsyncResponse response)
    {
        synchronized(m_AsyncRepsonses)
        {
            return m_AsyncRepsonses.get(response);
        }
    }

    private void registerObserver(AsyncResponse response, AlertObserver observer)
    {
        synchronized(m_AsyncRepsonses)
        {
            m_AsyncRepsonses.put(response, observer);
        }
    }

    void deregisterResponse(AsyncResponse response)
    {
        synchronized(m_AsyncRepsonses)
        {
            m_AsyncRepsonses.remove(response);
        }
    }
}
