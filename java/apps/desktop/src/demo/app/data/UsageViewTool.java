package demo.app.data;

import java.io.Serializable;

/**
 * Extension of the generic ViewTool class for launching a Usage View.
 * It adds properties which are specific to Usage Views, such as the metric,
 * time frame, source and user to display in the new Usage View.
 * @author Pete Harverson
 */
public class UsageViewTool extends ViewTool implements Serializable
{
	private String 		m_Metric;
	private TimeFrame 	m_TimeFrame;
	
	private String m_SourceArg;
	private String m_AttributeName;
	private String m_AttributeValueArg;
	private String m_TimeArg;


	/**
	 * Returns the name of the metric to be shown in the Usage View.
	 * @return the name of the metric e.g. total or serverload.
	 */
	public String getMetric()
	{
		return m_Metric;
	}


	/**
	 * Sets the name of the metric to be shown in the Usage View.
	 * @param metric the name of the metric to show.
	 */
	public void setMetric(String metric)
	{
		m_Metric = metric;
	}


	/**
	 * Returns the time frame to display in the Usage View.
	 * @return the time frame e.g. WEEK, DAY or HOUR.
	 */
	public TimeFrame getTimeFrame()
	{
		return m_TimeFrame;
	}


	/**
	 * Sets the time frame of the view to open e.g. SECOND, MINUTE, HOUR.
	 * @param timeFrame the time frame e.g. WEEK, DAY or HOUR.
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
	 * Returns the name of attribute whose value should be provided as the 
	 * Source (server) argument for the view to open.
     * @return the name of the field used to supply the value of the source.
     */
    public String getSourceArg()
    {
    	return m_SourceArg;
    }


	/**
	 * Sets the name of attribute whose value should be provided as the 
	 * Source (server) argument for the view to open.
     * @param sourceArg the name of the field used to supply the value of the source.
     */
    public void setSourceArg(String sourceArg)
    {
    	m_SourceArg = sourceArg;
    }
    
    
	/**
	 * Returns the attribute name, if any, to set in the Usage View.
	 * @return the attribute name e.g. username or app_id.
     */
    public String getAttributeName()
    {
    	return m_AttributeName;
    }


	/**
	 * Sets the attribute name to use in the Usage View.
     * @param attrName the attribute name.
     */
    public void setAttributeName(String attrName)
    {
    	m_AttributeName = attrName;
    }


	/**
	 * Returns the name of attribute whose value should be provided as the 
	 * attribute value argument for the view to open.
     * @return the name of the field used to supply the attribute value.
     */
    public String getAttributeValueArg()
    {
    	return m_AttributeValueArg;
    }


	/**
	 * Sets the name of attribute whose value should be provided as the 
	 * attribute value argument for the view to open.
     * @param attrValueArg the name of the field used to supply the attribute value.
     */
    public void setAttributeValueArg(String attrValueArg)
    {
    	m_AttributeValueArg = attrValueArg;
    }


	/**
	 * Returns the name of attribute whose value should be provided as the 
	 * time argument for the view to open.
     * @return the name of the field used to supply the value of the time.
     */
    public String getTimeArg()
    {
    	return m_TimeArg;
    }


	/**
	 * Sets the name of attribute whose value should be provided as the 
	 * time argument for the view to open.
     * @param userArg the name of the field used to supply the value of the time.
     */
    public void setTimeArg(String timeArg)
    {
    	m_TimeArg = timeArg;
    }


	/**
	 * Returns a String summarising the properties of this tool.
	 * @return a String displaying the properties of the Usage View Tool.
	 */
    public String toString()
    {
		StringBuilder strRep = new StringBuilder("{");
		   
		strRep.append("Name=");
		strRep.append(getName());

		strRep.append(",Open View=");
		strRep.append(getViewToOpen());

		strRep.append(",Metric=");
		strRep.append(getMetric());
		
		strRep.append(",Timeframe=");
		strRep.append(getTimeFrame());
		
		strRep.append(",Source Arg=");
		strRep.append(getSourceArg());
		
		strRep.append(",Attribute name=");
		strRep.append(getAttributeName());
		
		strRep.append(",Attribute value arg=");
		strRep.append(getAttributeValueArg());
		
		strRep.append(",Time Arg=");
		strRep.append(getTimeArg());
		
		strRep.append('}');

		return strRep.toString();
    }

}
