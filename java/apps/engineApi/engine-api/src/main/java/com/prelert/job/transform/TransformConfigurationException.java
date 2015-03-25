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

package com.prelert.job.transform;

import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.rs.data.ErrorCode;

/**
 * Represents the invalid configuration of a transform.
 */
public class TransformConfigurationException extends JobConfigurationException
{
	private static final long serialVersionUID = -8930949236695246267L;


	/**
	 * Create a new TransformConfigurationException.
	 *
	 * @param message Details of error explaining the context
	 * @param errorCode See {@linkplain com.prelert.rs.data.ErrorCode}
	 */
	public TransformConfigurationException(String message, ErrorCode errorCode)
	{
		super(message, errorCode);
	}

	public TransformConfigurationException(String message, ErrorCode errorCode, Throwable cause)
	{
		super(message, errorCode, cause);
	}

}
