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

import java.util.Date;

import org.apache.log4j.Logger;


/**
 * Interface for classes that update {@linkplain Bucket Buckets}
 * for a particular job with new normalised anomaly scores and
 * unusual scores
 */
public interface JobRenormaliser
{
	/**
	 * Update the anomaly score field on all previously persisted buckets
	 * and all contained records
	 * @param sysChangeState
	 * @param endTime
	 * @param logger
	 */
	public void updateBucketSysChange(String sysChangeState,
										Date endTime, Logger logger);


	/**
	 * Update the unsual score field on all previously persisted buckets
	 * and all contained records
	 * @param unusualBehaviourState
	 * @param endTime
	 * @param logger
	 */
	public void updateBucketUnusualBehaviour(String unusualBehaviourState,
											Date endTime, Logger logger);
};

