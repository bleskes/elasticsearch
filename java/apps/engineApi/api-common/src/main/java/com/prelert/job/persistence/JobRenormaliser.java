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

package com.prelert.job.persistence;

import java.util.List;

import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;


/**
 * Interface for classes that update {@linkplain Bucket Buckets}
 * for a particular job with new normalised anomaly scores and
 * unusual scores
 */
public interface JobRenormaliser
{
    /**
     * Update the bucket with the changes that may result
     * due to renormalisation.
     *
     * @param jobId Job to update
     * @param bucke the bucket to update
     */
    void updateBucket(String jobId, Bucket bucket);


    /**
     * Update the anomaly records for a particular bucket and job.
     * The anomaly records are updated with the values in the
     * <code>records</code> list.
     *
     * @param jobId Job to update
     * @param bucketId Id of the bucket to update
     * @param records The new record values
     */
    void updateRecords(String jobId, String bucketId, List<AnomalyRecord> records);

    /**
     * Update the influencer for a particular job
     *
     * @param jobId
     * @param influencer
     */
    void updateInfluencer(String jobId, Influencer influencer);
}

