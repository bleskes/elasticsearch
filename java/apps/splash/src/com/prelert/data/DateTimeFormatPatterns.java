/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.data;


/**
 * Constants class containing patterns used in applications for date-time formats.
 * @author Pete Harverson
 */
public final class DateTimeFormatPatterns
{
	public static final String WEEK_PATTERN = "yyyy-MM-dd HH:mm:ss";
	
	public static final String DAY_PATTERN = "yyyy-MM-dd";
	
	public static final String HOUR_PATTERN = "yyyy-MM-dd HH:00-59";
	
	public static final String MINUTE_PATTERN = "yyyy-MM-dd HH:mm";
	
	public static final String SECOND_PATTERN = "yyyy-MM-dd HH:mm:ss";
	
	
	private DateTimeFormatPatterns()
	{
		
	}
}
