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

import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;

public interface JobResultsDeleter
{
    /**
     * Delete a {@code Bucket} and its records
     * @param bucket the bucket to delete
     */
    void deleteBucket(Bucket bucket);

    /**
     * Delete the records of a {@code Bucket}
     * @param bucket the bucket whose records to delete
     */
    void deleteRecords(Bucket bucket);

    /**
     * Delete an {@code Influencer}
     * @param influencer the influencer to delete
     */
    void deleteInfluencer(Influencer influencer);

    /**
     * Commit the deletions and give the chance to implementors
     * to perform clean-up
     */
    void commit();
}
