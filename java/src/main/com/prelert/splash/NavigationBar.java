/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.splash;

import java.util.HashMap;

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.TriggerField;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.menu.SeparatorMenuItem;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.Image;


import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.ShowModuleEvent;
import com.prelert.client.gxt.ModuleComponent;


/**
 * Navigation bar component. It acts as container for the company logo,
 * links to the UI modules, search box and log out link.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>ChangePasswordClick</b> : ComponentEvent(navigationBar)<br>
 * <div>Fires when the user clicks on the Change Password link.</div>
 * <ul>
 * <li>navigationBar : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>LogoutClick</b> : ComponentEvent(navigationBar)<br>
 * <div>Fires when the user clicks on the Log Out link.</div>
 * <ul>
 * <li>navigationBar : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>RunSearchClick</b> : ShowModuleEvent(navigationBar, moduleID)<br>
 * <div>Fires when the triggers a search from the Search box.</div>
 * <ul>
 * <li>navigationBar : this</li>
 * <li>moduleID: the ID of the Search module.
 * </ul>
 * </dd>
 * 
 * <dd><b>ShowModuleClick</b> : ShowModuleEvent(navigationBar, moduleID)<br>
 * <div>Fires when the user clicks on a link to a UI module.</div>
 * <ul>
 * <li>navigationBar : this</li>
 * <li>moduleID : the ID of the module whose link has been clicked on</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class NavigationBar extends LayoutContainer
{
	private String			m_ActiveModuleId;
	private HashMap<String, NavigationBarLink>	m_ModuleLinks;
	
	private TriggerField<String>				m_SearchField;
	
	
	/**
	 * Creates a new navigation bar, initially holding the company logo, and
	 * change password and log out links.
	 */
	public NavigationBar()
	{
		m_ModuleLinks = new HashMap<String, NavigationBarLink>();
		
		HBoxLayout layout = new HBoxLayout(); 
		layout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);
		
		setLayout(layout);
		
		addStyleName("prl-navigationBar");
		
		// Add the company logo.
		add(new Image(ClientUtil.CLIENT_IMAGES.logo()), 
        		new HBoxLayoutData(new Margins(0, 20, 0, 0)));
  
		
		// Add a spacer before the elements on the right hand side.
		HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 5, 0, 0));   
        flex.setFlex(100);   
        add(new Text(), flex); 
        
        String username = ClientUtil.getLoggedInUsername();
        if (username != null)
        {
        	// Add the user options menu.      
        	addUserToolsMenu(username);
        }
	}
	
	
	/**
	 * Adds the search box into the navigation bar.
	 */
	public void addSearchBox()
	{
		m_SearchField = new TriggerField<String>();
		m_SearchField.setTriggerStyle("x-form-search-trigger");
		m_SearchField.setEmptyText("Search");
		m_SearchField.setWidth(200);
        
		m_SearchField.addListener(Events.TriggerClick, new Listener<FieldEvent>(){

			@Override
			public void handleEvent(FieldEvent be)
            {
				fireEvent(GXTEvents.RunSearchClick, 
						new ShowModuleEvent(NavigationBar.this, SearchModule.MODULE_ID));
            }
	    	
	    });
		m_SearchField.addListener(Events.KeyPress, new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent fe)
            {
	           	if (fe.getKeyCode() == KeyCodes.KEY_ENTER)
	           	{
	           		fireEvent(GXTEvents.RunSearchClick, 
							new ShowModuleEvent(NavigationBar.this, SearchModule.MODULE_ID));
	           	}
            }
	    	
	    });
        
		// Add before the User Tools menu.
		add(m_SearchField, new HBoxLayoutData(new Margins(0, 60, 0, 0)));
		insert(m_SearchField, getItemCount()-2, new HBoxLayoutData(new Margins(0, 60, 0, 0)));
	}
	
	
	/**
	 * Adds the menu containing user 'tools' such as 'Change password' and 
	 * 'Log out' into the navigation bar.
	 * @param loggedInUsername the username of the logged-in user.
	 */
	protected void addUserToolsMenu(String loggedInUsername)
	{
		// Add a button showing the logged-in user's name.
        Button userPrefsBtn = new Button(ClientUtil.getLoggedInUsername());
        
        // Add a menu containing 'Change password' and 'Log out' items.
        // Customise the style to tie in with the navigation bar style.
        Menu userPrefsMenu = new Menu();
        userPrefsMenu.addStyleName("prl-navigationBar-menu");
        
        MenuItem changePwdItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.changePassword());
        changePwdItem.addSelectionListener(new SelectionListener<MenuEvent>(){

			@Override
            public void componentSelected(MenuEvent ce)
            {
				fireEvent(GXTEvents.ChangePasswordClick, new ComponentEvent(NavigationBar.this));
        	}
        });
        
        MenuItem logoutItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.logOut());
        logoutItem.addSelectionListener(new SelectionListener<MenuEvent>(){

			@Override
            public void componentSelected(MenuEvent ce)
            {
				fireEvent(GXTEvents.LogoutClick, new ComponentEvent(NavigationBar.this));
        	}
        });
        
        userPrefsMenu.add(changePwdItem);
        userPrefsMenu.add(new SeparatorMenuItem());
        userPrefsMenu.add(logoutItem);
        
        userPrefsBtn.setMenu(userPrefsMenu); 
        add(userPrefsBtn);
	}
	
	
	/**
	 * Returns the text in the search box.
	 * @return the text that has been entered into the search box, or <code>null</code>
	 * 	if the search box is empty.
	 */
	public String getSearchText()
	{
		String searchText = null;
		if (m_SearchField != null)
		{
			searchText = m_SearchField.getValue();
		}
		
		return searchText;
	}
	
	
	/**
	 * Adds a link into the navigation bar for a module component with the specified ID.
	 * @param id the ID of the module to create a link to.
	 * @param label internationalised text to label the module in the navigation bar.
	 */
	public void addModuleLink(String id, String label)
	{
		insertModuleLink(id, label, m_ModuleLinks.size());
	}
	
	
	/**
	 * Inserts a link into the navigation bar for a module component with the
	 * specified ID, with the index relative to other module links already present.
	 * @param id the ID of the module to create a link to.
	 * @param label internationalised text to label the module in the navigation bar.
	 * @param index the index at which the link will be inserted.
	 */
	public void insertModuleLink(String id, String label, int index)
	{
		NavigationBarLink moduleLink = new NavigationBarLink(id, label);
		m_ModuleLinks.put(id, moduleLink);
		insert(moduleLink, index + 1);	// Prelert logo is at index 0.
	}
	
	
	/**
	 * Sets the module that should be marked as active, firing a ShowModuleClick event.
	 * @param id the ID of the {@link ModuleComponent} to mark as active, 
	 * 	or <code>null</code> to leave no modules marked as active 
	 * 	e.g. when the Search page is displayed.
	 */
	public void setActiveModuleId(String id)
	{
		setActiveModuleId(id, true);
	}
	
	
	/**
	 * Sets the module that should be marked as active.
	 * @param id the ID of the {@link ModuleComponent} to mark as active, 
	 * 	or <code>null</code> to leave no modules marked as active 
	 * 	e.g. when the Search page is displayed.
	 * @param fireEvent <code>true</code> to fire a ShowModuleClick event,
	 * 	<code>false</code> otherwise.
	 */
	public void setActiveModuleId(String id, boolean fireEvent)
	{
		if (id != null)
		{
			if ( (id != m_ActiveModuleId) && (m_ModuleLinks.containsKey(id)) )
			{
				// Clear selected style from previously selected link.
				if (m_ActiveModuleId != null)
				{
					NavigationBarLink previousSelected = m_ModuleLinks.get(m_ActiveModuleId);
					previousSelected.removeStyleName("prl-navigationActive");
				}
				
				// Add selected style to link.
				NavigationBarLink selected = m_ModuleLinks.get(id);
				selected.removeStyleName("prl-navigationOver");
				selected.addStyleName("prl-navigationActive");
				
				m_ActiveModuleId = id;
				
				if (fireEvent == true)
				{
					fireEvent(GXTEvents.ShowModuleClick, new ShowModuleEvent(this, m_ActiveModuleId));
				}
			}
		}
		else
		{	
			// Clear selected style from previously selected link.
			if (m_ActiveModuleId != null)
			{
				NavigationBarLink previousSelected = m_ModuleLinks.get(m_ActiveModuleId);
				previousSelected.removeStyleName("prl-navigationActive");
			}
			
			m_ActiveModuleId = null;
		}
	}
	
	
	/**
	 * Returns the ID of the module that is currently active.
	 * @return the ID of the active module.
	 */
	public String getActiveModuleId()
	{
		return m_ActiveModuleId;
	}
	
	
	/**
	 * Enables or disables the link to the module with the specified id.
	 * @param id module ID.
	 * @param enabled <code>true</code> to enable link, <code>false</code> to disable.
	 */
	public void setModuleLinkEnabled(String id, boolean enabled)
	{
		NavigationBarLink link = m_ModuleLinks.get(id);
		if (link != null)
		{
			link.setEnabled(enabled);
		}
	}
	
	
	/**
	 * Returns whether the link to the module with the specified id is enabled.
	 * @param id module ID.
	 * @return <code>true</code> if the link is enabled, <code>false</code> if the
	 * 	link is disabled or there is no link with the specified ID.
	 */
	public boolean isModuleLinkEnabled(String id)
	{
		boolean enabled = false;
		NavigationBarLink link = m_ModuleLinks.get(id);
		if (link != null)
		{
			enabled = link.isEnabled();
		}
		return enabled;
	}
	
	
	/**
	 * Inner class for the standard navigation bar links to UI modules.
	 */
	class NavigationBarLink extends Label
	{
		
		NavigationBarLink(final String moduleId, String moduleLabel)
		{
			setText(moduleLabel);
			
			addStyleName("prl-navigationItem");
			disabledStyle = "prl-navigationItem-disabled";
			
			addListener(Events.OnClick, new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent ce)
                {
					setActiveModuleId(moduleId);
                }
				
			});
			
			
			addListener(Events.OnMouseOver, new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent ce)
                {
					if (moduleId != getActiveModuleId())
					{
						addStyleName("prl-navigationOver");
					}
                }
				
			});
			
			addListener(Events.OnMouseOut, new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent ce)
                {
					if (moduleId != getActiveModuleId())
					{
						removeStyleName("prl-navigationOver");
					}
                }
				
			});
		}
	}
	
}
