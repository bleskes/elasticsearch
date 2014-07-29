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

package com.prelert.job.normalisation;

/**
 * Enum for the different normalization types with
 * toString and fromString methods
 */
public enum NormalizationType 
{
	STATE_CHANGE 
	{	
		@Override 
		public String toString()
		{
			return SYS_CHANGE_NORMALIZATION;
		}
	},
	UNUSUAL_BEHAVIOUR 
	{		
		@Override 
		public String toString()
		{
			return UNUSUAL_BEHAVIOUR_NORMALIZATION;
		}
	},
	BOTH
	{
		@Override 
		public String toString()
		{
			return BOTH_NORMALIZATIONS;
		}
	};
	
	static final public String SYS_CHANGE_NORMALIZATION = "s";
	static final public String UNUSUAL_BEHAVIOUR_NORMALIZATION = "u";
	static final public String BOTH_NORMALIZATIONS = "both";
	
	static public NormalizationType fromString(String value)
	{
		switch (value)
		{
		case SYS_CHANGE_NORMALIZATION:
			return STATE_CHANGE;
		case UNUSUAL_BEHAVIOUR_NORMALIZATION:
			return UNUSUAL_BEHAVIOUR;
		case BOTH_NORMALIZATIONS:
			return BOTH;
		default:
			throw new IllegalArgumentException("No enum value for '" + value + "'");
			
		}
	}
}
