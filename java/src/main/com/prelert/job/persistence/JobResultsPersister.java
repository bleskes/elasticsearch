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

import com.prelert.job.MemoryUsage;

import com.prelert.job.quantiles.Quantiles;
import com.prelert.rs.data.Bucket;

/**
 * Interface for classes that persist {@linkplain Bucket Buckets} and
 * {@linkplain Quantiles Quantiles}
 */
public interface JobResultsPersister 
{
	/**
	 * Persist the result bucket
	 * @param bucket
	 */
	public void persistBucket(Bucket bucket);


	/**
	 * Persist the quantiles
	 * @param quantiles
	 */
	public void persistQuantiles(Quantiles quantiles);

	/**
	 * Persist the memory usage data
	 * @param memoryUsage
	 */
	public void persistMemoryUsage(MemoryUsage memoryUsage);

	/**
	 * Increment the jobs bucket result count by <code>count</code>
	 * @param count
	 */
	public void incrementBucketCount(long count);


	/**
	 * Once all the job data has been written this function will be 
	 * called to commit the data if the implementing persister requries
	 * it. 
	 * 
	 * @return True if successful
	 */
	public boolean commitWrites();
}
