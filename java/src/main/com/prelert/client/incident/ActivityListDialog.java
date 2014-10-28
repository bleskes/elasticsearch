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

package com.prelert.client.incident;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.SliderEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Slider;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.tips.QuickTip;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.google.gwt.user.client.Timer;

import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.IncidentGChart;
import com.prelert.client.list.ModelDatePagingToolBar;

import static com.prelert.data.PropertyNames.*;

import com.prelert.data.PropertyNames;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.IncidentModel;


/**
 * An extension of the Ext GWT Dialog for paging through the activities detected
 * by the Prelert engine, allowing the user to select an activity for analysis. 
 * The dialog consists of a grid listing activity data, with a toolbar above holding
 * controls for paging back and forth through the data and a slider for setting the
 * activity anomaly threshold. Below the grid are 'OK' and 'Cancel' buttons, with 
 * the 'OK' button enabled when the user selects an activity in the grid.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>Select</b> : SelectionEvent<IncidentModel>(source, model)<br>
 * <div>Fires when the 'OK' button has been pressed after selection of an activity.</div>
 * <ul>
 * <li>source : this</li>
 * <li>model: activity that has been selected in the dialog</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * @author Pete Harverson
 */
public class ActivityListDialog extends Dialog
{
	private ActivityPagingLoader 	m_Loader;
	
	private Slider					m_AnomalySlider;
	private Grid<IncidentModel>		m_Grid;
	
	
	/**
	 * Creates a new dialog for paging through Activity data.
	 */
	public ActivityListDialog()
	{
		setBodyBorder(false);
		setSize(850, 510);
		setHeading(ClientUtil.CLIENT_CONSTANTS.activitySelect());
		setLayout(new FitLayout());
		
		setButtons(Dialog.OKCANCEL); 
		setHideOnButtonClick(true); 
		
		Button okButton = getButtonById(Dialog.OK);
		okButton.setEnabled(false);	// Enabled on row selection.
		okButton.addSelectionListener(new SelectionListener<ButtonEvent>(){

			@Override
            public void componentSelected(ButtonEvent be)
            {
				fireActivitySelectionEvent();
            }
			
		});
		
		// Create the loader and paging toolbar.
		ActivityPagingRpcProxy proxy = new ActivityPagingRpcProxy();
		m_Loader = new ActivityPagingLoader(proxy);
		
		ModelDatePagingToolBar<IncidentModel> toolbar = 
			new ModelDatePagingToolBar<IncidentModel>(
				PropertyNames.TIME, PropertyNames.EVIDENCE_ID);
		
		// Add a slider for setting the Anomaly threshold.
		// Add a 0.5s timer delay on loading to prevent unnecessary incremental
		// loads as the slider is moved back and forth.
		final Timer sliderLoaderTimer = new Timer(){
			@Override
			public void run() 
			{
				m_Loader.setAnomalyThreshold(m_AnomalySlider.getValue());
				m_Loader.loadAtTime(m_Loader.getTime());
			}
	    };
		
		m_AnomalySlider = new Slider();
		m_AnomalySlider.setWidth(210);
		m_AnomalySlider.setIncrement(1);
		m_AnomalySlider.setMinValue(1);
		m_AnomalySlider.setMaxValue(100);
		m_AnomalySlider.setUseTip(false);
		m_AnomalySlider.addListener(Events.Change, new Listener<SliderEvent>()
		{
            @Override
            public void handleEvent(SliderEvent se)
            {
            	sliderLoaderTimer.schedule(500);
            }

		});

		LayoutContainer sliderCont = new LayoutContainer();   
		sliderCont.setWidth(210);
		sliderCont.addStyleName("prl-anomaly-slider");
		sliderCont.add(m_AnomalySlider);
		
		toolbar.add(new SeparatorToolItem());
		toolbar.add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.anomalyThreshold()));
		toolbar.add(sliderCont);
		
		toolbar.bind(m_Loader);
		setTopComponent(toolbar);
		
		m_Grid = createGrid();
		add(m_Grid);
	}
	
	
	/**
	 * Creates the grid for listing Activity data.
	 * @return the Activity list grid.
	 */
	protected Grid<IncidentModel> createGrid()
	{
		final ClientMessages messages = ClientUtil.CLIENT_CONSTANTS;
		
		ListStore<IncidentModel> listStore = new ListStore<IncidentModel>(m_Loader);
		
		// Set up the columns for the grid. Disable column sorting.
	    List<ColumnConfig> columnConfig = new ArrayList<ColumnConfig>();   
	    
	    // Render the anomaly score with a coloured image next to the value.
	    ColumnConfig anomalyScore = new ColumnConfig(SCORE, 
	    		messages.anomalyScore(), 90);
	    anomalyScore.setSortable(false);
	    anomalyScore.setRenderer(new GridCellRenderer<IncidentModel>(){

			@Override
            public Object render(IncidentModel activity, String property,
                    ColumnData config, int rowIndex, int colIndex,
                    ListStore<IncidentModel> store, Grid<IncidentModel> grid)
            {
				int anomalyScore = activity.getAnomalyScore();
				String symbolURL = IncidentGChart.getSymbolImageURL(anomalyScore);
				String altText = ClientUtil.CLIENT_CONSTANTS.anomalyScore() + "=" + anomalyScore;
				return "<img class=\"prl-anomaly\" src=\"" + symbolURL + "\" " +
					"alt=\"" + altText + "\" />&nbsp;" + anomalyScore;
            }
	    	
	    });
	  
	    ColumnConfig time = new ColumnConfig(TIME, messages.time(), 130);
	    time.setDateTimeFormat(ClientUtil.getDateTimeFormat(TimeFrame.SECOND));  
	    time.setSortable(false);
	    
	    // Use a custom GridCellRenderer to display tooltips on the description column.
	    ColumnConfig description = new ColumnConfig(DESCRIPTION, 
	    		messages.description(), 550);
	    description.setSortable(false);
	    description.setRenderer(new GridCellRenderer<IncidentModel>() {

			@Override
            public Object render(IncidentModel incident, String property,
                    ColumnData config, int rowIndex, int colIndex,
                    ListStore<IncidentModel> store, Grid<IncidentModel> grid)
            {
				String desc = incident.getDescription();
					
				String text = "<span qtip=\"";
				text += desc;
				text += "\" qtitle=\"";
				text += messages.description();
				text += "\" >";
				text += desc;
				text += "</span>";
				
				return text;
            }
	    });
	    
	    columnConfig.add(time);
	    columnConfig.add(anomalyScore);
	    columnConfig.add(description);
	  
	    ColumnModel columnModel = new ColumnModel(columnConfig);   
	  
	    final Grid<IncidentModel> grid = new Grid<IncidentModel>(listStore, columnModel);   
	    grid.setAutoExpandColumn(DESCRIPTION);   
	    grid.setLoadMask(true);
	    grid.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
	    grid.getSelectionModel().addSelectionChangedListener(
	    		new SelectionChangedListener<IncidentModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<IncidentModel> se)
            {
				IncidentModel selected = se.getSelectedItem();				
				getButtonById(Dialog.OK).setEnabled(selected != null);
            }
	    });
	    
	    // Double-click on a row to trigger a SelectionEvent.
	    grid.addListener(Events.RowDoubleClick, new Listener<GridEvent<IncidentModel>>(){

			@Override
            public void handleEvent(GridEvent<IncidentModel> be)
            {
				fireActivitySelectionEvent();
				hide();
            }
	    	
	    });
	    
	    // This reference to QuickTip is needed to enable the tooltips on the description column.
	    @SuppressWarnings("unused")
	    QuickTip qtip = new QuickTip(grid);

	    return grid;
	}
	
	
	/**
	 * Loads the list of activities for the currently selected anomaly threshold, 
	 * displaying the first (most recent) page of data.
	 */
	public void load()
	{
		m_Loader.load();
	}
	
	
	/**
	 * Loads a page of activity data, whose top row will be closest in time, 
	 * up to or before, the specified time.
	 * @param time
	 */
	public void loadAtTime(Date time)
	{
		m_Loader.loadAtTime(time);
	}
	
	
	/**
	 * Fires a SelectionEvent to indicate an activity has been for analysis by the user.
	 */
	protected void fireActivitySelectionEvent()
	{
		GridSelectionModel<IncidentModel> selectionModel = m_Grid.getSelectionModel();
        IncidentModel selected = selectionModel.getSelectedItem();
        if (selected != null)
        {
        	SelectionEvent<IncidentModel> se = 
        		new SelectionEvent<IncidentModel>(this, selected);
        	fireEvent(Events.Select, se);
        }
	}
}
