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
import java.util.List;

import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Influencer;

/**
 * The observer class for alerting
 *
 * Abstract class, concrete sub-classes should implement {@linkplain #fire(Bucket)}
 */
public abstract class AlertObserver
{
    private AlertTrigger [] m_Triggers;

    public AlertObserver(AlertTrigger [] triggers)
    {
        m_Triggers = triggers;
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
     * Fire the alert with the bucket the alert came from
     *
     * @param bucket
     */
    public abstract void fire(Bucket bucket);
}
