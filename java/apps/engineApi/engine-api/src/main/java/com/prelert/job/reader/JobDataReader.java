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
package com.prelert.job.reader;

import java.util.Optional;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import com.prelert.job.JobDetails;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influencer;

/**
 * Class for reading Job details, results, model snapshots, etc
 * from the datastore
 */
public class JobDataReader implements Feature
{
    JobProvider m_JobProvider;

    public JobDataReader(JobProvider jobProvider)
    {
        m_JobProvider = jobProvider;
    }

    /**
     * Get the details of the specific job wrapped in a <code>Optional</code>
     *
     * @param jobId
     * @return An {@code Optional} containing the {@code JobDetails} if a job with the given
     * {@code jobId} exists, or an empty {@code Optional} otherwise
     */
    public Optional<JobDetails> getJob(String jobId)
    {
        return m_JobProvider.getJobDetails(jobId);
    }

    /**
     * Get details of all Jobs.
     *
     * @param skip Skip the first N Jobs. This parameter is for paging
     * results if not required set to 0.
     * @param take Take only this number of Jobs
     * @return A pagination object with hitCount set to the total number
     * of jobs not the only the number returned here as determined by the
     * <code>take</code>
     * parameter.
     */
    public QueryPage<JobDetails> getJobs(int skip, int take)
    {
        return m_JobProvider.getJobs(skip, take);
    }

    /**
     * Get a single result bucket
     *
     * @param jobId
     * @param timestampMillis
     * @param expand Include anomaly records. If false the bucket's records
     *  are set to <code>null</code> so they aren't serialised
     * @param includeInterim Include interim results
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public Optional<Bucket> bucket(String jobId, long timestampMillis, boolean expand,
                                    boolean includeInterim)
            throws NativeProcessRunException, UnknownJobException
    {
        Optional<Bucket> result = m_JobProvider.bucket(jobId, timestampMillis, expand, includeInterim);

        if (result.isPresent() && !expand)
        {
            result.get().setRecords(null);
        }

        return result;
    }

    /**
     * Get result buckets
     *
     * @param jobId
     * @param expand Include anomaly records. If false the bucket's records
     *  are set to <code>null</code> so they aren't serialised
     * @param includeInterim Include interim results
     * @param skip
     * @param take
     * @param anomalyScoreThreshold
     * @param normalizedProbabilityThreshold
     * @return
     * @throws UnknownJobException
     */
    public QueryPage<Bucket> buckets(String jobId, boolean expand, boolean includeInterim,
                              int skip, int take,
                              double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        QueryPage<Bucket> buckets = m_JobProvider.buckets(jobId, expand,
                includeInterim, skip, take, anomalyScoreThreshold, normalizedProbabilityThreshold);

        if (!expand)
        {
            for (Bucket bucket : buckets.queryResults())
            {
                bucket.setRecords(null);
            }
        }

        return buckets;
    }

    /**
     * Get result buckets between 2 dates
     * @param jobId
     * @param expand Include anomaly records. If false the bucket's records
     *  are set to <code>null</code> so they aren't serialised
     * @param includeInterim Include interim results
     * @param skip
     * @param take
     * @param startEpochMs Return buckets starting at this time
     * @param endBucketMs Include buckets up to this time
     * @param anomalyScoreThreshold
     * @param normalizedProbabilityThreshold
     * @return
     * @throws UnknownJobException
     */
    public QueryPage<Bucket> buckets(String jobId, boolean expand, boolean includeInterim,
                            int skip, int take, long startEpochMs, long endBucketMs,
                            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        QueryPage<Bucket> buckets =  m_JobProvider.buckets(jobId, expand,
                includeInterim, skip, take, startEpochMs, endBucketMs,
                anomalyScoreThreshold, normalizedProbabilityThreshold);

        if (!expand)
        {
            for (Bucket bucket : buckets.queryResults())
            {
                bucket.setRecords(null);
            }
        }

        return buckets;
    }


    /**
     * Get a page of anomaly records from all buckets.
     *
     * @param jobId
     * @param skip Skip the first N records. This parameter is for paging
     * results if not required set to 0.
     * @param take Take only this number of records
     * @param includeInterim Include interim results
     * @param sortField The field to sort by
     * @param sortDescending
     * @param anomalyScoreThreshold Return only buckets with an anomalyScore >=
     * this value
     * @param normalizedProbabilityThreshold Return only buckets with a maxNormalizedProbability >=
     * this value
     *
     * @return
     * @throws UnknownJobException
     */
    public QueryPage<AnomalyRecord> records(String jobId, int skip, int take,
                    boolean includeInterim, String sortField, boolean sortDescending,
                    double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        return m_JobProvider.records(jobId, skip, take, includeInterim, sortField,
                                            sortDescending, anomalyScoreThreshold,
                                            normalizedProbabilityThreshold);
    }


    /**
     * Get a page of anomaly records from the buckets between
     * epochStart and epochEnd.
     *
     * @param jobId
     * @param skip
     * @param take
     * @param epochStartMs
     * @param epochEndMs
     * @param includeInterim Include interim results
     * @param sortField
     * @param sortDescending
     * @param anomalyScoreThreshold Return only buckets with an anomalyScore >=
     * this value
     * @param normalizedProbabilityThreshold Return only buckets with a maxNormalizedProbability >=
     * this value
     *
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public QueryPage<AnomalyRecord> records(String jobId,
            int skip, int take, long epochStartMs, long epochEndMs,
            boolean includeInterim, String sortField, boolean sortDescending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws NativeProcessRunException, UnknownJobException
    {
        return m_JobProvider.records(jobId, skip, take,
                            epochStartMs, epochEndMs, includeInterim, sortField, sortDescending,
                            anomalyScoreThreshold, normalizedProbabilityThreshold);
    }

    /**
     * Get a page of category definition results
     *
     * @param jobId
     * @param skip
     * @param take
     * @return
     * @throws UnknownJobException
     */
    public QueryPage<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take)
            throws UnknownJobException
    {
        return m_JobProvider.categoryDefinitions(jobId, skip, take);
    }

    /**
     * Get a category definition returning an empty optional if the category
     * does not exist
     *
     * @param jobId
     * @param categoryId
     * @return
     * @throws UnknownJobException
     */
    public Optional<CategoryDefinition> categoryDefinition(String jobId, String categoryId)
            throws UnknownJobException
    {
        return m_JobProvider.categoryDefinition(jobId, categoryId);
    }

    /**
     * Get a page of influencers
     *
     * @param jobId
     * @param skip
     * @param take
     * @param epochStartMs
     * @param epochEndMs
     * @param sortField
     * @param sortDescending
     * @param anomalyScoreFilter
     * @param includeInterim
     * @return
     * @throws UnknownJobException
     */
    public QueryPage<Influencer> influencers(String jobId, int skip, int take, long epochStartMs,
            long epochEndMs, String sortField, boolean sortDescending, double anomalyScoreFilter,
            boolean includeInterim)
            throws UnknownJobException
    {
        return m_JobProvider.influencers(jobId, skip, take, epochStartMs, epochEndMs, sortField,
                sortDescending, anomalyScoreFilter, includeInterim);
    }

    /**
     * Get an influencer
     * @param jobId
     * @param influencerId
     * @return
     * @throws UnknownJobException
     */
    public Optional<Influencer> influencer(String jobId, String influencerId)
            throws UnknownJobException
    {
        return m_JobProvider.influencer(jobId, influencerId);
    }

    /**
     * Get a page of model snapshots
     *
     * @param jobId
     * @param skip
     * @param take
     * @param epochStartMs
     * @param epochEndMs
     * @param sortField
     * @param sortDescending
     * @param description
     * @return
     * @throws UnknownJobException
     */
    public QueryPage<ModelSnapshot> modelSnapshots(String jobId, int skip, int take,
            long epochStartMs, long epochEndMs, String sortField, boolean sortDescending, String description)
            throws UnknownJobException
    {
        return m_JobProvider.modelSnapshots(jobId, skip, take, epochStartMs, epochEndMs,
                sortField, sortDescending, null, description);
    }


    public Optional<ModelSizeStats> modelSizeStats(String jobId)
    {
        return m_JobProvider.modelSizeStats(jobId);
    }

    @Override
    public boolean configure(FeatureContext context)
    {
        return false;
    }

}
