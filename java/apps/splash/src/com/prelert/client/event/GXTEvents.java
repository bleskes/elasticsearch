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

/**
 * Defines specialised Ext GWT (GXT) event types found in Prelert clients.
 * 
 * @author Pete Harverson
 */
public final class GXTEvents
{
	/**
	 * Before pan left event type, such as from a chart.
	 */
	public static final EventType BeforePanLeft = new EventType();
	
	/**
	 * Before pan right event type, such as from a chart.
	 */
	public static final EventType BeforePanRight = new EventType();
	
	/**
	 * Before zoom in event type, such as from a chart.
	 */
	public static final EventType BeforeZoomIn = new EventType();
	
	/**
	 * Before zoom out event type, such as from a chart.
	 */
	public static final EventType BeforeZoomOut = new EventType();
	
	/**
	 * Logout click.
	 */
	public static final EventType LogoutClick = new EventType();
	
	/**
	 * Open Causality View click.
	 */
	public static final EventType OpenCausalityViewClick = new EventType();

	/**
	 * Open View click, often where the type of view (notification or time
	 * series) is not known in the source component.
	 */
	public static final EventType OpenViewClick = new EventType();

	/**
	 * Open Notification View click.
	 */
	public static final EventType OpenNotificationViewClick = new EventType();

	/**
	 * Open Time Series View click.
	 */
	public static final EventType OpenTimeSeriesViewClick = new EventType();
	
	/**
	 * Run search click.
	 */
	public static final EventType RunSearchClick = new EventType();
	
	/**
	 * Show Module click.
	 */
	public static final EventType ShowModuleClick = new EventType();
	

	private GXTEvents()
	{

	}

}
