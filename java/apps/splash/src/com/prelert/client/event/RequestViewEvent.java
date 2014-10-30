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

package com.prelert.client.event;

import java.util.Date;

import com.extjs.gxt.ui.client.event.DomEvent;
import com.google.gwt.user.client.ui.Widget;

import com.prelert.data.DataSourceType;


/**
 * RequestView event type, corresponding to a trigger for requesting a
 * particular view be displayed.
 * @author Pete Harverson
 *
 * @param <M> data associated with the event e.g. the selected row of
 * 		evidence or TimeSeriesConfig.
 */
public class RequestViewEvent<M> extends DomEvent
{
	private DataSourceType	m_OpenViewDataType;

	private String			m_Source;
	private Date			m_OpenAtTime;
	
	private M				m_Model;
	
	
	/**
	 * Creates a new RequestViewEvent, fired by the specified component.
	 * @param widget the widget generating the event.
	 */
	public RequestViewEvent(Widget widget)
    {
	    super(widget);
    }
	
	
	/**
	 * Returns the data source type of the view to be opened e.g. system_udp, p2pslogs.
     * @return the data source type of the view to open.
     */
    public DataSourceType getViewToOpenDataType()
    {
    	return m_OpenViewDataType;
    }


	/**
	 * Sets the data source type of the view to be opened e.g. system_udp, p2pslogs.
     * @param openViewDataType the data source type of the view to open.
     */
    public void setViewToOpenDataType(DataSourceType openViewDataType)
    {
    	m_OpenViewDataType = openViewDataType;
    }


	/**
	 * Returns the name of the source of the model data that was selected
	 * when this event was triggered.
     * @return the name of the source (server).
     */
    public String getSourceName()
    {
    	return m_Source;
    }


	/**
	 * Sets the name of the source (server) associated with this event
	 * e.g. the name of the source of a selected point in a time series.
     * @param source the name of the source (server).
     */
    public void setSourceName(String source)
    {
    	m_Source = source;
    }
    
    
    /**
     * Returns the time which should be displayed in the view that is being requested.
     * @return the time the requested view should display.
     */
    public Date getOpenAtTime()
    {
    	return m_OpenAtTime;
    }


	/**
	 * Sets the time which should be displayed in the view that is being requested.
     * @param openAtTime tthe time the requested view should display.
     */
    public void setOpenAtTime(Date openAtTime)
    {
    	m_OpenAtTime = openAtTime;
    }


    /**
     * Returns the model data associated with the event e.g. the selected row of
     * evidence when running a 'Show Probable Cause' tool.
     * @return	the model data.
     */
	public M getModel()
    {
    	return m_Model;
    }
    
    
	/**
	 * Sets the model data associated with the event e.g. the selected row of
     * evidence when running a 'Show Probable Cause' tool.
	 * @param model the model data.
	 */
    public void setModel(M model)
    {
    	m_Model = model;
    }
    

}
