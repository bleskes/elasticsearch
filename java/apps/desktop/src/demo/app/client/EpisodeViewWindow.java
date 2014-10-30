/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package demo.app.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.menu.Menu;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import demo.app.client.chart.CausalityEpisodeChart;
import demo.app.client.event.ViewMenuItemListener;
import demo.app.data.CausalityEpisodeLayoutData;
import demo.app.data.CausalityView;
import demo.app.data.HistoryViewTool;
import demo.app.data.ListViewTool;
import demo.app.data.TimeFrame;
import demo.app.data.Tool;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;


/**
 * An extension of the Causality View Window for displaying causally related
 * notifications in the form of probable cause 'episodes'.
 * Each episode will consist of one or more evidence items, with an information
 * panel below to display details on the selected item.
 * 
 * @author Pete Harverson
 */
public class EpisodeViewWindow extends CausalityViewWindow
{
	private DesktopApp					m_Desktop;
	
	private CausalityEpisodeChart		m_EpisodeChart;
	private Grid<GridRowInfo> 			m_Grid;
	
	
	/**
	 * Creates a new window for displaying causally related
	 * notifications in the form of probable cause 'episodes'.
	 */
	public EpisodeViewWindow(DesktopApp desktop, CausalityView causalityView)
	{
		super(causalityView);
		m_Desktop = desktop;
		
		setSize(600, 590);
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		setLayout(new BorderLayout());
		
		ContentPanel chartPanel = new ContentPanel();
		chartPanel.setHeaderVisible(false);
	    
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(5, 0, 5, 0));   
	  
	    BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 225);   
	    northData.setSplit(true);    
	    northData.setMargins(new Margins(5));   
	    
	    // Create the Episode chart.
	    // On selection events, update the evidence in the Info Grid.
	    m_EpisodeChart = new CausalityEpisodeChart(490, 160);
	    m_EpisodeChart.setSymbolSize(10);
	    m_EpisodeChart.addClickHandler(new ClickHandler(){

            public void onClick(ClickEvent e)
            {
            	int episodeId = m_EpisodeChart.getTouchedPointEpisodeId();
    			EvidenceModel evidence = m_EpisodeChart.getTouchedPointEvidence();
    			if (evidence != null)
    			{
    				setInfoEvidenceId(episodeId, evidence.getId());
    			}
            }
			
		});
	    
	    // Configure the right-click context menu on the chart.
	    Menu menu = new Menu();
	    List<Tool> viewMenuItems = m_CausalityView.getContextMenuItems();
	    if (viewMenuItems != null)
	    {
	    	ViewMenuItem menuItem;
	    	ViewMenuItemListener<MenuEvent> menuItemLsnr;
	    	
	    	for (int i = 0; i < viewMenuItems.size(); i++)
	    	{
	    	    menuItem = new ViewMenuItem(viewMenuItems.get(i));
	    	    menuItemLsnr = new ViewMenuItemListener<MenuEvent>(menuItem, this);
	    	    menuItem.addSelectionListener(menuItemLsnr);
	    	    menu.add(menuItem);
	    	}
	    }
	    
	    if (menu.getItemCount() > 0)
	    {
	    	m_EpisodeChart.setContextMenu(menu);
	    }
	    
	    chartPanel.add(m_EpisodeChart);
	    
	    // Create the Info grid.
	    ColumnConfig propColumn = new ColumnConfig("columnName", "Property", 150);
	    ColumnConfig valueColumn = new ColumnConfig("columnValue", "Value", 250);
	    valueColumn.setRenderer(new ShowInfoValueCellRenderer());

	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(propColumn);
	    config.add(valueColumn);

	    ColumnModel columnModel = new ColumnModel(config);
		
	    m_Grid = new Grid<GridRowInfo>(new ListStore<GridRowInfo>(), columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.setAutoExpandColumn("columnValue");
	   
	    add(chartPanel, northData);
	    add(m_Grid, centerData);      
	     
	}

    
    /**
     * Sets the id of the item of evidence from an episode whose details are to 
     * be displayed in the information panel.
     * @param episodeId id of the episode containing the evidence whose details
     * 	are to be displayed.
     * @param evidenceId the id of the evidence whose details are to be displayed.
     */
    public void setInfoEvidenceId(int episodeId, int evidenceId)
    {
    	m_CausalityQueryService.getEvidenceInfo(episodeId, evidenceId, 
    			new ApplicationResponseHandler<List<GridRowInfo>>(){

	        public void uponFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Error retrieving evidence info.", null);
	        }


	        public void uponSuccess(List<GridRowInfo> modelData)
	        { 	
	        	m_Grid.getStore().removeAll();
	        	m_Grid.getStore().add(modelData);
	        }
		});
    }

    
    /**
     * Reloads the data in the window to display the probable cause episodes
     * for the currently set item of evidence.
     */
	@Override
    public void load()
    {	
		final EvidenceModel evidence = getEvidence();
		
		if (evidence != null)
		{
			// Load the episodes and their layout positions for the chart.
			ApplicationResponseHandler<List<CausalityEpisodeLayoutData>> callback = 
				new ApplicationResponseHandler<List<CausalityEpisodeLayoutData>>()
			{
				public void uponFailure(Throwable caught)
				{
					MessageBox.alert("Prelert - Error",
			                "Error retrieving causality data from server.", null);
				}
	
	
				public void uponSuccess(List<CausalityEpisodeLayoutData> episodeLayoutData)
				{	
					// Load the data in the Info Grid for this item of evidence 
					// in the first (most probable) episode.
					CausalityEpisodeLayoutData topEpisodeData = episodeLayoutData.get(0);
					setInfoEvidenceId(topEpisodeData.getEpisode().getId(), evidence.getId());
					
					Date startTime = new Date();
					m_EpisodeChart.setEpisodeLayoutData(episodeLayoutData, evidence);
					
					Date endTime = new Date();
					//Info.display("Causality Chart", "Time to layout chart data: " + (startTime.getTime() - endTime.getTime() + "ms"));
					
					m_EpisodeChart.update();	
					
					endTime = new Date();
					//Info.display("Causality Chart", "Time to update chart data: " + (startTime.getTime() - endTime.getTime() + "ms"));
					
				}
			};
			m_CausalityQueryService.getEpisodeLayoutData(evidence.getId(), m_EpisodeChart.getXChartSize(),
					m_EpisodeChart.getYChartSize(), m_EpisodeChart.getSymbolSize(), 10, 5, callback);

		}
		else
		{
			m_Grid.getStore().removeAll();
			m_EpisodeChart.clearChart();
			m_EpisodeChart.update();
		}
    }
	
	
	/**
	 * Runs a tool against the selected row of evidence. No action is taken if
	 * no row is currently selected.
	 * @param tool the tool that has been run by the user.
	 */
	@Override
    public void runTool(Tool tool)
	{
		// Note that GWT does not support reflection, so need to use getClass().
		if (tool.getClass() == ListViewTool.class)
		{
			runListViewTool((ListViewTool)tool);
		}
		else if (tool.getClass() == HistoryViewTool.class)
		{
			runHistoryViewTool((HistoryViewTool)tool);
		}
	}
	
	
	/**
	 * Runs a tool against the hovered over usage record to open a List View.
	 * Currently only the opening of the Evidence View is supported, showing
	 * evidence at the time of the selected record.
	 * @param tool the ViewTool that has been run by the user.
	 */
	public void runListViewTool(ListViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EvidenceModel selectedEvidence = m_EpisodeChart.getTouchedPointEvidence();
		if (selectedEvidence != null)
		{
			String dataType = selectedEvidence.getDataType();
			if (dataType != null)
			{
				m_Desktop.openEvidenceWindowForType(dataType, selectedEvidence.getId());
			}
			else
			{
				MessageBox.alert(
						"Prelert - Error", "Data type of evidence record is unknown.", null);
			}
		}
	}
	
	
	/**
	 * Runs a tool against the selected item of evidence to launch the History View.
	 * No action is taken if no row is currently selected.
	 * @param tool the HistoryViewTool that has been run by the user.
	 */
	public void runHistoryViewTool(HistoryViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EvidenceModel selectedEvidence = m_EpisodeChart.getTouchedPointEvidence();
		if (selectedEvidence != null)
		{
			Date date = ClientUtil.parseTimeField(selectedEvidence, TimeFrame.SECOND);
			
			String dataType = selectedEvidence.getDataType();
			if (dataType != null)
			{
				m_Desktop.openHistoryWindow(dataType, tool.getTimeFrame(), 
						selectedEvidence.getDescription(), date);
			}
			else
			{
				MessageBox.alert(
						"Prelert - Error", "Data type of evidence record is unknown.", null);
			}
			
		}
	}
	
	
	/**
	 * Custom GridCellRenderer to show tooltips on the value column cells.
	 */
	class ShowInfoValueCellRenderer implements GridCellRenderer<GridRowInfo>
	{
		
		public String render(GridRowInfo rowInfo, String property, ColumnData config,
				int rowIndex, int colIndex, ListStore<GridRowInfo> store, Grid<GridRowInfo> grid)
		{
			String text = "";
			
			if (rowInfo.get(property) != null)
			{
				String columnVal = rowInfo.getColumnValue();
				String tooltip = rowInfo.getColumnValue();
				
				text += "<span alt=\"";
				text += tooltip;
				text += "\" title=\"";
				text += tooltip;
				text += "\">";
				text += columnVal;
				text += "</span>";
			}
			
			return text;
		}
		
	}
    
    
}
