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

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;

import com.prelert.job.alert.Alert;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.alert.AlertTrigger;
import com.prelert.job.results.Bucket;

class AsyncResponseAlertObserver extends AlertObserver
{
    private static final Logger LOGGER = Logger.getLogger(AsyncResponseAlertObserver.class);

    private final AsyncResponse m_Response;
    private final AlertManager m_Manager;
    private final URI m_BaseUri;

    public AsyncResponseAlertObserver(AsyncResponse response, AlertManager manager, String jobId,
            AlertTrigger [] triggers, URI baseUri)
    {
        super(triggers, jobId);

        m_Response = response;
        m_Manager = manager;
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

        Alert alert = createAlert(bucket, trigger);

        UriBuilder uriBuilder = UriBuilder.fromUri(m_BaseUri)
                .path("results")
                .path(getJobId())
                .path("buckets")
                .path(bucket.getId())
                .queryParam("expand", true);
        alert.setUri(uriBuilder.build());

        m_Response.resume(alert);
    }
}
