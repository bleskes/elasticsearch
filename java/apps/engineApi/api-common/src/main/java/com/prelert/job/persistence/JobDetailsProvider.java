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
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import com.prelert.job.JobDetails;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;

/**
 * General interface for classes that persist Jobs and job data
 */
public interface JobDetailsProvider extends Closeable
{
    /**
     * Store the Prelert info doc (version number etc)
     *
     * @param infoDoc
     * @return
     */
    public boolean savePrelertInfo(String infoDoc);

    /**
     * Ensures a given {@code jobId} corresponds to an existing job
     * @throws UnknownJobException if there is no job with {@code jobId}
     */
    public void checkJobExists(String jobId) throws UnknownJobException;

    /**
     * Return true if the job id is unique else if it is already used
     * by another job false is returned
     *
     * @param jobId
     * @return true or false
     */
    public boolean jobIdIsUnique(String jobId);

    /**
     * Get the details of the specific job or an empty
     * Optional if there is no job with the given id.
     *
     * @param jobId
     * @return The JobDetails
     */
    public Optional<JobDetails> getJobDetails(String jobId);

    /**
     * Get details of all Jobs.
     *
     * @param skip Skip the first N Jobs. This parameter is for paging
     * results if not required set to 0.
     * @param take Take only this number of Jobs
     * @return A QueryPage object with hitCount set to the total number
     * of jobs not the only the number returned here as determined by the
     * <code>take</code> parameter.
     */
    public QueryPage<JobDetails> getJobs(int skip, int take);

    /**
     * Save the details of the new job to the datastore.
     * Throws <code>JobIdAlreadyExistsException</code> if a job with the
     * same Id already exists.
     *
     * @param job
     * @return True
     * @throws JobIdAlreadyExistsException
     */
    public boolean createJob(JobDetails job) throws JobIdAlreadyExistsException;

    /**
     * Update the job document with the values in the <code>updates</code> map.
     * e.g. Map<String, Object> update = new HashMap<>();<br>
     *      update.put(JobDetails.STATUS, JobStatus.CLOSED);
     *
     * @param jobId
     * @return Whether the operation was a success
     * @throws UnknownJobException if there is no job with the id.
     */
    public boolean updateJob(String jobId, Map<String, Object> updates) throws UnknownJobException;

    /**
     * Get the specified field from the jobs document
     *
     * @param jobId
     * @param fields
     * @return
     */
    public <V> V getField(String jobId, String field);

    /**
     * Delete all the job related documents from the database.
     *
     * @param jobId
     * @return
     * @throws UnknownJobException If the jobId is not recognised
     * @throws DataStoreException If there is a datastore error
     */
    public boolean deleteJob(String jobId) throws UnknownJobException, DataStoreException;

    /**
     * Set the job status
     *
     * @param jobId
     * @param status
     * @return
     * @throws UnknownJobException If there is no job with id <code>jobId</code>
     */
    public boolean setJobStatus(String jobId, JobStatus status) throws UnknownJobException;

    /**
     * Set the job's finish time and status
     * @param jobId
     * @param time
     * @param status
     * @return
     * @throws UnknownJobException
     */
    public boolean setJobFinishedTimeAndStatus(String jobId, Date time, JobStatus status)
            throws UnknownJobException;
}
