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

package com.prelert.client.event;

import com.extjs.gxt.ui.client.event.EventType;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;


/**
 * Listener for <code>ChartWidget</code> events.
 * @param <M> any data associated with the event e.g. TimeSeriesConfig.
 * @author Pete Harverson
 */
public class ChartWidgetListener<M> implements Listener<ChartWidgetEvent<M>>
{

	@Override
    public void handleEvent(ChartWidgetEvent<M> e)
    {
		EventType type = e.getType();
		if (type == GXTEvents.BeforePanLeft || type == GXTEvents.BeforePanRight)
		{
			chartBeforePan(e);
		}
		else if (type == GXTEvents.BeforeZoomIn || type == GXTEvents.BeforeZoomOut)
		{
			chartBeforeZoom(e);
		}
		else if (type == Events.Remove)
		{
			chartRemove(e);
		}
    }
	
	
	/**
	 * Fires before a chart pan operation.
	 * @param e the chart widget event.
	 */
	public void chartBeforePan(ChartWidgetEvent<M> e)
	{

	}


	/**
	 * Fires before a chart zoom operation.
	 * @param e the chart widget event.
	 */
	public void chartBeforeZoom(ChartWidgetEvent<M> e)
	{

	}
	
	
	/**
	 * Fires after removal of data from the chart widget.
	 * @param e the chart widget event.
	 */
	public void chartRemove(ChartWidgetEvent<M> e)
	{
		
	}

}
