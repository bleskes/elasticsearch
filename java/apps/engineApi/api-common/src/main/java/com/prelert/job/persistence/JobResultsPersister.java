/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

import com.prelert.job.ModelSizeStats;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influencer;

/**
 * Interface for classes that persist {@linkplain Bucket Buckets} and
 * {@linkplain Quantiles Quantiles}
 */
public interface JobResultsPersister extends JobRenormaliser
{
    /**
     * Persist the result bucket
     * @param bucket
     */
    void persistBucket(Bucket bucket);

    /**
     * Persist the category definition
     * @param category The category to be persisted
     */
    void persistCategoryDefinition(CategoryDefinition category);

    /**
     * Persist the quantiles
     * @param quantiles
     */
    void persistQuantiles(Quantiles quantiles);

    /**
     * Persist the memory usage data
     * @param modelSizeStats
     */
    void persistModelSizeStats(ModelSizeStats modelSizeStats);

    /**
     * Persist the influencer
     * @param influencer
     */
    void persistInfluencer(Influencer influencer);

    /**
     * Increment the jobs bucket result count by <code>count</code>
     * @param count
     */
    void incrementBucketCount(long count);

    /**
     * Once all the job data has been written this function will be
     * called to commit the data if the implementing persister requries
     * it.
     *
     * @return True if successful
     */
    boolean commitWrites();
}
