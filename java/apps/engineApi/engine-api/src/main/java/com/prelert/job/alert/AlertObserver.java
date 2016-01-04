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
package com.prelert.job.alert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Detector;
import com.prelert.job.results.Influencer;

/**
 * The observer class for alerting
 *
 * Abstract class, concrete sub-classes should implement {@linkplain #fire(Bucket)}
 */
public abstract class AlertObserver
{
    private final AlertTrigger [] m_Triggers;
    private final String m_JobId;

    public AlertObserver(AlertTrigger [] triggers, String jobId)
    {
        m_Triggers = triggers;
        m_JobId = jobId;
    }

    /**
     * Return true if the alert should be fired for these values.
     *
     * @param bucket
     * @return
     */
    public boolean evaluate(Bucket bucket)
    {
        return triggeredAlerts(bucket).isEmpty() == false;
    }


    /**
     * The list of AlertTriggers that have been triggered by the
     * bucket
     *
     * @param bucket
     * @return
     */
    public List<AlertTrigger> triggeredAlerts(Bucket bucket)
    {
        List<AlertTrigger> alerts = new ArrayList<>();

        for (AlertTrigger trigger : m_Triggers)
        {
            boolean skipInterimResults = bucket.isInterim() && !trigger.isIncludeInterim();
            if (skipInterimResults)
            {
                continue;
            }

            switch (trigger.getAlertType())
            {
            case BUCKET:
            {
                if (wouldAlertTrigger(bucket.getMaxNormalizedProbability(),
                                      bucket.getAnomalyScore(),
                                      trigger))
                {
                    alerts.add(trigger);
                }
                break;
            }
            case BUCKETINFLUENCER:
            {
                for (BucketInfluencer bi : bucket.getBucketInfluencers())
                {
                    if (isGreaterOrEqual(bi.getAnomalyScore(), trigger.getAnomalyThreshold()))
                    {
                        alerts.add(trigger);
                        break;
                    }
                }
                break;
            }
            case INFLUENCER:
            {
                for (Influencer inf : bucket.getInfluencers())
                {
                    if (isGreaterOrEqual(inf.getAnomalyScore(), trigger.getAnomalyThreshold()))
                    {
                        alerts.add(trigger);
                        break;
                    }
                }
                break;
            }
            }
        }

        return alerts;
    }

    /**
     * Create the alert defined in the <code>trigger</code> based on
     * the bucket result
     *
     * @param bucket
     * @param trigger
     * @return
     */
    protected Alert createAlert(Bucket bucket, AlertTrigger trigger)
    {
        Alert alert = new Alert();
        alert.setAlertType(trigger.getAlertType());
        alert.setInterim(bucket.isInterim());
        alert.setTimestamp(new Date());
        alert.setJobId(getJobId());
        alert.setAnomalyScore(bucket.getAnomalyScore());
        alert.setMaxNormalizedProbability(bucket.getMaxNormalizedProbability());

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

    protected static boolean isGreaterOrEqual(double value, Double threshold)
    {
        return threshold == null ? false : value >= threshold;
    }

    private static boolean wouldAlertTrigger(double normalisedValue, double  anomalyScore,
                                            AlertTrigger trigger)
    {
        return isGreaterOrEqual(normalisedValue, trigger.getNormalisedThreshold())
                    || isGreaterOrEqual(anomalyScore, trigger.getAnomalyThreshold());
    }

    /**
     * The Job the observer is registered for
     * @return
     */
    public String getJobId()
    {
        return m_JobId;
    }


    /**
     * Fire the alert with the bucket the alert came from
     *
     * @param bucket
     * @param tigger The alert trigger that fired the alert
     */
    public abstract void fire(Bucket bucket, AlertTrigger trigger);
}
