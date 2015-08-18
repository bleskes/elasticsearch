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

public class DummyJobDataPersister extends JobDataPersister
{
	private int m_RecordCount = 0;

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

	public String [] getFieldNames()
	{
	    return m_FieldNames;
	}

	public int [] getFieldMappings()
	{
	    return m_FieldMappings;
	}

	public int [] getByFieldMappings()
	{
	    return m_ByFieldMappings;
	}

	public int [] getOverFieldMappings()
	{
	    return m_OverFieldMappings;
	}

	public int [] getPartitionFieldMappings()
	{
	    return m_PartitionFieldMappings;
	}
}
