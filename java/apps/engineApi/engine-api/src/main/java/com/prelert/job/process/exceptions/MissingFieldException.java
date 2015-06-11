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
package com.prelert.job.process.exceptions;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobException;

/**
 * Represents the case where a job has been configured to use
 * a specific field but that field is missing from the data.
 */
public class MissingFieldException extends JobException
{
	private static final long serialVersionUID = -5303432170987377451L;

	private final String m_MissingFieldName;

	public MissingFieldException(String fieldName, String message)
	{
		super(message, ErrorCodes.MISSING_FIELD);
		m_MissingFieldName = fieldName;
	}

	public String getMissingFieldName()
	{
		return m_MissingFieldName;
	}
}
