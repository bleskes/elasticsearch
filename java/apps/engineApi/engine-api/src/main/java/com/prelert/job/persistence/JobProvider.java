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

import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.quantiles.Quantiles;

public interface JobProvider extends JobDetailsProvider, JobResultsProvider, JobRenormaliser
{
	/**
	 * Get the persisted quantiles state for the job
	 */
	public Quantiles getQuantiles(String jobId)
	throws UnknownJobException;

	/**
	 * Refresh the datastore index so that all recent changes are
	 * available to search operations. This is a synchronous blocking
	 * call that should not return until the index has been refreshed.
	 *
	 * @param jobId
	 */
	public void refreshIndex(String jobId);
}
