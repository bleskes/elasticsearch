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

package com.prelert.splash;

import java.util.HashMap;

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.form.TriggerField;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.NavigationBarEvent;
import com.prelert.client.gxt.ModuleComponent;


/**
 * Navigation bar component. It acts as container for the company logo,
 * links to the UI modules, search box and log out link.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>LogoutClick</b> : NavigationBarEvent(navigationBar)<br>
 * <div>Fires when the user clicks on the Log Out link.</div>
 * <ul>
 * <li>navigationBar : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>RunSearchClick</b> : NavigationBarEvent(navigationBar)<br>
 * <div>Fires when the triggers a search from the Search box.</div>
 * <ul>
 * <li>navigationBar : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>ShowModuleClick</b> : NavigationBarEvent(navigationBar, module)<br>
 * <div>Fires when the user clicks on a link to a UI module.</div>
 * <ul>
 * <li>navigationBar : this</li>
 * <li>module : the module that has been clicked on</li>
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
	private HashMap<String, ModuleComponent>	m_Modules;
	
	private TriggerField<String>				m_SearchField;
	
	
	/**
	 * Creates a new navigation bar, initially holding the company logo
	 * and log out link.
	 */
	public NavigationBar()
	{
		m_ModuleLinks = new HashMap<String, NavigationBarLink>();
		m_Modules = new HashMap<String, ModuleComponent>();
		
		HBoxLayout layout = new HBoxLayout(); 
		layout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);
		
		setLayout(layout);
		
		addStyleName("prl-navigationBar");
		
		// Add the company logo.
		add(new Image(ClientUtil.CLIENT_IMAGES.logo()), 
        		new HBoxLayoutData(new Margins(0, 20, 0, 0)));
  
		
		// Add a Log Out link.
		Anchor logoutAnchor = new Anchor("", true);
		logoutAnchor.setStyleName(ClientUtil.CLIENT_CONSTANTS.logoffLinkStylename());
		logoutAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
				fireEvent(GXTEvents.LogoutClick, new NavigationBarEvent(NavigationBar.this));
            }
        	
        });
		
		
        HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 5, 0, 0));   
        flex.setFlex(100);   
        add(new Text(), flex);   
        add(logoutAnchor, new HBoxLayoutData(new Margins(0, 5, 0, 0)));     
	}
	
	
	/**
	 * Adds the search box into the navigation bar.
	 */
	public void addSearchBox()
	{
		// TODO - ultimately this can be done in the constructor once Search
		// functionality is an integral part of the UI.
		m_SearchField = new TriggerField<String>();
		m_SearchField.setTriggerStyle("x-form-search-trigger");
		m_SearchField.setEmptyText("Search");
		m_SearchField.setWidth(200);
        
		m_SearchField.addListener(Events.TriggerClick, new Listener<FieldEvent>(){

			@Override
			public void handleEvent(FieldEvent be)
            {
				//setContainsText(m_SearchField.getValue());
				fireEvent(GXTEvents.RunSearchClick, 
						new NavigationBarEvent(NavigationBar.this));
            }
	    	
	    });
		m_SearchField.addListener(Events.KeyPress, new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent fe)
            {
	           	if (fe.getKeyCode() == KeyCodes.KEY_ENTER)
	           	{
	           		fireEvent(GXTEvents.RunSearchClick, 
							new NavigationBarEvent(NavigationBar.this));
	           	}
            }
	    	
	    });
        
        // Insert at penultimate position, to the left of the Log Out link.
		int indexToAdd = getItemCount() - 1;
        insert(m_SearchField, indexToAdd, new HBoxLayoutData(new Margins(0, 70, 0, 0)));
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
	 * Adds a link into the navigation bar for the specified UI module.
	 * @param module the module to create a link to.
	 */
	public void addModuleLink(ModuleComponent module)
	{
		NavigationBarLink moduleLink = new NavigationBarLink(module);
		m_ModuleLinks.put(module.getModuleId(), moduleLink);
		m_Modules.put(module.getModuleId(), module);
		insert(moduleLink, m_ModuleLinks.size());
	}
	
	
	/**
	 * Sets the module that should be marked as active, firing a ShowModuleClick event.
	 * @param id the ID of the module to mark as active, or <code>null</code> to 
	 * 	leave no modules marked as active e.g. when the Search page is displayed.
	 */
	public void setActiveModuleId(String id)
	{
		setActiveModuleId(id, true);
	}
	
	
	/**
	 * Sets the module that should be marked as active.
	 * @param id the ID of the module to mark as active, or <code>null</code> to 
	 * 	leave no modules marked as active e.g. when the Search page is displayed.
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
					fireEvent(GXTEvents.ShowModuleClick, new NavigationBarEvent(this, m_Modules.get(m_ActiveModuleId)));
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
	 * Inner class for the standard navigation bar links to UI modules.
	 */
	class NavigationBarLink extends Label
	{
		
		NavigationBarLink(ModuleComponent module)
		{
			final String finalId = module.getModuleId();
			setText(finalId);
			
			addStyleName("prl-navigationItem");
			
			addClickHandler(new ClickHandler(){

				@Override
	            public void onClick(ClickEvent ev)
	            {
					setActiveModuleId(finalId);
	            }
	        	
	        });
			
	        addMouseOverHandler(new MouseOverHandler(){

				@Override
	            public void onMouseOver(MouseOverEvent ev)
	            {
					if (finalId != getActiveModuleId())
					{
						addStyleName("prl-navigationOver");
					}
	            }
	        	
	        });
	        
	        addMouseOutHandler(new MouseOutHandler(){

				@Override
	            public void onMouseOut(MouseOutEvent ev)
	            {
					if (finalId != getActiveModuleId())
					{
						removeStyleName("prl-navigationOver");
					}
	            }
	        	
	        });
		}
	}
	
}
