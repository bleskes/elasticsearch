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

import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.exceptions.JobIdAlreadyExistsException;
import com.prelert.job.exceptions.UnknownJobException;

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
	 * Returns true if the job exists else an
	 * <code>UnknownJobException</code> is thrown.
	 *
	 * @param jobId
	 * @return True
	 * @throws UnknownJobException
	 */
	public boolean jobExists(String jobId) throws UnknownJobException;

	/**
	 * Return true if the job id is unique else if it is already used
	 * by another job throw <code>JobIdAlreadyExistsException</code>
	 *
	 * @param jobId
	 * @return True
	 * @throws JobIdAlreadyExistsException
	 */
	public boolean jobIdIsUnique(String jobId) throws JobIdAlreadyExistsException;


	/**
	 * Get the details of the specific job or throw a
	 * <code>UnknownJobException</code>
	 *
	 * @param jobId
	 * @return The JobDetails
	 * @throws UnknownJobException if the job details document cannot be found
	 */
	public JobDetails getJobDetails(String jobId) throws UnknownJobException;


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
	 * @return
	 * @throws UnknownJobException if there is no job with the id.
	 */
	public boolean updateJob(String jobId, Map<String, Object> updates)
	throws UnknownJobException;

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
	 */
	public boolean deleteJob(String jobId) throws UnknownJobException;

	/**
	 * Set the job status
	 *
	 * @param jobId
	 * @param status
	 * @return
	 * @throws UnknownJobException If there is no job with id <code>jobId</code>
	 */
	public boolean setJobStatus(String jobId, JobStatus status)
	throws UnknownJobException;

	/**
	 * Set the job's finish time and status
	 * @param jobId
	 * @param time
	 * @param status
	 * @return
	 * @throws UnknownJobException
	 */
	public boolean setJobFinishedTimeandStatus(String jobId, Date time,
			JobStatus status)
	throws UnknownJobException;

}
