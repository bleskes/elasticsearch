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
package com.prelert.job.alert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;

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
            if (trigger.isTriggeredBy(bucket))
            {
                alerts.add(trigger);
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

        for (AlertTrigger at : triggeredAlerts(bucket))
        {
            switch (at.getAlertType())
            {
                case INFLUENCER:
                case BUCKETINFLUENCER:
                    alert.setBucket(bucket);
                    break;
                case BUCKET:
                    setTriggeredRecordsOnBucketAlert(at, bucket, alert);
                    break;
            }
        }

        return alert;
    }

    private static void setTriggeredRecordsOnBucketAlert(AlertTrigger trigger, Bucket bucket,
            Alert alert)
    {
        List<AnomalyRecord> records = extractRecordsAboveThreshold(
                                    trigger.getNormalisedThreshold(), bucket);

        if (trigger.triggersAnomalyThreshold(bucket.getAnomalyScore()))
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

    private static List<AnomalyRecord> extractRecordsAboveThreshold(Double normalisedThreshold,
            Bucket bucket)
    {
        List<AnomalyRecord> records = new ArrayList<>();

        if (normalisedThreshold == null)
        {
            return records;
        }

        for (AnomalyRecord r : bucket.getRecords())
        {
            if (r.getNormalizedProbability() >= normalisedThreshold)
            {
                r.setTimestamp(bucket.getTimestamp());
                records.add(r);
            }
        }

        return records;
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
