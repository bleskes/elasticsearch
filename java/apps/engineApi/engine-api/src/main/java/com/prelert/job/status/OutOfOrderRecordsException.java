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
package com.prelert.job.status;

import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodes;

/**
 *  Records sent to autodetect should be in ascending chronological
 *  order else they are ignored and a error logged. This exception
 *  represents the case where a high proportion of messages are not
 *  in temporal order.
 */
public class OutOfOrderRecordsException extends JobException
{
	private static final long serialVersionUID = -7088347813900268191L;

	private final long m_NumberBad;
	private final long m_TotalNumber;

	public OutOfOrderRecordsException(long numberBadRecords,
			long totalNumberRecords)
	{
		super(String.format("A high proportion of records are not in ascending "
						+ "chronological order (%d of %d) and/or not within latency.",
						numberBadRecords, totalNumberRecords),
			ErrorCodes.TOO_MANY_OUT_OF_ORDER_RECORDS);

		m_NumberBad = numberBadRecords;
		m_TotalNumber = totalNumberRecords;
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
}
