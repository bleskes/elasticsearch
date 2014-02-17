package com.prelert.client.widget;

import com.extjs.gxt.ui.client.widget.menu.MenuItem;

import com.prelert.data.*;

/**
 * Extension of the Ext GWT MenuItem widget to hold a reference to a tool to be
 * run when the menu item is selected.
 * @author Pete Harverson
 */
public class ViewMenuItem extends MenuItem
{
	private Tool	m_Tool;
	
	
	/**
	 * Creates a new, empty View MenuItem.
	 */
	public ViewMenuItem()
	{
		
	}
	
	
	/**
	 * Creates a new menu item to run the specified tool when selected.
	 * @param viewTool tool to run when the menu item is selected.
	 */
	public ViewMenuItem(Tool viewTool)
	{
		m_Tool = viewTool;
		if(m_Tool != null)
		{
			setText(m_Tool.getName());
		}
	}
	
	
	/**
	 * Returns the tool to be run when this menu item is selected.
	 * @return the tool to run.
	 */
	public Tool getTool()
    {
    	return m_Tool;
    }

	
	/**
	 * Sets the tool to be run when this menu item is selected.
	 * @param viewTool the tool to run.
	 */
	public void setTool(Tool viewTool)
    {
		m_Tool = viewTool;
    }


}
