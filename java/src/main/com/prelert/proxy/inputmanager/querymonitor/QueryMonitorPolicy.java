/************************************************************
 *                                                          *
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
 ***********************************************************/

package com.prelert.proxy.inputmanager.querymonitor;

import java.util.Date;

/**
 * Interface to class which can monitor query performance and 
 * validates its arguments.
 */
public interface QueryMonitorPolicy 
{
	/**
	 * Validate the span of the arguments. A QueryMonitorPolicy may limit the 
	 * length of a time a query is done for (end - start), if so start will
	 * be set to a new valid time and false returned.
	 * 
	 * @param start The start time argument to the query.
	 * 				If this function returns false start is modified to a 
	 * 			    valid time.
	 * @param end The end time argument to the query.
	 * @return
	 */
	public boolean validateQueryArgsTimeSpan(Date start, Date end);

	/**
	 * Validate the age of the start argument. A QueryMonitorPolicy may limit 
	 * the age of the start of the query to some value. In that case start will
	 * be changed to a new value and false returned.
	 * 
	 * @param start The start time argument to the query.
	 * 				If this function returns false start is modified to a 
	 * 			    valid time.
	 * @return True if
	 */
	public boolean validateQueryDateParamsAge(Date start);
	
	/**
	 * Returns true if the query completed inside its maximum time limit.
	 * The start time of the query is the creation time of this object
	 * and the end time is when this function is called.	 
	 * 
	 * @return
	 */
	public boolean wasQueryInsideTimeLimit();
}
