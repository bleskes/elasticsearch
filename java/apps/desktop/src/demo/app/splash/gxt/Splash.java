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

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.TabPanelEvent;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChartCanvasFactory;
import com.googlecode.gchart.client.GChartCanvasLite;

import demo.app.client.ApplicationResponseHandler;
import demo.app.client.ClientUtil;
import demo.app.data.DataSourceType;
import demo.app.data.gxt.DataSourceTreeModel;
import demo.app.splash.service.QueryServiceLocator;


/**
 * Entry point for Splash UI.
 */
public class Splash implements EntryPoint
{

	private DataSourceTree 		m_DataSourceTree;
	private WorkAreaTabPanel	m_WorkAreaTabPanel;
	
	
	// Static block to tell GChart to use the the GWTCanvas widget to render 
	// any continuous, non-rectangular, chart aspects (solid fill pie slices,
    // continuously connected lines, etc.)
	static 
	{
		final class GWTCanvasBasedCanvasLite extends GWTCanvas implements GChartCanvasLite
		{
			// GChartCanvasLite requires CSS/RGBA color strings, but
			// GWTCanvas uses its own Color class instead, so we wrap:
			public void setStrokeStyle(String cssColor)
			{
				// Sharp angles of default MITER can overwrite adjacent pie slices.
				setLineJoin(GWTCanvas.ROUND);
				setStrokeStyle(new Color(cssColor));
			}


			public void setFillStyle(String cssColor)
			{
				setFillStyle(new Color(cssColor));
			}
		}
		
		final class GWTCanvasBasedCanvasFactory implements GChartCanvasFactory
		{
			public GChartCanvasLite create()
			{
				GChartCanvasLite result = new GWTCanvasBasedCanvasLite();
				return result;
			}
		}

		GChart.setCanvasFactory(new GWTCanvasBasedCanvasFactory());
	} 
	
	
	public void onModuleLoad()
	{
		// Define the JSNI bridge methods.
		defineBridgeMethod();
		defineBridgeMethod2();

		// Create the graphical components.
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		GXT.useShims = true;
		
		Viewport container = new Viewport();
		
		BorderLayout containerLayout = new BorderLayout();
		containerLayout.setContainerStyle("prl-viewport");
		
		container.setLayout(containerLayout);
		
		// Create components for the top panel - logo and logout button.
		LayoutContainer topPanel = createTopPanel();
		
		// Create components for the Data sources tree.
		LayoutContainer dataSourcePanel = createDataSourcePanel();
		
		// Create the main tabbed work area.
		LayoutContainer workAreaPanel = createWorkAreaPanel();
		
		// Add a listener to the data source tree to switch to the appropriate
		// tab in the work area on selection.
		final SelectionChangedListener<DataSourceTreeModel> treeSelectionListener = 
			new SelectionChangedListener<DataSourceTreeModel>(){

            @Override
            public void selectionChanged(
                    SelectionChangedEvent<DataSourceTreeModel> se)
            {
            	DataSourceTreeModel selectedDataSource = se.getSelectedItem();
            	GWT.log("Splash treeSelectionListener selectedDataSource: " + selectedDataSource, null);
            	
            	if (selectedDataSource != null)
            	{
            		DataSourceType dsType = selectedDataSource.getDataSourceType(); 
	            	String source = selectedDataSource.getSource();
	            	GWT.log("Splash treeSelectionListener showDataTab() for " + dsType, null);
	            	m_WorkAreaTabPanel.showDataTab(dsType, source);
            	}
            }
			
		};

		
		m_DataSourceTree.getSelectionModel().addSelectionChangedListener(treeSelectionListener);
		m_DataSourceTree.setTreeWidth(248);
		
		// Add a listener to the work area tab panel to select the corresponding
		// node in the data sources tree.
		m_WorkAreaTabPanel.addListener(Events.Select, new Listener<TabPanelEvent>(){

            public void handleEvent(TabPanelEvent be)
            {
            	final DataSourceType selectedType = m_WorkAreaTabPanel.getSelectedDataSourceType();
            	if (selectedType != null)
            	{
	            	final String selectedSource = m_WorkAreaTabPanel.getSelectedSource();
		            GWT.log("Splash tab panel selection listener, tab selected: " + 
		            		selectedType + ", " + selectedSource, null);
		            
		            boolean loadedInTree = (m_DataSourceTree.findModel(selectedType, selectedSource) != null);
		            if (loadedInTree == true)
		            {
		            	m_DataSourceTree.getSelectionModel().removeSelectionListener(treeSelectionListener);
			            m_DataSourceTree.selectModel(selectedType, selectedSource);
			            m_DataSourceTree.getSelectionModel().addSelectionChangedListener(treeSelectionListener);
		            }
		            else
		            {
		            	m_DataSourceTree.getTreeStore().addStoreListener(new StoreListener<DataSourceTreeModel>(){

		                    public void storeDataChanged(
		                    		StoreEvent<DataSourceTreeModel> se)
		                    {
		                    	m_DataSourceTree.getSelectionModel().removeSelectionListener(treeSelectionListener);
					            m_DataSourceTree.selectModel(selectedType, selectedSource);
					            m_DataSourceTree.getSelectionModel().addSelectionChangedListener(treeSelectionListener);
		                    	m_DataSourceTree.removeListener(TreeStore.DataChanged, this);
		                    }	
						});          
		            	
			            m_DataSourceTree.expandDataSource(selectedType);
			            
		            }
 
            	}
            }
			
		});
		
        BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 50);     
        northData.setFloatable(false);   
        northData.setHideCollapseTool(true);   
        northData.setSplit(false);   
        northData.setMargins(new Margins(10, 10, 10, 10));  
        
        BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 270);   
        westData.setCollapsible(true);   
        westData.setMargins(new Margins(0, 10, 10, 10));   
        
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
        centerData.setMargins(new Margins(0, 10, 10, 0));
		  
        container.add(topPanel, northData);    
        container.add(dataSourcePanel, westData);    
        container.add(workAreaPanel, centerData);
        
        RootPanel.get("prelertGUI").add(container);
	}
	
	
	/**
	 * Creates the top panel which holds the Prelert logo and Log Off link.
	 * @return the top panel.
	 */
	protected LayoutContainer createTopPanel()
	{
		HBoxLayout layout = new HBoxLayout(); 
		layout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);
		
		LayoutContainer topPanel = new LayoutContainer(layout);
  
		Anchor logoutAnchor = new Anchor(" ", true);
		logoutAnchor.setStyleName(ClientUtil.CLIENT_CONSTANTS.logoffLinkStylename());
		logoutAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
				logout();
            }
        	
        });
		
		topPanel.add(IconHelper.create("images/logo.png", 194, 46).createImage());   
        HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 5, 0, 0));   
        flex.setFlex(1);   
        topPanel.add(new Text(), flex);   
        topPanel.add(logoutAnchor, new HBoxLayoutData(new Margins(0, 5, 0, 0)));     
		
		return topPanel;
	}
	
	
	/**
	 * Creates the panel holding the data sources tree.
	 * @return data sources panel.
	 */
	protected LayoutContainer createDataSourcePanel()
	{	
		VerticalPanel dataSourcePanel = new VerticalPanel();
		dataSourcePanel.setScrollMode(Style.Scroll.AUTO);
		
		dataSourcePanel.addStyleName("prl-dataSourcePanel");
		
		Label titleLabel = new Label(ClientUtil.CLIENT_CONSTANTS.analysedData());
		titleLabel.addStyleName("prl-headerLabels");
		
		m_DataSourceTree = new DataSourceTree();
		
		dataSourcePanel.add(titleLabel);
		dataSourcePanel.add(m_DataSourceTree);
		
		return dataSourcePanel;
	}
	
	
	/**
	 * Creates the panel holding the main tabbed work area.
	 * @return work area panel.
	 */
	protected LayoutContainer createWorkAreaPanel()
	{
		LayoutContainer workAreaPanel = new LayoutContainer();
		workAreaPanel.setLayout(new FitLayout());

		m_WorkAreaTabPanel = new WorkAreaTabPanel();
		workAreaPanel.add(m_WorkAreaTabPanel);
		
		return workAreaPanel;
	}
	
	
	/**
	 * Logs the user out of the application, returning them to the log in page.
	 */
	public void logout()
	{
		// Call the LogoutService to invalidate the Http session and
		// then reload the page.
		ApplicationResponseHandler<Void> callback = 
			new ApplicationResponseHandler<Void>() {
                public void uponFailure(Throwable caught) 
                {
                	Window.Location.reload();
                }


                public void uponSuccess(Void result)
                {
                	Window.Location.reload();
                }
          };
		
		QueryServiceLocator.getInstance().getLogoutService().logout(callback);
	}
	
	
	public static int displayValue(int val)
	{
		MessageBox.alert("Splash page", "The counter value is:" + val, null);
		return 1;
	}
	
	
	public void displayCounterValue(int val)
	{
		 //m_ValueLabel.setText("Value from counter applet: " + val);
		MessageBox.alert("Splash", "displayCounterValue()", null);
	}
	
	
	public void appletViewToolRun(String viewToOpenDataType)
	{
		MessageBox.alert("TimeSeriesChartApplet", "show view - " + viewToOpenDataType, null);
	}
	
	
	
	/**
	 * Tests out calling a method in the applet and passing it a String parameter.
	 * @param appletId id of the applet.
	 * @param name source name to set in applet.
	 */
	public static native void setSourceName(String appletId, String name) /*-{ 
    $doc.getElementById(appletId).setSourceName(name); 
}-*/; 
	
	
	
	public static native void defineBridgeMethod() /*-{      
		 $wnd.displayValue = function(val)  {          
		 	return @demo.app.splash.gxt.Splash::displayValue(I)(val);       
		 }    
	 }-*/;
	
	
	public native void defineBridgeMethod2() /*-{      
		var blah = this;
		$wnd.displayCounterValue = function(val)  {          
			blah.@demo.app.splash.gxt.Splash::displayCounterValue(I)(val);       
		}    
	}-*/;
	
	

	

}
