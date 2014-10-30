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

package com.prelert.client.explorer;

import static com.prelert.data.PropertyNames.*;

import java.util.ArrayList;
import java.util.Date;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.BorderLayoutEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.LayoutEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.CollapsePanel;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;

import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.list.EvidenceViewPanel;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.gxt.EvidenceModel;


/**
 * A GXT panel which displays a view of time series data. The panel consists of 
 * a  {@link TimeSeriesChartPanel} charting component, and a collapsible 
 * {@link EvidenceViewPanel} below listing features in the time series.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to the Analysis View is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: time series feature whose causality data is being requested</li>
 * </ul>
 * </dd>
 * 
 * @author Pete Harverson
 */
public class TimeSeriesViewPanel extends ContentPanel
{
	private TimeSeriesChartPanel	m_TimeSeriesPanel;
	private EvidenceViewPanel		m_FeaturesList;
	
	private BorderLayout 			m_BorderLayout;
	
	
	/**
	 * Creates a new panel for displaying time series data.
	 */
	public TimeSeriesViewPanel()
	{
		m_BorderLayout = new BorderLayout();   
		m_BorderLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(m_BorderLayout); 
	    setHeaderVisible(false);
	    setBodyBorder(false);
	    
	    // Create the charting component.
	    m_TimeSeriesPanel = new TimeSeriesChartPanel();
	    m_TimeSeriesPanel.setCollapsible(true);
				
	    // Create the features list, which is collapsed by default.
	    m_FeaturesList = new EvidenceViewPanel();
	    m_FeaturesList.setHeaderVisible(true);
	    m_FeaturesList.getSelectionModel().addSelectionChangedListener(
     			new SelectionChangedListener<EvidenceModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<EvidenceModel> se)
            {
				EvidenceModel selectedRow = se.getSelectedItem();
				if (selectedRow != null)
				{
					m_TimeSeriesPanel.loadFeature(selectedRow);
				}  
            }
     		
     	});
	    
	    // Listen for events to open causality views.
	    Listener<RequestViewEvent<EvidenceModel>> openCausalityListener =
	    	new Listener<RequestViewEvent<EvidenceModel>>(){

            @Override
            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	// Propagate the event.
        		fireEvent(rve.getType(), rve);
            }
			
		}; 
	    
		m_TimeSeriesPanel.addListener(GXTEvents.OpenCausalityViewClick, 
				openCausalityListener);
		m_FeaturesList.addListener(GXTEvents.OpenCausalityViewClick, 
				openCausalityListener);
		
	    
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));
	    
	    BorderLayoutData southData = new BorderLayoutData(LayoutRegion.SOUTH, 280, 30, 525); 
	    southData.setSplit(true);
	    southData.setCollapsible(true); 
	    southData.setFloatable(false);  
	    southData.setMargins(new Margins(5, 0, 0, 0)); 
	    
	    m_BorderLayout.addListener(Events.AfterLayout, new Listener<LayoutEvent>(){

			@Override
            public void handleEvent(LayoutEvent be)
            {	
				m_BorderLayout.collapse(LayoutRegion.SOUTH);
				m_BorderLayout.removeListener(Events.AfterLayout, this);
            }
			
		});
	    
	    m_BorderLayout.addListener(Events.Expand, new Listener<BorderLayoutEvent>(){
	    	 
 			@Override
             public void handleEvent(BorderLayoutEvent be)
             {
 				// Load feature list - this also re-renders the paging toolbar
 				// which may not render properly on expand in Safari and Chrome.
 				m_FeaturesList.refreshGridView();
 				m_FeaturesList.load();
             }
 			
 		});
	    
        add(m_TimeSeriesPanel, centerData);   
        add(m_FeaturesList, southData);     
	}
	
	
	/**
	 * Sets the time series for viewing in the panel.
	 * @param config configuration of time series to display.
	 */
	public void setTimeSeriesConfig(TimeSeriesConfig config)
	{
		m_TimeSeriesPanel.setTimeSeriesConfig(config);
		
		// Reconfigure the features list.
		ArrayList<Attribute> filter = new ArrayList<Attribute>();
		filter.add(new Attribute(METRIC, config.getMetric()));
		if (config.getAttributes() != null)
		{
			filter.addAll(config.getAttributes());
		}
		if (config.getTimeSeriesId() != 0)
		{
			// Add the time_series_id to the filter to ensure the feature list only
			// contains features in this particular series, rather than all features
			// which match to this particular level in the metric path.
			filter.add(new Attribute(TIME_SERIES_ID, Integer.toString(config.getTimeSeriesId())));
		}
		
		m_FeaturesList.reconfigure(
				new DataSourceType(config.getDataType(), DataSourceCategory.TIME_SERIES_FEATURE), 
				config.getSource(), filter);
		
		// Update the heading in the feature list, AND the corresponding CollapsePanel.
		String featuresTitle = 
			ClientUtil.CLIENT_CONSTANTS.timeSeriesFeaturesHeading(config.getMetric());
		m_FeaturesList.setHeading(featuresTitle);
		CollapsePanel cp = (CollapsePanel)(m_FeaturesList.getData("collapse"));
		if (cp != null)
		{
			cp.el().selectNode(".x-panel-header-text").update(featuresTitle);
		}
	}
	
	
	/**
     * Sets the text for the chart title label.
     * @param text new title label text
     */
	public void setChartTitleText(String text)
	{
		m_TimeSeriesPanel.setTitleLabelText(text);
	}


	/**
     * Sets the text for the chart subtitle label.
     * @param text new subtitle label text.
     */
	public void setChartSubtitleText(String text)
	{
		m_TimeSeriesPanel.setSubtitleLabelText(text);
	}
	
	
	/**
	 * Sets the load time for the chart and features list.
	 * @param time date/time to use for the next load operation. The load
	 * 	time span for the chart will be 30 minutes either side of this time.
	 */
	public void setLoadTime(Date time)
	{
		m_TimeSeriesPanel.setLoadTime(time);
		m_FeaturesList.getLoader().setTime(time);
	}
	
	
	/**
	 * Loads the chart and features list for the current configuration and load time.
	 */
	public void load()
	{
		m_TimeSeriesPanel.load();
		
		if ( (m_FeaturesList.isVisible() == true) && 
				(m_FeaturesList.isExpanded() == true) )
		{
			m_FeaturesList.load();
		}
	}
	
	
	/**
	 * Sets whether the features list should be shown or hidden.
	 * @param showFeatures <code>true</code> to show the features list below the 
	 * 	chart, <code>false</code> otherwise.
	 */
	public void setShowFeaturesList(boolean showFeatures)
	{
		if (showFeatures == true)
		{
			m_BorderLayout.show(LayoutRegion.SOUTH);
		}
		else
		{
			m_BorderLayout.hide(LayoutRegion.SOUTH);
		}
	}
}
