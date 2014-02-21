package com.prelert.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ShowInfoTool extends Tool implements Serializable
{
	private List<String>				m_ActiveAttributes;
	
	/**
	 * Sets the list of active Info attributes. These are attributes of data
	 * displayed in the Show Info window from which actions can be fired e.g. for opening
	 * filtered Evidence View windows from attributes displayed in a Show Info window.
	 * @param activeInfoAttributes the list names of active attributes.
	 */
	public void setActiveAttributes(List<String> activeAttributes)
	{
		m_ActiveAttributes = activeAttributes;
	}
	
	
	/**
	 * Returns the list of active Info attributes. These are attributes of data
	 * displayed in the Show Info window from which actions can be fired e.g. for opening
	 * filtered Evidence View windows from attributes displayed in a Show Info window.
	 * @return  the list names of active attributes.
	 */
	public List<String> getActiveAttributes()
	{
		return m_ActiveAttributes;
	}
	
	
	/**
	 * Adds an attribute to the list of active Info attributes.
	 * @param attributeName	name of attribute to add.
	 */
	public void addActiveAttribute(String attributeName)
	{
		if (m_ActiveAttributes == null)
		{
			m_ActiveAttributes = new ArrayList<String>();
		}
		
		m_ActiveAttributes.add(attributeName);
	}
	
	
	/**
	 * Returns a String summarising the properties of this tool.
	 * @return a String displaying the properties of the tool.
	 */
    public String toString()
    {
		StringBuilder strRep = new StringBuilder("{");
		   
		strRep.append("Name=");
		strRep.append(getName());

		strRep.append(",Active Attributes=");
		strRep.append(m_ActiveAttributes);

		strRep.append('}');

		return strRep.toString();
    }
}
