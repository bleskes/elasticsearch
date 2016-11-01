/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.exceptions.JobIdAlreadyExistsException;

/**
 * General interface for classes that persist Jobs and job data
 */
public interface JobDetailsProvider
{
    /**
     * Store the Prelert info doc (version number etc)
     */
    boolean savePrelertInfo(String infoDoc);

    /**
     * Ensures a given {@code jobId} corresponds to an existing job
     * @throws ResourceNotFoundException if there is no job with {@code jobId}
     */
    void checkJobExists(String jobId) throws ResourceNotFoundException;

    /**
     * Return true if the job id is unique else if it is already used
     * by another job false is returned
     */
    boolean jobIdIsUnique(String jobId);

    /**
     * Returns a {@link BatchedDocumentsIterator} that allows querying
     * and iterating over all jobs
     *
     * @return a job {@link BatchedDocumentsIterator}
     */
    BatchedDocumentsIterator<JobDetails> newBatchedJobsIterator();

    /**
     * Save the details of the new job to the datastore.
     * Throws <code>JobIdAlreadyExistsException</code> if a job with the
     * same Id already exists.
     */
    // TODO: rename and move?
    void createJob(JobDetails job, ActionListener<Boolean> listener) throws JobIdAlreadyExistsException;

    /**
     * Delete all the job related documents from the database.
     */
    // TODO: should live together with createJob (in case it moves)?
    void deleteJob(String jobId, ActionListener<Boolean> listener);
}
