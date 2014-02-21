package com.prelert.data;

import java.io.Serializable;

/**
 * Class encapsulating the configuration properties of a generic tool for
 * launching a Prelert Desktop View. 
 * <p>
 * Subclasses for opening specific types of View, such as List Views or Usage
 * Views, add extra properties specific to that particular type of View.
 * @author Pete Harverson
 */
public class ViewTool extends Tool implements Serializable
{
	private String m_ViewToOpen;


	/**
	 * Returns the name of the view to open when running this tool.
	 * @return the name of the view to open.
	 */
	public String getViewToOpen()
	{
		return m_ViewToOpen;
	}


	/**
	 * Sets the name of the view to open when running this tool.
	 * @param viewToOpen the name of the view to open.
	 */
	public void setViewToOpen(String viewToOpen)
	{
		m_ViewToOpen = viewToOpen;
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

		strRep.append(",Open View=");
		strRep.append(m_ViewToOpen);

		strRep.append('}');

		return strRep.toString();
    }
}
