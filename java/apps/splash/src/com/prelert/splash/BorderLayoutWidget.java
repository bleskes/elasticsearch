package com.prelert.splash;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class BorderLayoutWidget extends LayoutContainer
{
	protected void onRender(Element target, int index) {   
		
	    super.onRender(target, index);   
	    
	    final BorderLayout layout = new BorderLayout();   
	    setLayout(layout);   
	  //  setStyleAttribute("padding", "5px");   
	  
	    ContentPanel north = new ContentPanel();   
	    ContentPanel west = new ContentPanel();   
	    ContentPanel center = new ContentPanel();   
	    center.setHeading("BorderLayout Example");   
	    center.setScrollMode(Scroll.AUTOX);   
	  
 
	  
	      
	    center.add(new Label("Hello"));   
	  
	    ContentPanel east = new ContentPanel();   
	    ContentPanel south = new ContentPanel();   
	  
	    BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 100);   
	    northData.setCollapsible(true);   
	    northData.setFloatable(true);   
	    northData.setHideCollapseTool(true);   
	    northData.setSplit(true);   
	    if (GXT.isGecko)
	    {
	    	northData.setMargins(new Margins(0, 0, 5, 0));   
	    }
	    else
	    {
	    	northData.setMargins(new Margins(0, 0, 5, 0));   
	    }
	  
	    BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 150);   
	    westData.setSplit(true);   
	    westData.setCollapsible(true);   
	    westData.setMargins(new Margins(0,5,0,0));   
	  
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));   
	  
	    BorderLayoutData eastData = new BorderLayoutData(LayoutRegion.EAST, 150);   
	    eastData.setSplit(true);   
	    eastData.setCollapsible(true);   
	    eastData.setMargins(new Margins(0,0,0,5));   
	  
	    BorderLayoutData southData = new BorderLayoutData(LayoutRegion.SOUTH, 300);   
	    southData.setSplit(true);   
	    southData.setCollapsible(true);   
	    southData.setFloatable(true);   
	    southData.setMargins(new Margins(5, 0, 0, 0));   
	  

	    
		//add(north, northData);   
	 //	add(west, westData);   
	  	add(center, centerData);   
	 //	add(east, eastData);   
		add(south, southData);   
	    
	    
	    GWT.log("isGecko:" + GXT.isGecko);
	  }   

}
