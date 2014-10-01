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
package com.prelert.job.status;

import com.prelert.rs.data.ErrorCode;

/**
 *  Records sent to autodetect should be in ascending chronological 
 *  order else they are ignored and a error logged. This exception
 *  represents the case where a high proportion of messages are not
 *  in temporal order. 
 */
public class OutOfOrderRecordsException extends Exception 
{
	private static final long serialVersionUID = -7088347813900268191L;
	
	private long m_NumberBad;
	private long m_TotalNumber;
	
	public OutOfOrderRecordsException(long numberBadRecords, 
			long totalNumberRecords)
	{
		m_NumberBad = numberBadRecords;
		m_TotalNumber = totalNumberRecords;
	}
	
	public ErrorCode getErrorCode()
	{
		return ErrorCode.TOO_MANY_OUT_OF_ORDER_RECORDS;
	}
	
	/**
	 * The number of out of order records
	 * @return
	 */
	public long getNumberOutOfOrder()
	{
		return m_NumberBad;
	}
	
	/**
	 * Total number of records (good + bad)
	 * @return
	 */
	public long getTotalNumber()
	{
		return m_TotalNumber;
	}
	
	@Override
	public String getMessage()
	{
		return String.format("A high proportion of records are not in ascending "
				+ "chronological  order (%d of %d).",
				m_NumberBad, m_TotalNumber);
	}
}
