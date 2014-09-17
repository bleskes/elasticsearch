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

import com.prelert.rs.data.ErrorCode;

/**
 * If the timestamp field of a record cannot be read or the 
 * date format is incorrect the record is ignored. This 
 * exception is thrown when a high proportion of records
 * have a bad timestamp.
 */
public class HighProportionOfBadTimestampsException extends Exception 
{
	private static final long serialVersionUID = -7776085998658495251L;

	private long m_NumberBad;
	private long m_TotalNumber;
	
	public HighProportionOfBadTimestampsException(long numberBadRecords, 
			long totalNumberRecords)
	{
		m_NumberBad = numberBadRecords;
		m_TotalNumber = totalNumberRecords;
	}
	
	public ErrorCode getErrorCode()
	{
		return ErrorCode.TOO_MANY_BAD_DATES;
	}
	
	/**
	 * The number of bad records
	 * @return
	 */
	public long getNumberBad()
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
		return String.format("A high proportion of records have a timestamp "
				+ "that cannot be interpreted (%d of %d).",
				m_NumberBad, m_TotalNumber);
	}
	
}
