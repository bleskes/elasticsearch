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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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
package com.prelert.job.process;

import java.util.Date;

import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;

public interface JobDetailsProvider 
{
	/**
	 * Get the <code>JobDetail</code>s for the given job id. 
	 * 
	 * @param jobId The job to look up
	 * @return
	 * @throws UnknownJobException If the jobId is not recognised
	 */
	public JobDetails getJobDetails(String jobId) throws UnknownJobException;
	
	/**
	 * Get the persisted detector state for the job or <code>null</code>
	 * @param jobId
	 * @return <code>null</code> or the DetectorState if it has been peristed
	 * @throws UnknownJobException If the jobId is not recognised
	 */
	public DetectorState getPersistedState(String jobId) throws UnknownJobException;
	
	/**
	 * Set the job status and finish time for the job.
	 * 
	 * @param jobId
	 * @param time
	 * @param status
	 * @return
	 * @throws UnknownJobException
	 */
	public boolean setJobFinishedTimeandStatus(String jobId, Date time, 
			JobStatus status) throws UnknownJobException;
}
