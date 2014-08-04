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

package com.prelert.job.persistence;

import com.prelert.job.DetectorState;
import com.prelert.job.UnknownJobException;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Quantiles;

/**
 * Interface for classes that persist {@linkplain Bucket Buckets},
 * {@linkplain Quantiles Quantiles} and {@link DetectorState DetectorStates}  
 */
public interface JobDataPersister 
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
	 * Persist the serialised detector state
	 * @param state
	 */
	public void persistDetectorState(DetectorState state);
	
	/**
	 * Reads all the detector state documents from 
	 * the database and returns a {@linkplain DetectorState} object.
	 * 
	 * @return
	 */
	public DetectorState retrieveDetectorState() throws UnknownJobException;
	
	/**
	 * If the job has persisted model state then this function 
	 * returns true 
	 * 
	 * @return
	 */
	public boolean isDetectorStatePersisted();
	
	/**
	 * Once all the job data has been written this function will be 
	 * called to commit the data if the implementing persister requries
	 * it. 
	 * 
	 * @return True if successful
	 */
	public boolean commitWrites();
}
