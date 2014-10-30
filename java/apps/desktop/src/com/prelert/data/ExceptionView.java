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

package com.prelert.data;

import java.io.Serializable;

public class ExceptionView extends ListView implements Serializable
{
	private int 		m_NoiseLevel;
	private TimeFrame	m_TimeWindow;
	
	
	/**
	 * Creates a new Exception View object, with a default DAY time window
	 * and a noise level of 1.
	 */
	public ExceptionView()
	{
		m_NoiseLevel = 1;
		m_TimeWindow = TimeFrame.DAY;
	}
	
	
	/**
	 * Returns the level of noise to act as the filter for the exception list.
	 * @return the noise level, a value from 1 to 100.
	 */
	public int getNoiseLevel()
	{
		return m_NoiseLevel;
	}


	/**
	 * Sets the level of noise to act as the filter for the exception list.
	 * @param noiseLevel the noise level, a value from 1 to 100.
	 */
	public void setNoiseLevel(int noiseLevel)
	{
		m_NoiseLevel = noiseLevel;
	}
	
	
	/**
	 * Returns the exception time window to be used by the Exception List.
	 * @return exception time window e.g. week, day or hour.
	 */
	public TimeFrame getTimeWindow()
    {
    	return m_TimeWindow;
    }


	/**
	 * Sets the exception time window. 
	 * @param timeWindow time window for the exceptions e.g. week, day or hour.
	 */
	public void setTimeWindow(TimeFrame timeWindow)
    {	
		m_TimeWindow = timeWindow;
    }
	
	
	/**
	 * Sets the time frame of the view e.g. SECOND, MINUTE, HOUR.
     * @param timeFrame the time frame of the view.
     * @throws IllegalArgumentException if there is no TimeFrame enum
     * with the specified name.
     */
    public void setTimeWindow(String timeWindow) throws IllegalArgumentException
    {
    	m_TimeWindow = Enum.valueOf(TimeFrame.class, timeWindow);
    }
	
	
	/**
	 * Creates a new Exception View based on the properties of this view.
	 * @param filterAttribute the filter attribute for the new view (not used).
	 * @param filterValue the filter value for the new view (not used).
	 * @return new EvidenceView object.
	 */
	public ExceptionView createCopyAndAppendFilter(String filterAttribute, String filterValue)
	{
		// Copy each of the properties of the view.
		ExceptionView newView = new ExceptionView();
		newView.setName(new String(getName()));
		newView.setDataType(getDataType());
		newView.setStyleId(new String(getStyleId()));
		newView.setNoiseLevel(getNoiseLevel());
		newView.setTimeWindow(m_TimeWindow);
		newView.setDatabaseView(new String(getDatabaseView()));
		newView.setContextMenuItems(getContextMenuItems());
		if (getDoubleClickTool() != null)
		{
			newView.setDoubleClickTool(new String(getDoubleClickTool()));
		}

		return newView;
	}


	/**
	 * Returns a String summarising the properties of this View.
	 * @return a String displaying the properties of the View.
	 */
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");

	   strRep.append("ExceptionView Name=");
	   strRep.append(getName());
	   
	   strRep.append(",Data Type=");
	   strRep.append(getDataType());

	   strRep.append(",Style=");
	   strRep.append(getStyleId());
	   
	   strRep.append(",Shortcut=");
	   strRep.append(isDesktopShortcut());

	   strRep.append(",Noise Level=");
	   strRep.append(getNoiseLevel());
	   
	   strRep.append(",Time Window=");
	   strRep.append(getTimeWindow());
	   
	   strRep.append(",Database View=");
	   strRep.append(getDatabaseView());

	   strRep.append(",Context Menu=");
	   strRep.append(getContextMenuItems());

	   strRep.append('}');

	   return strRep.toString();
    }
}
