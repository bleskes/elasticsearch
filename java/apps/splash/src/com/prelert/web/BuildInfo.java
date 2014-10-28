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

package com.prelert.web;


/**
 * Bean class containing UI build properties such as version and build numbers.
 * @author Pete Harverson
 */
public class BuildInfo
{
	public static final String VERSION_NUMBER = "3.2";
	public static final String BUILD_NUMBER = "9";
	public static final String BUILD_YEAR = "2010";
	
	
	/**
	 * Returns the Prelert UI version number.
	 * @return the version number.
	 */
	public String getVersionNumber()
	{
		return VERSION_NUMBER;
	}
	
	
	/**
	 * Returns the Prelert UI build number.
	 * @return the build number.
	 */
	public String getBuildNumber()
	{
		return BUILD_NUMBER;
	}
	
	
	/**
	 * Returns the year of the current build.
	 * @return the build year.
	 */
	public String getBuildYear()
	{
		return BUILD_YEAR;
	}
}
