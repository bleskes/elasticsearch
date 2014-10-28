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

package com.prelert.client.chart;

import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.BoxLayout.BoxLayoutPack;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.prelert.client.ClientUtil;


/**
 * Toolbar containing zooming and panning controls for use in a ChartWidget component.
 * @author Pete Harverson
 *
 * @param <D> the data type being displayed in the parent ChartWidget.
 * @see ChartWidget
 */
public class ChartToolBar<D> extends LayoutContainer
{
	private ChartWidget<D>			m_ChartWidget;
	
	private Anchor 					m_PanLeftAnchor;
	private Anchor					m_PanRightAnchor;
	private Anchor					m_ZoomInAnchor;
	private Anchor					m_ZoomOutAnchor;
	
	
	/**
	 * Creates zooming, and optionally panning, controls for the specified chart
	 * widget.
	 * @param chartWidget the chart widget on which the controls are to be used.
	 * @param showPan <code>true</code> to add pan left/right controls, 
	 * 	<code>false</code> to only create zoom in/out controls.
	 */
	public ChartToolBar(ChartWidget<D> chartWidget, boolean showPan)
	{
		m_ChartWidget = chartWidget;
		
		HBoxLayout zbcLayout = new HBoxLayout();     
        zbcLayout.setPadding(new Padding(0, 3, 0, 3));
        zbcLayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);   

        zbcLayout.setPack(BoxLayoutPack.END);   
        setLayout(zbcLayout); 
        if (showPan == true)
        {
        	setSize(280, 20);
        }
        else
        {
        	setSize(140, 20);
        }
        
        if (showPan == true)
        {
	        m_PanLeftAnchor = new Anchor(ClientUtil.CLIENT_CONSTANTS.panLeftLink(), true);
	        m_PanLeftAnchor.addStyleName("prl-textLink");
	        m_PanLeftAnchor.addStyleName("prl-timeSeriesChart-btnPanLeft");
	        m_PanLeftAnchor.addClickHandler(new ClickHandler(){
	
				@Override
	            public void onClick(ClickEvent event)
	            {
					m_ChartWidget.panLeft();
	            }
	        	
	        });
	        
	        m_PanRightAnchor = new Anchor(ClientUtil.CLIENT_CONSTANTS.panRightLink(), true);
	        m_PanRightAnchor.setStyleName("prl-textLink");
	        m_PanRightAnchor.addStyleName("prl-timeSeriesChart-btnPanRight");
	        m_PanRightAnchor.addClickHandler(new ClickHandler(){
	
				@Override
	            public void onClick(ClickEvent event)
	            {
					m_ChartWidget.panRight();
	            }
	        	
	        });
	        
	        add(m_PanLeftAnchor, new HBoxLayoutData(new Margins(0, 10, 0, 0)));    
	        add(m_PanRightAnchor, new HBoxLayoutData(new Margins(0, 15, 0, 0)));  
        }
        
  
        m_ZoomInAnchor = new Anchor(ClientUtil.CLIENT_CONSTANTS.zoomInLink(), true);
        m_ZoomInAnchor.addStyleName("prl-textLink");
        m_ZoomInAnchor.addStyleName("prl-timeSeriesChart-btnZoomIn");
        m_ZoomInAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
				m_ChartWidget.zoomInDateAxis();
            }
        	
        });
        
        m_ZoomOutAnchor = new Anchor(ClientUtil.CLIENT_CONSTANTS.zoomOutLink(), true); 
        m_ZoomOutAnchor.addStyleName("prl-textLink");
        m_ZoomOutAnchor.addStyleName("prl-timeSeriesChart-btnZoomOut");
        m_ZoomOutAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
				m_ChartWidget.zoomOutDateAxis();
            }
        	
        });

          
        add(m_ZoomInAnchor, new HBoxLayoutData(new Margins(0, 10, 0, 0)));    
        add(m_ZoomOutAnchor, new HBoxLayoutData(new Margins(0)));   
	}
	
	
	/**
	 * Returns whether the panning controls are shown on the toolbar.
	 * @return <code>true</code> if the pan left/pan right controls have been added,
	 * 		<code>false</code> otherwise.
	 */
	public boolean isShowPan()
	{
		return !(m_PanLeftAnchor == null);
	}
	
	
	/**
	 * Enables or disables the chart tools.
	 * @param enabled <code>true</code> to enable, <code>false</code> to disable.
	 */
    @Override
    public void setEnabled(boolean enabled)
    {
    	if (enabled == true)
    	{
    		fireEvent(Events.Enable);
    	}
    	else
    	{
    		fireEvent(Events.Disable);
    	}
	    
    	if (m_PanLeftAnchor != null)
    	{
    		m_PanLeftAnchor.setEnabled(enabled);
    		m_PanRightAnchor.setEnabled(enabled);
    	}
		
		if (enabled == true)
		{
			if (m_PanLeftAnchor != null)
			{
				m_PanLeftAnchor.removeStyleName("prl-textLink-disabled");
				m_PanRightAnchor.removeStyleName("prl-textLink-disabled");
			}
			
			if (m_ChartWidget.isMaxZoomLevel() == false)
	        {
				m_ZoomInAnchor.setEnabled(true);
				m_ZoomInAnchor.removeStyleName("prl-textLink-disabled");
	        }
			
			if (m_ChartWidget.isMinZoomLevel() == false)
			{
				m_ZoomOutAnchor.setEnabled(enabled);
				m_ZoomOutAnchor.removeStyleName("prl-textLink-disabled");
			}
		}
		else
		{
			m_ZoomInAnchor.setEnabled(false);
			m_ZoomOutAnchor.setEnabled(false);
			
			if (m_PanLeftAnchor != null)
			{
				m_PanLeftAnchor.addStyleName("prl-textLink-disabled");
				m_PanRightAnchor.addStyleName("prl-textLink-disabled");
			}
			
			m_ZoomInAnchor.addStyleName("prl-textLink-disabled");
			m_ZoomOutAnchor.addStyleName("prl-textLink-disabled");
		}
    }
}
