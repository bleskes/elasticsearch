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

package com.prelert.rs.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.job.alert.Alert;

/**
 * Build a blocking Long poll alert for the job.
 * Blocks until the alert occurs or the timeout period expires.
 */
public class AlertRequestBuilder extends BaseJobRequestBuilder<Alert>
{
    public static final String TIMEOUT = "timeout";
    public static final String SCORE = "score";
    public static final String PROBABILITY = "probability";
    public static final String ENDPOINT = "/alerts_longpoll/";

    public static final String BUCKET = "bucket";
    public static final String BUCKET_INFLUENCER = "bucketinfluencer";
    public static final String INFLUENCER = "influencer";

    public static final String ALERT_ON = "alert";

    private Map<String, String> m_Params;

    public AlertRequestBuilder(EngineApiClient client, String jobId)
    {
        super(client, jobId);
        m_Params = new HashMap<>();
    }


    /**
     * Set the timeout period for the request
     *
     * @param seconds Timeout the request after this many seconds.
     * @return
     */
    public AlertRequestBuilder timeout(long seconds)
    {
        m_Params.put(TIMEOUT, Long.toString(seconds));
        return this;
    }

    /**
     * Set the anomaly score threshold and alert if a record
     * has an anomalyScore threshold &gt;= <code>threshold</code>
     *
     * @param threshold This must be in the range 0-100
     * @return
     */
    public AlertRequestBuilder score(double threshold)
    {
        m_Params.put(SCORE, Double.toString(threshold));
        return this;
    }

    /**
     * Set the max normalised probability threshold and alert if
     * a bucket's maxNormalizedProbability is &gt;= <code>threshold</code>
     *
     * @param threshold This must be in the range 0-100
     * @return
     */
    public AlertRequestBuilder probability(double threshold)
    {
        m_Params.put(PROBABILITY, Double.toString(threshold));
        return this;
    }


    public AlertRequestBuilder alertOnBuckets()
    {
        addAlertType(BUCKET);
        return this;
    }

    public AlertRequestBuilder alertOnInfluencers()
    {
        addAlertType(INFLUENCER);
        return this;
    }

    public AlertRequestBuilder alertOnBucketInfluencers()
    {
        addAlertType(BUCKET_INFLUENCER);
        return this;
    }

    public Alert get()
    throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append(baseUrl()).append(ENDPOINT).append(jobId());
        appendParams(m_Params, url);

        return createHttpGetRequester().get(url.toString(), new TypeReference<Alert>() {});
    }

    private void addAlertType(String type)
    {
        if (m_Params.containsKey(ALERT_ON))
        {
            type = m_Params.get(ALERT_ON) + "," + type;
        }

        m_Params.put(ALERT_ON, type);
    }

}
