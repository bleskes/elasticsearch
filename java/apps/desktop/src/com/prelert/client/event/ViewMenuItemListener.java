package com.prelert.client.event;

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;

import com.prelert.client.*;
import com.prelert.client.widget.ViewMenuItem;

/**
 * Listener for menu item events from view windows.
 * @author Pete Harverson
 */
public class ViewMenuItemListener<E extends ComponentEvent> extends SelectionListener<ComponentEvent>
{
	private ViewMenuItem 		m_ViewMenuItem;
	private ViewWindow			m_ViewWindow;
	
	
	/**
	 * Creates a new ViewMenuItemListener object to listen for selection events
	 * from a menu item on a Prelert view window.
	 * @param menuItem the menu item to listen for selection events from.
	 * @param viewWindow the View Window which contains the specified menu item.
	 */
	public ViewMenuItemListener(ViewMenuItem menuItem, ViewWindow viewWindow)
	{
		m_ViewMenuItem =  menuItem;
		m_ViewWindow = viewWindow;
	}
	
	
	/**
	 * Fires when the menu item has been selected.
	 * @param ce the ComponentEvent.
	 */
	public void componentSelected(ComponentEvent ce)
	{
		processMenuItemEvent(ce);
	}
	
	
	/**
	 * Sent when an event that the listener has registered for occurs. 
	 * @param ce the ComponentEvent.
	 */
    public void handleEvent(ComponentEvent ce)
    {
    	processMenuItemEvent(ce);
    }
	

    /**
     * Processes the menu item, which typically involves running the tool
     * configured on the menu item against the selected record in the view window.
     * @param ce the ComponentEvent.
     */
	protected void processMenuItemEvent(ComponentEvent ce)
	{
		m_ViewWindow.runTool(m_ViewMenuItem.getTool());
	}
}
