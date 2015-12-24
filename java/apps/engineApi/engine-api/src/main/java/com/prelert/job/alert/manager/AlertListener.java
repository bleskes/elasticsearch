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

package com.prelert.job.alert.manager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;

import com.prelert.job.alert.Alert;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.alert.AlertTrigger;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Detector;

class AlertListener extends AlertObserver
{
    private static final Logger LOGGER = Logger.getLogger(AlertListener.class);

    private final AsyncResponse m_Response;
    private final AlertManager m_Manager;
    private final String m_JobId;
    private final URI m_BaseUri;

    public AlertListener(AsyncResponse response, AlertManager manager, String jobId,
            AlertTrigger [] triggers, URI baseUri)
    {
        super(triggers);

        m_Response = response;
        m_Manager = manager;
        m_JobId = jobId;
        m_BaseUri = baseUri;
    }

    @Override
    public void fire(Bucket bucket, AlertTrigger trigger)
    {
        LOGGER.info(String.format("Alert fired in bucket %s, probablilty = %f, anomaly score = %f",
                                    bucket.getTimestamp(),
                                    bucket.getMaxNormalizedProbability(),
                                    bucket.getAnomalyScore()));

        m_Manager.deregisterResponse(m_Response);
        m_Response.resume(createAlert(bucket, trigger));
    }

    private Alert createAlert(Bucket bucket, AlertTrigger trigger)
    {
        Alert alert = new Alert();
        alert.setAlertType(trigger.getAlertType());
        alert.setInterim(bucket.isInterim());
        alert.setTimestamp(new Date());
        alert.setJobId(getJobId());
        alert.setAnomalyScore(bucket.getAnomalyScore());
        alert.setMaxNormalizedProbability(bucket.getMaxNormalizedProbability());

        UriBuilder uriBuilder = UriBuilder.fromUri(getBaseUri())
                                .path("results")
                                .path(getJobId())
                                .path("buckets")
                                .path(bucket.getId())
                                .queryParam("expand", true);
        alert.setUri(uriBuilder.build());

        for (AlertTrigger at : this.triggeredAlerts(bucket))
        {
            switch (at.getAlertType())
            {
            case INFLUENCER:
            case BUCKETINFLUENCER:
                alert.setBucket(bucket);
                break;
            case BUCKET:
            {
                List<AnomalyRecord> records = new ArrayList<>();
                if (at.getNormalisedThreshold() != null)
                {
                    for (Detector detector : bucket.getDetectors())
                    {
                        for (AnomalyRecord r : detector.getRecords())
                        {
                            if (r.getNormalizedProbability() >= at.getNormalisedThreshold())
                            {
                                records.add(r);
                            }
                        }
                    }
                }

                boolean isAnomalyScoreAlert = AlertObserver.isGreaterOrEqual(
                                                        bucket.getAnomalyScore(),
                                                        at.getAnomalyThreshold());
                if (isAnomalyScoreAlert)
                {
                    bucket.setRecords(records);
                    bucket.setRecordCount(records.size());
                    alert.setBucket(bucket);
                }
                else
                {
                    alert.setRecords(records);
                }
            }
            }
        }

        return alert;
    }

    public AsyncResponse getResponse()
    {
        return m_Response;
    }

    public String getJobId()
    {
        return m_JobId;
    }

    public URI getBaseUri()
    {
        return m_BaseUri;
    }
}
