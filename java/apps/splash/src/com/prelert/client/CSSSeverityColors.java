/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package com.prelert.client;

import java.util.HashMap;

import com.prelert.data.Severity;


/**
 * Constants class which maintains a map of the CSS colours hashed against severity.
 * @author Pete Harverson
 */
public final class CSSSeverityColors
{
	private static HashMap<Severity, String> m_ColourMap;
	
	static
	{
		m_ColourMap = new HashMap<Severity, String>(7);
		
		m_ColourMap.put(Severity.NONE, "#cccccc");
		m_ColourMap.put(Severity.CLEAR, "#00cd00");
		m_ColourMap.put(Severity.UNKNOWN, "#b23aee");
		m_ColourMap.put(Severity.WARNING, "#63b8ff");
		m_ColourMap.put(Severity.MINOR, "#ffff00");
		m_ColourMap.put(Severity.MAJOR, "#ffb429");
		m_ColourMap.put(Severity.CRITICAL, "#ff0000");
	}
	
	private CSSSeverityColors()
	{
		
	}
	
	
	/**
	 * Returns the CSS hex colour notation which indicates the specified severity label.
	 * @param severity the notification severity
	 * @return the CSS hex colour notation for the given severity e.g. '#ff0000'. 
	 * 			The color matching a severity of 'none' is returned if the supplied 
	 * 			label is not one of the standard values.
	 */
	public static String getColor(Severity severity)
	{
		String hex = m_ColourMap.get(severity);
		if (hex == null)
		{
			hex = m_ColourMap.get(Severity.NONE);
		}
		return hex;
	}
}
