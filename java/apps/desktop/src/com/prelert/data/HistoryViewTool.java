package com.prelert.data;

import java.io.Serializable;


/**
 * Extension of the generic ViewTool class for launching an evidence History View.
 * It adds properties which are specific to History Views, such as the time frame
 * to display in the new Usage View.
 * @author Pete Harverson
 */
public class HistoryViewTool extends ViewTool implements Serializable
{
	private TimeFrame 	m_TimeFrame;
	
	/**
	 * Returns the time frame to display in the History View.
	 * @return the time frame e.g. WEEK, DAY, HOUR, MINUTE or SECOND.
	 */
	public TimeFrame getTimeFrame()
	{
		return m_TimeFrame;
	}


	/**
	 * Sets the time frame of the view to open e.g. SECOND, MINUTE, HOUR.
	 * @param timeFrame the time frame e.g. WEEK, DAY, HOUR, MINUTE or SECOND.
	 */
	public void setTimeFrame(TimeFrame timeFrame)
	{
		m_TimeFrame = timeFrame;
	}
	
	
	/**
	 * Sets the time frame of the view to open e.g. SECOND, MINUTE, HOUR.
     * @param timeFrame the time frame of the view to open.
     * @throws IllegalArgumentException if there is no TimeFrame enum
     * with the specified name.
     */
    public void setTimeFrame(String timeFrame) throws IllegalArgumentException
    {
    	m_TimeFrame = Enum.valueOf(TimeFrame.class, timeFrame);
    }
    
    
	/**
	 * Returns a String summarising the properties of this tool.
	 * @return a String displaying the properties of the History View Tool.
	 */
    public String toString()
    {
		StringBuilder strRep = new StringBuilder("{");
		   
		strRep.append("Name=");
		strRep.append(getName());

		strRep.append(",Open View=");
		strRep.append(getViewToOpen());
		
		strRep.append(",Timeframe=");
		strRep.append(getTimeFrame());
		
		strRep.append('}');

		return strRep.toString();
    }
}
