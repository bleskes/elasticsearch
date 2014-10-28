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

package com.prelert.client.gxt;

import java.util.HashMap;

import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Frame;

import com.prelert.client.ClientUtil;


/**
 * GXT <code>ContentPanel</code> for displaying help pages for the modules in 
 * the Prelert UI. The panel should be passed a map of the help page URLs by
 * module ID via the {@link #setPagesByModule(HashMap)} method.
 * @author Pete Harverson
 */
public class ModuleHelpPanel extends ContentPanel
{
	private String 	m_ModuleId;
	private HashMap<String, String>	m_PagesInView;
	private Frame	m_ContentFrame;
	
	
	/**
	 * Creates a new panel for displaying HTML help pages.
	 */
	public ModuleHelpPanel()
	{
		setHeading(ClientUtil.CLIENT_CONSTANTS.help());
		
		m_PagesInView = new HashMap<String, String>();
	}
	
	
	/**
	 * Sets the map of help page URL against module ID.
	 * @param urlsByModule map of HTML help page URL against module ID.
	 */
	public void setPagesByModule(HashMap<String, String> urlsByModule)
	{
		m_PagesInView = urlsByModule;
	}
	
	
	/**
	 * Sets the ID of the currently active (visible) module in the UI for
	 * which the relevant help page should be displayed in the Help panel.
	 * Content of the frame is left unchanged if there is no help page for
	 * the specified module.
	 * @param moduleId the ID of the {@link ModuleComponent} that should be made active.
	 */
	public void setActiveModule(String moduleId)
	{	
		m_ModuleId = moduleId;
		String pageURL = m_PagesInView.get(m_ModuleId);
		if (pageURL != null)
		{
			m_ContentFrame = setUrl(pageURL);
			m_ContentFrame.addLoadHandler(new LoadHandler() {      
				
		        @Override
		        public void onLoad(LoadEvent event) 
		        {
		        	// Remember the URL of the page the user has navigated to for the current module.
		        	String frameURL = getIFrameURL(IFrameElement.as(m_ContentFrame.getElement()));
					GWT.log("URL of help frame for module " + m_ModuleId + ": " + frameURL);
					m_PagesInView.put(m_ModuleId, frameURL);
		        }
		    });
		}
	}
	
	
	/**
	 * Returns the URL of the given IFrame element.
	 * @param iframe IFrame for which to return the URL.
	 * @return the URL of the IFrame, or <code>null</code> if the document of the
	 * 	contentWindow genenerated by the IFrame is undefined.
	 */
	protected static native String getIFrameURL(IFrameElement iframe) /*-{
		if (iframe.contentDocument !== undefined) 
		{
			if (iframe.contentDocument.defaultView !== undefined
				&& iframe.contentDocument.defaultView.location !== undefined) 
			{
				return iframe.contentDocument.defaultView.location.href;
			} 
			else 
			{
				return iframe.contentDocument.URL;
			}
		} 
		else if (iframe.contentWindow !== undefined
			&& iframe.contentWindow.document !== undefined) 
		{
			return iframe.contentWindow.document.URL;
		} 
		else 
		{
			return null;
		}
	}-*/;
}
