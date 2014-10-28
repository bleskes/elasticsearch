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

import java.io.Serializable;

/**
 * Enum representing a category of data source e.g. notification or time series.
 * @author Pete Harverson
 */
public enum DataSourceCategory implements Serializable
{
	NOTIFICATION, TIME_SERIES, TIME_SERIES_FEATURE;
	
	
	/**
	 * Returns a String representation of the DataSourceCategory enum by replacing
	 * all underscores in the constant name with spaces and converting to lowercase.
	 * @return a String representation of the DataSourceCategory.
	 */
	public String toString() 
	{
        return name().replaceAll("_", " ").toLowerCase();
    }


	/**
	 * Returns the DataSourceCategory constant which maps to the supplied String value.
	 * @param value String value of the DataSourceCategory enum which matches
	 * 		the value returned by the toString() method of this enum.
	 * @return the enum constant whose String representation is the supplied value.
	 * @throws IllegalArgumentException if there is no constant which can be mapped
	 * 			to the specified value via the <code>toString()</code> method.
	 */
	public static DataSourceCategory getValue(String value)
	{
		return valueOf(value.replaceAll(" ", "_").toUpperCase());
	}

}
