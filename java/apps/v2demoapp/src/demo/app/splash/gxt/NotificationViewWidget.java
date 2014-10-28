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

package demo.app.splash.gxt;

import java.util.Date;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import demo.app.client.SourceViewWidget;
import demo.app.client.event.GXTEvents;
import demo.app.client.event.RequestViewEvent;
import demo.app.data.EvidenceView;
import demo.app.data.Tool;
import demo.app.data.View;
import demo.app.data.gxt.EvidenceModel;


/**
 * ViewWidget implementation for displaying a notification view. 
 * The widget contains a grid component for paging through a list of records 
 * retrieved from the database, with a toolbar that allows the users to page by 
 * date/time and set a simple filter on the list of records displayed 
 * e.g. description='rrcp congestion'.
 * 
 * @author Pete Harverson
 */
public class NotificationViewWidget extends VerticalPanel implements SourceViewWidget
{
	private EvidenceViewPanel	m_EvidencePanel;
	private String				m_Source;
	
	
	/**
	 * Creates a new widget for displaying a view of notification data.
	 * @param evidenceView the view to display in the widget.
	 */
	public NotificationViewWidget(EvidenceView evidenceView)
	{
		m_EvidencePanel = new EvidenceViewPanel(evidenceView, Style.LayoutRegion.NORTH);
		
		// Listen for events to open notification, time series and causality views.
		Listener<RequestViewEvent<EvidenceModel>> rveListener = new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	// Propagate the event.
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_EvidencePanel.addListener(GXTEvents.OpenNotificationViewClick, rveListener);
		m_EvidencePanel.addListener(GXTEvents.OpenTimeSeriesViewClick, rveListener);
		m_EvidencePanel.addListener(GXTEvents.OpenCausalityViewClick, rveListener);
		
		add(m_EvidencePanel);
	}


	/**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Widget which is added in the user interface.
	 */
	public Widget getWidget()
	{
		return this;
	}
	
	
	/**
	 * Returns the View displayed in the Widget.
	 * @return the view displayed in the Widget.
	 */
	public View getView()
	{
		return m_EvidencePanel.getView();
	}
	
	
	/**
	 * Returns the name of the source (server) whose data will be viewed in the
	 * widget.
	 * @return the name of the source whose data is currently being viewed,
	 * or <code>null</code> if no specific source is currently selected
	 * i.e. 'All Sources' is selected.
	 */
	public String getSource()
	{
		return m_Source;
	}
	
	
	/**
	 * Sets the name of the source (server) whose data will be viewed in the widget.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 */
	public void setSource(String source)
	{
		m_Source = source;
		m_EvidencePanel.setSource(source);
	}
	
	
	/**
	 * Sets the filter for the evidence data. A separate call should be made to
	 * reload data into the window following the call to this method.
	 * @param filterAttribute 	the name of the attribute on which the evidence
	 * 	should be filtered.	
	 * @param filterValue		the value of the filter on which the evidence
	 * 	should be filtered.
	 */
	public void setFilter(String filterAttribute, String filterValue)
	{
		m_EvidencePanel.setFilter(filterAttribute, filterValue);
	}
	
	
	/**
	 * Overrides setWidth(int) to set the width of the evidence grid to the 
	 * specified size. This method fires the <i>Resize</i> event.
	 * @param width the new width to set.
	 */
	@Override
    public void setWidth(int width)
	{
		super.setWidth(width);
		m_EvidencePanel.setWidth(width);
	}


	/**
	 * Loads the data in the widget according to its current configuration.
	 */
	public void load()
	{
		m_EvidencePanel.load();
	}
	
	
	/**
	 * Loads the data in the widget so that the top of row of evidence will match  
	 * the specified time.
	 * @param date date/time of evidence data to load.
	 */
	public void loadAtTime(Date date)
	{
		m_EvidencePanel.loadAtTime(date);
	}
	
	
	/**
	 * Loads the data in the widget so that the top of row of evidence will match 
	 * the specified id.
	 * @param evidenceId id for the top row of evidence data to be loaded.
	 */
    public void loadAtId(int evidenceId)
    {
    	// Clear any filter, and reload for this id.
    	m_EvidencePanel.setFilter(null, null);
    	m_EvidencePanel.loadAtId(evidenceId);
    }
	
	
	/**
	 * Runs a tool against the selected notification. No action is taken if
	 * no notification is currently selected.
	 * @param tool the tool that has been run by the user.
	 */
	public void runTool(Tool tool)
	{
		// No longer used. Needs to be removed from ViewWidget interface.
	}
	
}
