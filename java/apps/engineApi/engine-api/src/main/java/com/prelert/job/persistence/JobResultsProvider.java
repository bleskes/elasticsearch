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

import java.io.Closeable;

import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public interface JobResultsProvider extends Closeable
{
    /**
     * Get a page of result buckets for the job id
     *
     * @param jobId
     * @param expand Include anomaly records
     * @param includeInterim Include interim results
     * @param skip Skip the first N Buckets. This parameter is for paging
     * if not required set to 0.
     * @param take Take only this number of Buckets
     * @param anomalyScoreThreshold Return only buckets with an anomalyScore >=
     * this value
     * @param normalizedProbabilityThreshold Return only buckets with a maxNormalizedProbability >=
     * this value
     *
     * @return
     * @throws UnknownJobException If the job id is no recognised
     */
    public Pagination<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException;


    /**
     * Get the result buckets for the job id starting with bucket id =
     * <code>startBucket</code> up to <code>endBucket</code>. One of either
     * <code>startBucket</code> or <code>endBucket</code> should be non-zero else
     * it is more efficient to use {@linkplain #buckets(String, boolean, int, int)}
     *
     * @param jobId
     * @param expand Include anomaly records
     * @param includeInterim Include interim results
     * @param skip Skip the first N Buckets. This parameter is for paging
     * if not required set to 0.
     * @param take Take only this number of Buckets
     * @param startEpochMs The start bucket time. A bucket with this timestamp will be
     * included in the results. If 0 all buckets up to <code>endEpochMs</code>
     * are returned
     * @param endEpochMs The end bucket timestamp buckets up to but NOT including this
     * timestamp are returned. If 0 all buckets from <code>startEpochMs</code>
     * are returned
     * @param anomalyScoreThreshold Return only buckets with an anomalyScore >=
     * this value
     * @param normalizedProbabilityThreshold Return only buckets with a maxNormalizedProbability >=
     * this value
     *
     * @return
     * @throws UnknownJobException If the job id is no recognised
     */
    public Pagination<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take,
            long startEpochMs, long endEpochMs,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException;


    /**
     * Get the bucket by Id from the job.
     *
     * @param jobId
     * @param bucketId
     * @param expand Include anomaly records
     * @param includeInterim Include interim results
     * @return
     * @throws UnknownJobException If the job id is no recognised
     */
    public SingleDocument<Bucket> bucket(String jobId,
            String bucketId, boolean expand, boolean includeInterim)
    throws UnknownJobException;


    /**
     * Get the anomaly records for the bucket.
     * The returned records will have the <code>parent</code> member
     * set to the parent bucket's id.
     *
     * @param jobId
     * @param bucketId
     * @param skip Skip the first N Jobs. This parameter is for paging
     * results if not required set to 0.
     * @param take Take only this number of Jobs
     * @param includeInterim Include interim results
     * @param sortField The field to sort results by if <code>null</code> no
     * sort is applied
     * @param sortDescending Sort in descending order
     * @return
     * @throws UnknownJobException If the job id is no recognised
     */
    public Pagination<AnomalyRecord> bucketRecords(String jobId,
            String bucketId, int skip, int take, boolean includeInterim, String sortField,
            boolean sortDescending)
    throws UnknownJobException;

    public Pagination<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take)
    throws UnknownJobException;

    public SingleDocument<CategoryDefinition> categoryDefinition(String jobId, String categoryId)
    throws UnknownJobException;

    /**
     * Get the anomaly records for all buckets.
     * The returned records will have the <code>parent</code> member
     * set to the parent bucket's id.
     *
     * @param jobId
     * @param skip Skip the first N records. This parameter is for paging
     * if not required set to 0.
     * @param take Take only this number of records
     * @param includeInterim Include interim results
     * @param sortField The field to sort results by if <code>null</code> no
     * sort is applied
     * @param sortDescending Sort in descending order
     * @param anomalyScoreThreshold Return only buckets with an anomalyScore >=
     * this value
     * @param normalizedProbabilityThreshold Return only buckets with a maxNormalizedProbability >=
     * this value
     *
     * @return
     * @throws UnknownJobException If the job id is no recognised
     */
    public Pagination<AnomalyRecord> records(String jobId,
             int skip, int take, boolean includeInterim, String sortField, boolean sortDescending,
             double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException;

    /**
     * Get the anomaly records for all buckets in the given
     * date (epoch time) range. The returned records will have the
     * <code>parent</code> member set to the parent bucket's id.
     *
     * @param jobId
     * @param skip Skip the first N records. This parameter is for paging
     * if not required set to 0.
     * @param take Take only this number of records
     * @param startEpochMs The start bucket time. A bucket with this timestamp will be
     * included in the results. If 0 all buckets up to <code>endEpochMs</code>
     * are returned
     * @param endEpochMs The end bucket timestamp buckets up to but NOT including this
     * timestamp are returned. If 0 all buckets from <code>startEpochMs</code>
     * are returned
     * @param includeInterim Include interim results
     * @param sortField The field to sort results by if <code>null</code> no
     * sort is applied
     * @param sortDescending Sort in descending order
     * @param anomalyScoreThreshold Return only buckets with an anomalyScore >=
     * this value
     * @param normalizedProbabilityThreshold Return only buckets with a maxNormalizedProbability >=
     * this value
     *
     * @return
     * throws UnknownJobException If the job id is no recognised
     */
    public Pagination<AnomalyRecord> records(String jobId,
            int skip, int take, long startEpochMs, long endEpochMs,
            boolean includeInterim, String sortField, boolean sortDescending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException;
}
