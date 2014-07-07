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

package com.prelert.job.alert.manager;


import org.apache.log4j.Logger;

import com.prelert.job.alert.Alert;
import com.prelert.rs.data.Pagination;


public class AlertManager 
{
	static public final Logger s_Logger = Logger.getLogger(AlertManager.class);
	
	public Pagination<Alert> alerts(int skip, int take, boolean expand)
	{
		return new Pagination<>();
	}
	
	
	public Pagination<Alert> alerts(int skip, int take, long epochStart, 
			long epochEnd, boolean expand)
	{
		return new Pagination<>();
	}
	
	
	public Pagination<Alert> jobAlerts(String jobId, int skip, int take, 
			boolean expand)
	{
		
		return new Pagination<>();
	}
	
	public Pagination<Alert> jobAlerts(String jobId, int skip, int take, 
			long epochStart, long epochEnd, boolean expand)
	{
		
		return new Pagination<>();
	}
}
