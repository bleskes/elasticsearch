package com.prelert.data;

import java.io.Serializable;


/**
 * Abstract class representing a tool for running an action against data in
 * a Prelert Desktop View. 
 * <p>
 * Classes for tools such as opening Views should extend this class and add in
 * properties specific for the action to be run by the tool.
 * @author Pete Harverson
 */
public abstract class Tool implements Serializable
{
	private String m_Name;
	
	/**
	 * Returns the name of the tool. This can be used as a label for the tool, for
	 * example as a label for a menu item used for running the tool.
	 * @return the name of the tool.
	 */
	public String getName()
	{
		return m_Name;
	}


	/**
	 * Sets the name of the tool. This can be used as a label for the tool, for
	 * example as a label for a menu item used for running the tool.
	 * @param name the name of the tool.
	 */
	public void setName(String name)
	{
		m_Name = name;
	}
}
