
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.xpack.prelert.job.*;
import org.elasticsearch.xpack.prelert.job.exceptions.JobIdAlreadyExistsException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;

import java.util.Optional;

/**
 * General interface for classes that persist Jobs and job data
 */
public interface JobDetailsProvider
{
    /**
     * Store the Prelert info doc (version number etc)
     *
     * @param infoDoc
     * @return
     */
    boolean savePrelertInfo(String infoDoc);

    /**
     * Ensures a given {@code jobId} corresponds to an existing job
     * @throws UnknownJobException if there is no job with {@code jobId}
     */
    void checkJobExists(String jobId) throws ResourceNotFoundException;

    /**
     * Return true if the job id is unique else if it is already used
     * by another job false is returned
     *
     * @param jobId
     * @return true or false
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
     *
     * @param job
     * @return True
     * @throws JobIdAlreadyExistsException
     */
    // TODO: rename and move?
    void createJob(JobDetails job, ActionListener<Boolean> listener) throws JobIdAlreadyExistsException;

    /**
     * Delete all the job related documents from the database.
     *
     * @param jobId
     * @param listener
     */
    // TODO: should live together with createJob (in case it moves)?
    void deleteJob(String jobId, ActionListener<Boolean> listener);

    /**
     * Updates the scheduler state for the given job
     * @param jobId the job id
     * @param schedulerState the new scheduler state
     * @return {@code true} if update was successful
     * @throws UnknownJobException If there is no job with id <code>jobId</code>
     */
    boolean updateSchedulerState(String jobId, SchedulerState schedulerState) throws UnknownJobException;

    /**
     * Retrieves the state of the scheduler for the given job
     * @param jobId the job id
     * @return the scheduler state or empty if none exists
     */
    Optional<SchedulerState> getSchedulerState(String jobId);
}
