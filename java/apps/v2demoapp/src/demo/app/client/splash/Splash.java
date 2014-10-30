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

package demo.app.client.splash;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.BaseTreeModel;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;


/**
 * Entry point for Splash UI.
 */
public class Splash implements EntryPoint
{
	private final String appletId = "MyApplet"; 
	private final static int APPLET_WIDTH = 600;
	private final static int APPLET_HEIGHT = 500;
	
	private Label 		m_ValueLabel;
	
	
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
		Viewport container = new Viewport();
		container.addStyleName("viewport");
		container.setLayout(new BorderLayout());
		
		// Create components for the Data sources tree.
		VerticalPanel dataSourcePanel = createDataSourcePanel();
		
		VerticalPanel panelMain = new VerticalPanel();
		Button buttonInc = new Button("Increment");
		
		buttonInc.addSelectionListener(new SelectionListener<ButtonEvent>()
		{

            @Override
            public void componentSelected(ButtonEvent ce)
            {
            	// Test out calling methods on the applet.
            	incrementCounter(appletId);
            	setSourceName(appletId, "london-webserver1");
            }
		});

		panelMain.add(buttonInc);
		
		// Add in the charting applet.
		HTML appletWidget = new HTML(); 
        appletWidget.setHTML(createAppletHTML()); 
        panelMain.add(appletWidget); 
		
        // Add in label to test out displaying a value passed by the applet.
        m_ValueLabel = new Label("Value from counter applet: - ");
        panelMain.add(m_ValueLabel); 
        
        BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 50);     
        northData.setFloatable(false);   
        northData.setHideCollapseTool(true);   
        northData.setSplit(false);   
        northData.setMargins(new Margins(10, 10, 10, 10));  
        
        BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 180);   
        westData.setSplit(true);   
        westData.setCollapsible(true);   
        westData.setMargins(new Margins(0, 10, 10, 10));   
        
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
        centerData.setMargins(new Margins(0));
		
        container.add(IconHelper.create("splash/images/logo.png", 194, 46).createImage(), northData);    
        container.add(dataSourcePanel, westData);    
        container.add(panelMain, centerData);   

		RootPanel.get("splashGUI").add(container);
	}
	
	
	protected VerticalPanel createDataSourcePanel()
	{		
		VerticalPanel dataSourcePanel = new VerticalPanel();
		dataSourcePanel.addStyleName("dataSourceTree");
		
		Label titleLabel = new Label("Analysed Data");
		titleLabel.addStyleName("headerLabels");
		
		TreeStore<BaseTreeModel> dataSourcesTreeStore = getDataSourcesTree();
		TreePanel<BaseTreeModel> dataSourcesTree = new TreePanel<BaseTreeModel>(dataSourcesTreeStore);   
		dataSourcesTree.setDisplayProperty("label"); 
		dataSourcesTree.setWidth(150);
		
		dataSourcePanel.add(titleLabel);
		dataSourcePanel.add(dataSourcesTree);
		
		return dataSourcePanel;
	}
	
	
	protected TreeStore<BaseTreeModel> getDataSourcesTree()
	{
		// Just hardcode a simple data sources tree.

		// p2ps logs data.
		BaseTreeModel p2pslogs = new BaseTreeModel();
		p2pslogs.set("label", "p2ps logs");
		
		BaseTreeModel logs8202 = new BaseTreeModel();
		logs8202.set("label", "sol30m-8202");
		BaseTreeModel logs8203 = new BaseTreeModel();
		logs8203.set("label", "sol30m-8203");
		
		p2pslogs.add(logs8202);
		p2pslogs.add(logs8203);
		
		// p2psmon user usage data.
		BaseTreeModel userUsage = new BaseTreeModel();
		userUsage.set("label", "p2psmon user usage");
		
		BaseTreeModel user8202 = new BaseTreeModel();
		user8202.set("label", "sol30m-8202");
		BaseTreeModel user8203 = new BaseTreeModel();
		user8203.set("label", "sol30m-8203");
		
		userUsage.add(user8202);
		userUsage.add(user8203);
		
		TreeStore<BaseTreeModel> dataSourcesTreeStore = new TreeStore<BaseTreeModel>();
		dataSourcesTreeStore.add(p2pslogs, true);
		dataSourcesTreeStore.add(userUsage, true);
		
		return dataSourcesTreeStore;
	}
	
	
	public static int displayValue(int val)
	{
		MessageBox.alert("Splash page", "The counter value is:" + val, null);
		return 1;
	}
	
	
	public void displayCounterValue(int val)
	{
		 m_ValueLabel.setText("Value from counter applet: " + val);
	}
	
	
	/**
	 * Creates the HTML to embed the applet in the web page.
	 * @return the applet HTML.
	 */
	private String createAppletHTML() 
	{ 
		
		String archiveList = createAppletJarsList();
		
        StringBuffer buff = new StringBuffer(); 
        
        buff.append("<!--[if !IE]> -->"); 
        buff.append("<object classid= \"java:demo.app.client.splash.applet.ChartApplet.class\""); 
        buff.append(" type=\"application/x-java-applet\""); 
        buff.append(" archive=\"" + archiveList + "\""); 
        buff.append(" height=\"" + APPLET_HEIGHT+ "\" width=\"" + APPLET_WIDTH + "\""); 
        buff.append(" id=\"" + appletId + "\" name=\"" + appletId + "\""); 
        buff.append(" >"); 
        //buff.append("<param name=\"code\" value= \"com.optilab.sargas.applets.MyApplet.class\" />"); 
        buff.append("<param name=\"archive\" value=\"" + archiveList + "\" />"); 
        buff.append("<param name=\"persistState\" value=\"false\" / >"); 
        buff.append("<param name=\"mayscript\" value=\"yes\" />"); 
        buff.append("<param name=\"scriptable\" value=\"true\" />"); 
        buff.append("<param name=\"servletUrl\" value=\"" + GWT.getModuleBaseURL() + "someservlet\" />"); 
        buff.append("<center>"); 
        buff.append("<p><strong>ChartApplet content requires Java 1.5 or higher, which your browser does not appear to have.</strong></p>"); 
        buff.append("<p><a href=\"http://www.java.com/en/download/index.jsp\">Get the latest Java Plug-in.</a></p>"); 
        buff.append("</center>"); 
        buff.append("</object>"); 
        buff.append("<!--<![endif]-->"); 
        
        buff.append("<!--[if IE]>"); 
        buff.append("<object classid=\"clsid:8AD9C840-044E-11D1-B3E9-00805F499D93\""); 
        buff.append(" codebase=\"http://java.sun.com/products/plugin/autodl/jinstall-1_4-windows-i586.cab#Version=1,4,0,0\""); 
        buff.append(" height=\"" + APPLET_HEIGHT+ "\" width=\"" + APPLET_WIDTH + "\"");  
        buff.append(" id=\"" + appletId + "\" name=\"" + appletId + "\""); 
        buff.append(" >"); 
        buff.append("<param name=\"code\" value=\"demo.app.client.splash.applet.ChartApplet.class\" />"); 
        buff.append("<param name=\"archive\" value=\"" + archiveList + "\" />"); 
        buff.append("<param name=\"persistState\" value=\"false\" />"); 
        buff.append("<param name=\"mayscript\" value=\"yes\" />"); 
        buff.append("<param name=\"scriptable\" value=\"true\" />"); 
        buff.append("<param name=\"servletUrl\" value=\"" + GWT.getModuleBaseURL() + "someservlet\" />"); 
        buff.append("<center>"); 
        buff.append("<p><strong>ChartApplet content requires Java 1.5 or higher, which your browser does not appear to have.</strong></p>"); 
        buff.append("<p><a href=\"http://www.java.com/en/download/index.jsp\">Get the latest Java Plug-in.</a></p>"); 
        buff.append("</center>"); 
        buff.append("</object>"); 
        buff.append("<![endif]-->"); 
        
        return buff.toString(); 
    } 
	
	
	private String createAppletJarsList()
	{
		StringBuffer buff = new StringBuffer(); 
		
		buff.append(GWT.getHostPageBaseURL());
		buff.append("splashApplet.jar, ");
		buff.append(GWT.getHostPageBaseURL());
		buff.append("jfreechart-1.0.13.jar, ");
		buff.append(GWT.getHostPageBaseURL());
		buff.append("jcommon-1.0.16.jar");
		
		return buff.toString(); 
	}
	
	
	public static native void incrementCounter(String appletId) /*-{ 
    $doc.getElementById(appletId).increment(); 
}-*/; 
	
	
	public static native void setSourceName(String appletId, String name) /*-{ 
    $doc.getElementById(appletId).setSourceName(name); 
}-*/; 
	
	
	
	public static native void defineBridgeMethod() /*-{      
		 $wnd.displayValue = function(val)  {          
		 	return @demo.app.client.splash.Splash::displayValue(I)(val);       
		 }    
	 }-*/;
	
	
	public native void defineBridgeMethod2() /*-{      
		var blah = this;
		$wnd.displayCounterValue = function(val)  {          
			blah.@demo.app.client.splash.Splash::displayCounterValue(I)(val);       
		}    
	}-*/;
	

}
