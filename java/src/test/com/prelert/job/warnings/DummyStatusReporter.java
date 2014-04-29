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
package com.prelert.job.warnings;

public class DummyStatusReporter implements StatusReporter 
{
	private int m_RecordsWritten = 0;
	private int m_RecordsDiscarded = 0;
	private int m_DateParseErrorsCount = 0;
	private int m_MissingFieldErrorCount = 0;
	private int m_OutOfOrderRecordCount = 0;
	
	public DummyStatusReporter()
	{
		
	}
		
	@Override
	public void reportRecordsWritten(int recordsWritten, int recordsDiscarded)
	throws HighProportionOfBadRecordsException 
	{
		m_RecordsWritten = recordsWritten;
		m_RecordsDiscarded = recordsDiscarded;
	}

	@Override
	public void reportDateParseError(String date)
	throws HighProportionOfBadRecordsException 
	{
		m_DateParseErrorsCount++;
	}

	@Override
	public void reportMissingField(String field) 
	{
		m_MissingFieldErrorCount++;
	}


	@Override
	public void reportOutOfOrderRecord(long date, long previousDate)
	throws OutOfOrderRecordsException 
	{
		m_OutOfOrderRecordCount++;
	}

	public int getRecordsWritten() 
	{
		return m_RecordsWritten;
	}

	public int getRecordsDiscarded() 
	{
		return m_RecordsDiscarded;
	}

	public int getDateParseErrorsCount() 
	{
		return m_DateParseErrorsCount;
	}

	public int getMissingFieldErrorCount() 
	{
		return m_MissingFieldErrorCount;
	}

	public int getOutOfOrderRecordCount() 
	{
		return m_OutOfOrderRecordCount;
	}
}
