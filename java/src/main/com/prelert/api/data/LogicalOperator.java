/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.api.data;

/**
 * The valid set of logical operators that can be used in the 
 * filter expression.
 */
public enum LogicalOperator  
{
	EQ, GE,
	
	LT
	{
		@Override
		public boolean isOpen()
		{
			return true;
		}	

		@Override
		public boolean isLess()
		{
			return true;
		}
	},
	
	LE 
	{
		@Override
		public boolean isLess()
		{
			return true;
		}
	},
	
	GT
	{
		@Override
		public boolean isOpen()
		{
			return true;
		}
	};
	
	public boolean isOpen()
	{
		return false;
	}	
	
	public boolean isLess()
	{
		return false;
	}
};
