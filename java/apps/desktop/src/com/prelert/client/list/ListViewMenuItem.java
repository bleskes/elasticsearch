package com.prelert.client.list;

import com.extjs.gxt.ui.client.widget.menu.MenuItem;

import com.prelert.data.*;

/**
 * Extension of the Ext GWT MenuItem widget to hold a reference to a tool to be
 * run when the menu item is selected.
 * @author Pete Harverson
 */
public class ListViewMenuItem extends MenuItem
{
	private ViewTool	m_ViewTool;
	
	
	/**
	 * Creates a new, empty View MenuItem.
	 */
	public ListViewMenuItem()
	{
		
	}
	
	
	/**
	 * Creates a new menu item to run the specified tool when selected.
	 * @param viewTool tool to run when the menu item is selected.
	 */
	public ListViewMenuItem(ViewTool viewTool)
	{
		m_ViewTool = viewTool;
		if(m_ViewTool != null)
		{
			setText(m_ViewTool.getName());
		}
	}
	
	
	/**
	 * Returns the tool to be run when this menu item is selected.
	 * @return the tool to run.
	 */
	public ViewTool getTool()
    {
    	return m_ViewTool;
    }

	
	/**
	 * Sets the tool to be run when this menu item is selected.
	 * @param viewTool the tool to run.
	 */
	public void setTool(ViewTool viewTool)
    {
		m_ViewTool = viewTool;
    }


}
