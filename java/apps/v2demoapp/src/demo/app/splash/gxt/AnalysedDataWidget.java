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

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.google.gwt.core.client.GWT;

import demo.app.client.ApplicationResponseHandler;
import demo.app.data.gxt.DataSourceTreeModel;
import demo.app.splash.service.DataSourceQueryServiceAsync;
import demo.app.splash.service.QueryServiceLocator;


/**
 * Ext GWT (GXT) widget for displaying a summary of the data analysed by the 
 * Prelert engine. The widget consists of a table listing the names, types and
 * count of data analysed.
 * @author Pete Harverson
 */
public class AnalysedDataWidget extends LayoutContainer
{
	private DataSourceQueryServiceAsync 	m_DataSourceQueryService;
	
	private ListStore<DataSourceTreeModel>	m_ListStore;
	
	
	public AnalysedDataWidget()
	{
		m_DataSourceQueryService = QueryServiceLocator.getInstance().getDataSourceQueryService();
		
	    setLayout(new FlowLayout(10));     
	  
	    ArrayList<ColumnConfig> configs = new ArrayList<ColumnConfig>();   
	  
	    ColumnConfig column = new ColumnConfig();   
	    column.setId("dataSourceName");   
	    column.setHeader("Data source");   
	    column.setWidth(225);   
	    configs.add(column);   
	  
	    column = new ColumnConfig();   
	    column.setId("dataSourceCategory");   
	    column.setHeader("Category");   
	    column.setWidth(100);   
	    configs.add(column);   
	  
	    column = new ColumnConfig();   
	    column.setId("count");   
	    column.setHeader("Count");   
	    column.setWidth(75);   
	    configs.add(column);   
	  
	  
	    m_ListStore = new ListStore<DataSourceTreeModel>();  
	  
	    ColumnModel cm = new ColumnModel(configs);   
	  
	    ContentPanel cp = new ContentPanel();   
	    cp.setHeaderVisible(false); 
	    cp.setLayout(new FitLayout());   
	    cp.setSize(400, 300);   
	  
	    Grid<DataSourceTreeModel> grid = new Grid<DataSourceTreeModel>(m_ListStore, cm);   
	    grid.setStyleAttribute("borderTop", "none");   
	    grid.setAutoExpandColumn("dataSourceName");   
	    grid.setStripeRows(true);   
	    cp.add(grid);   
	  
	    add(cp);
	}
	
	
	/**
	 * Loads the summary of analysed data into the widget.
	 */
	public void load()
	{
		ApplicationResponseHandler<List<DataSourceTreeModel>> callback = 
			new ApplicationResponseHandler<List<DataSourceTreeModel>>()
		{
			public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading summary of analysed data", caught);
				MessageBox.alert("Prelert - Error", "Error loading analysed data.", null);
			}


			public void uponSuccess(List<DataSourceTreeModel> models)
			{
				m_ListStore.add(models);
			}
		};

		m_ListStore.removeAll();
		
		m_DataSourceQueryService.getDataSourceTypeCounts(callback);
	}
}
