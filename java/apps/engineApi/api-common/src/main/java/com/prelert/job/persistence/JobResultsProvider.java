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

import java.io.Closeable;
import java.util.Optional;

import com.prelert.job.UnknownJobException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influencer;

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
     * @return QueryPage of buckets
     * @throws UnknownJobException If the job id is no recognised
     */
    public QueryPage<Bucket> buckets(String jobId,
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
     * @return QueryPage of Buckets
     * @throws UnknownJobException If the job id is no recognised
     */
    public QueryPage<Bucket> buckets(String jobId,
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
     * @return Optional Bucket
     * @throws UnknownJobException If the job id is no recognised
     */
    public Optional<Bucket> bucket(String jobId,
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
     * @return QueryPage of AnomalyRecords
     * @throws UnknownJobException If the job id is no recognised
     */
    public QueryPage<AnomalyRecord> bucketRecords(String jobId,
            String bucketId, int skip, int take, boolean includeInterim, String sortField,
            boolean sortDescending)
    throws UnknownJobException;

    /**
     * Get a page of {@linkplain CategoryDefinition}s for the given <code>jobId</code>.
     *
     * @param jobId
     * @param skip Skip the first N categories. This parameter is for paging
     * @param take Take only this number of categories
     * @return QueryPage of CategoryDefinition
     * @throws UnknownJobException If the job id is no recognised
     */
    public QueryPage<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take)
    throws UnknownJobException;

    /**
     * Get the specific CategoryDefinition for the given job and category id.
     *
     * @param jobId
     * @param categoryId Unique id
     * @return Optional CategoryDefinition
     * @throws UnknownJobException
     */
    public Optional<CategoryDefinition> categoryDefinition(String jobId, String categoryId)
    throws UnknownJobException;

    /**
     * Get the anomaly records for all buckets.
     * The returned records will have the {@linkplain AnomalyRecord#getParent()}
     *  member set to the parent bucket's id.
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
     * @return QueryPage of AnomalyRecords
     * @throws UnknownJobException If the job id is no recognised
     */
    public QueryPage<AnomalyRecord> records(String jobId,
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
     * @return QueryPage of AnomalyRecords
     * throws UnknownJobException If the job id is no recognised
     */
    public QueryPage<AnomalyRecord> records(String jobId,
            int skip, int take, long startEpochMs, long endEpochMs,
            boolean includeInterim, String sortField, boolean sortDescending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException;


    /**
     * Return a page of influencers for the given job.
     *
     * @param jobId
     * @param skip Skip the first N records. This parameter is for paging
     * if not required set to 0.
     * @param take Take only this number of records
     * @return QueryPage of Influencer
     */
    public QueryPage<Influencer> influencers(String jobId, int skip, int take);

    /**
     * Return a page of influencers for the given job and within the given date range
     *
     * @param jobId The job ID for which influencers are requested
     * @param skip Skip the first N Buckets. This parameter is for paging
     * if not required set to 0.
     * @param take Maximum number of influencers to insert in the page
     * @param startEpochMs The start influencer timestamp. An influencer with this timestamp will be
     * included in the results. If 0 all buckets up to <code>endEpochMs</code>
     * are returned
     * @param endEpochMs The end bucket timestamp buckets up to but NOT including this
     * timestamp are returned. If 0 all buckets from <code>startEpochMs</code>
     * are returned
     * @return QueryPage of Influencer
     */
    public QueryPage<Influencer> influencers(String jobId, int skip, int take, long startEpochMs,
            long endEpochMs);


    /**
     * Get the influencer for the given job for id
     *
     * @param jobId
     * @param influencerId The unique influencer Id
     * @return Optional Influencer
     */
    public Optional<Influencer> influencer(String jobId, String influencerId);
}
