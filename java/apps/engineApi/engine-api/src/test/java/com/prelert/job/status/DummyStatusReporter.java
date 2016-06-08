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
 *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/
package com.prelert.job.status;

import com.prelert.job.DataCounts;
import com.prelert.job.persistence.none.NoneJobDataCountsPersister;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.usage.UsageReporter;

/**
 * Dummy StatusReporter for testing abstract class
 */
public class DummyStatusReporter extends StatusReporter
{
	boolean m_StatusReported = false;
	public DummyStatusReporter(UsageReporter usageReporter)
	{
		super("DummyJobId", usageReporter, new NoneJobDataCountsPersister(), null, 1);
	}

	public DummyStatusReporter(DataCounts counts,
                            UsageReporter usageReporter)
	{
	    super("DummyJobId", counts, usageReporter, new NoneJobDataCountsPersister(), null, 1);
	}


	public boolean isStatusReported()
	{
		return m_StatusReported;
	}
}
