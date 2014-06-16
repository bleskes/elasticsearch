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

package com.prelert.rs.data;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

/**
 * This test ensures that all the error values in {@linkplain ErrorCode}
 * are unique so no 2 conditions can return the same error code.
 * This tests is designed to catch copy/paste errors.  
 */
public class ErrorCodesTest 
{
	@Test
	public void errorCodesUnique() 
	throws IllegalArgumentException, IllegalAccessException
	{
		ErrorCode[] values = ErrorCode.class.getEnumConstants();
		
		Set<Long> errorValueSet = new HashSet<>();

		for (ErrorCode value : values) 
		{
			errorValueSet.add(value.getValue());
		}
				
		Assert.assertEquals(values.length, errorValueSet.size());
	}
}
