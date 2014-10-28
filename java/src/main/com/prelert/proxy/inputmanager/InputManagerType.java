/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
 ***********************************************************/

package com.prelert.proxy.inputmanager;

/**
 * Enum of the different InputManager types. 
 */
public enum InputManagerType 
{
	INTERNAL
	{
		@Override
		public String toString()
		{
			return "Internal";
		}	
	},
	
	EXTERNAL
	{
		@Override
		public String toString()
		{
			return "External";
		}
		
		@Override
		public boolean isExternalType()
		{
			return true;
		}
	},

	EXTERNALPOINTS
	{
		@Override
		public String toString()
		{
			return "ExternalPoints";
		}
		
		@Override
		public boolean isExternalType()
		{
			return true;
		}
	},

	NOTIFICATION
	{
		@Override
		public String toString()
		{
			return "Notification";
		}
	},
	
	GZIP
	{
		@Override
		public String toString()
		{
			return "GZip";
		}		
	};
	
	
	public boolean isExternalType()
	{
		return false;
	}

	
	static public InputManagerType enumValue(String name)
	{
		return Enum.valueOf(InputManagerType.class, name.toUpperCase());
	}
};
