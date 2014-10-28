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

package com.prelert.client.chart;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.prelert.client.ClientUtil;


/**
 * Toolbar containing zooming and panning controls for use in a ChartWidget component.
 * @author Pete Harverson
 *
 * @param <D> the data type being displayed in the parent ChartWidget.
 * @see ChartWidget
 */
public class ChartToolBar<D> extends ToolBar
{
	private ChartWidget<D>	m_ChartWidget;
	
	private Button			m_PanLeftBtn;
	private Button			m_PanRightBtn;
	private Button			m_ZoomInBtn;
	private Button			m_ZoomOutBtn;
	
	
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
		
		addStyleName("prl-internal-toolbar");
		setBorders(false);
		setSpacing(2);
        
        if (showPan == true)
        {
	        m_PanLeftBtn = new Button();  
	        m_PanLeftBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
	        m_PanLeftBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_pan_left()));
	        m_PanLeftBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.panLeftLink());
	        m_PanLeftBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
			{
				@Override
	            public void componentSelected(ButtonEvent ce)
				{
					m_ChartWidget.panLeft();
				}
			});
	        
	        m_PanRightBtn = new Button();
	        m_PanRightBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
	        m_PanRightBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_pan_right()));
	        m_PanRightBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.panRightLink());
	        m_PanRightBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
			{
				@Override
	            public void componentSelected(ButtonEvent ce)
				{
					m_ChartWidget.panRight();
				}
			});

	        add(m_PanLeftBtn);    
	        add(m_PanRightBtn);  
        }
        
        
        m_ZoomInBtn = new Button();
        m_ZoomInBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
        m_ZoomInBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_zoom_in()));
        m_ZoomInBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.zoomIn());
        m_ZoomInBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				if (m_ZoomInBtn.isEnabled())
				{
					m_ChartWidget.zoomInDateAxis();
				}
			}
		});
        
        m_ZoomOutBtn = new Button();
        m_ZoomOutBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
        m_ZoomOutBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_zoom_out()));
        m_ZoomOutBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.zoomOut());
        m_ZoomOutBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				if (m_ZoomOutBtn.isEnabled())
				{
					m_ChartWidget.zoomOutDateAxis();
				}
			}
		});

        add(m_ZoomInBtn);
        add(m_ZoomOutBtn);        
	}
	
	
	/**
	 * Returns whether the panning controls are shown on the toolbar.
	 * @return <code>true</code> if the pan left/pan right controls have been added,
	 * 		<code>false</code> otherwise.
	 */
	public boolean isShowPan()
	{
		return !(m_PanLeftBtn == null);
	}
	
	
	/**
	 * Enables or disables the chart tools.
	 * @param enabled <code>true</code> to enable, <code>false</code> to disable.
	 */
    @Override
    public void setEnabled(boolean enabled)
    {
    	super.setEnabled(enabled);
		
		if (enabled == true)
		{
			if (m_ChartWidget.isMaxZoomLevel() == false)
	        {
				m_ZoomInBtn.setEnabled(true);
	        }
			else
			{
				m_ZoomInBtn.setEnabled(false);
			}
			
			if (m_ChartWidget.isMinZoomLevel() == false)
			{
				m_ZoomOutBtn.setEnabled(true);
			}
			else
			{
				m_ZoomOutBtn.setEnabled(false);
			}
		}
    }
 
}
