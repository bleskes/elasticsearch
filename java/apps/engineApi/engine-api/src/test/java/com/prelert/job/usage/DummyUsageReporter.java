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

package com.prelert.job.usage;

import org.apache.log4j.Logger;

import com.prelert.job.persistence.UsagePersister;
import com.prelert.job.persistence.none.NoneUsagePersister;
import com.prelert.job.usage.UsageReporter;

public class DummyUsageReporter extends UsageReporter
{
	long m_TotalByteCount;
	long m_TotalFieldCount;
	long m_TotalRecordCount;

	public DummyUsageReporter(String jobId, Logger logger)
	{
		super(jobId, new NoneUsagePersister(), logger);

		m_TotalByteCount = 0;
		m_TotalFieldCount = 0;
		m_TotalRecordCount = 0;
	}

	public DummyUsageReporter(String jobId, UsagePersister persister, Logger logger)
	{
	    super(jobId, persister, logger);

	    m_TotalByteCount = 0;
	    m_TotalFieldCount = 0;
	    m_TotalRecordCount = 0;
	}


	@Override
    public void addBytesRead(long bytesRead)
    {
       super.addBytesRead(bytesRead);

        m_TotalByteCount += bytesRead;
    }

    @Override
    public void addFieldsRecordsRead(long fieldsRead)
    {
        super.addFieldsRecordsRead(fieldsRead);

        m_TotalFieldCount += fieldsRead;
        ++m_TotalRecordCount;
    }

	public long getTotalBytesRead()
	{
		return m_TotalByteCount;
	}

	public long getTotalFieldsRead()
	{
		return m_TotalFieldCount;
	}

	public long getTotalRecordsRead()
	{
		return m_TotalRecordCount;
	}

}
