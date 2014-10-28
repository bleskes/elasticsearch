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

package demo.app.splash.swing;

import java.awt.Color;
import java.util.HashMap;

import demo.app.data.Severity;


/**
 * Constants class which maintains a map of the standard alert severity colours.
 * @author Pete Harverson
 */
public final class NotificationSeverityColours
{
	private static HashMap<Severity, Color> m_ColourMap;
	
	static
	{
		m_ColourMap = new HashMap<Severity, Color>(7);
		
		m_ColourMap.put(Severity.NONE, new Color(0xcccccc));
		m_ColourMap.put(Severity.CLEAR, new Color(0xf00cd00));
		m_ColourMap.put(Severity.UNKNOWN, new Color(0xb23aee));
		m_ColourMap.put(Severity.WARNING, new Color(0x63b8ff));
		m_ColourMap.put(Severity.MINOR, new Color(0xffff00));
		m_ColourMap.put(Severity.MAJOR, new Color(0xffb429));
		m_ColourMap.put(Severity.CRITICAL, new Color(0xff0000));
	}
	
	private NotificationSeverityColours()
	{
		
	}
	
	
	/**
	 * Returns the colour which indicates the specified severity label.
	 * @param severity the notification severity
	 * @return the Color for the given severity. The color matching a severity of
	 * 			'none' is returned if the supplied label is not one of the standard values.
	 */
	public static Color getColor(Severity severity)
	{
		Color color = m_ColourMap.get(severity);
		if (color == null)
		{
			color = m_ColourMap.get(Severity.NONE);
		}
		return color;
	}
}
