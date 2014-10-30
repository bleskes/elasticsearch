/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package demo.app.client.event;

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;

import demo.app.client.ViewMenuItem;
import demo.app.client.ViewWidget;


/**
 * Listener for menu item events from view widgets.
 * @author Pete Harverson
 */
public class ViewMenuItemListener<E extends MenuEvent> extends SelectionListener<MenuEvent>
{
	private ViewMenuItem 		m_ViewMenuItem;
	private ViewWidget			m_ViewWidget;
	
	
	/**
	 * Creates a new ViewMenuItemListener object to listen for selection events
	 * from a menu item on a Prelert view window.
	 * @param menuItem the menu item to listen for selection events from.
	 * @param viewWindow the View Widget which contains the specified menu item 
	 * 		which will be notified when the menu item is selected.
	 */
	public ViewMenuItemListener(ViewMenuItem menuItem, ViewWidget viewWidget)
	{
		m_ViewMenuItem =  menuItem;
		m_ViewWidget = viewWidget;
	}
	
	
	/**
	 * Fires when the menu item has been selected.
	 * @param ce the ComponentEvent.
	 */
	public void componentSelected(MenuEvent ce)
	{
		processMenuItemEvent(ce);
	}
	
	
	/**
	 * Sent when an event that the listener has registered for occurs. 
	 * @param ce the ComponentEvent.
	 */
    public void handleEvent(MenuEvent ce)
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
		m_ViewWidget.runTool(m_ViewMenuItem.getTool());
	}


}
