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

import com.extjs.gxt.ui.client.event.EventType;
import com.extjs.gxt.ui.client.event.Listener;


/**
 * Listener for <code>ChartWidget</code> events.
 * @author Pete Harverson
 */
public class ChartWidgetListener implements Listener<ChartWidgetEvent>
{

	@Override
    public void handleEvent(ChartWidgetEvent e)
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
    }
	
	
	/**
	 * Fires before a chart pan operation.
	 * @param e the chart widget event.
	 */
	public void chartBeforePan(ChartWidgetEvent e)
	{

	}


	/**
	 * Fires before a chart zoom operation.
	 * @param e the chart widget event.
	 */
	public void chartBeforeZoom(ChartWidgetEvent e)
	{

	}

}
