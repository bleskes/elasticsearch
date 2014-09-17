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

import java.util.List;

public class DummyJobDataPersister implements JobDataPersister 
{
	private int m_RecordCount = 0;
	

	@Override
	public void setFieldMappings(List<String> fields, List<String> byFields,
			List<String> overFields, List<String> partitionFields,
			String[] header) 
	{

	}

	@Override
	public void persistRecord(long epoch, String[] record) 
	{
		m_RecordCount++;
	}
	
	@Override
	public void flushRecords() 
	{
	}
	
	@Override
	public boolean deleteData() 
	{
		return false;
	}
	
	public int getRecordCount()
	{
		return m_RecordCount;
	}
}
