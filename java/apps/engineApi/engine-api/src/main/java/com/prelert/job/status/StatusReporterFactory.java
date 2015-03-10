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

package com.prelert.job.status;

import org.apache.log4j.Logger;

import com.prelert.job.DataCounts;
import com.prelert.job.usage.UsageReporter;

/**
 * Abstract Factory method for creating new {@link StatusReporter}
 * instances.
 */
public interface StatusReporterFactory
{
	/**
	 * Return a new StatusReporter for the given job id.
	 * @param jobId
	 * @param counts The persisted counts for the job
	 * @param usageReporter
	 * to be analysed in each record. This count does not include the
	 * time field
	 * @param logger The job logger
	 * @return
	 */
	public StatusReporter newStatusReporter(String jobId, DataCounts
			counts, UsageReporter usageReporter, Logger logger);
}
