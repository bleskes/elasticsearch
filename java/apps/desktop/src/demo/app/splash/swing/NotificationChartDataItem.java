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

package demo.app.splash.swing;

import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeriesDataItem;

import demo.app.data.Evidence;


/**
 * Class for a chart data item which represents a notifcation.
 * @author Pete Harverson
 */
public class NotificationChartDataItem extends TimeSeriesDataItem
{
	private Evidence	m_Notification;
	
	
	/**
	 * Creates a new chart data item to represent a notification.
	 * @param notification item of evidence to be represented by this chart data item.
	 */
	public NotificationChartDataItem(Evidence notification)
	{
		super(new Millisecond(notification.getTime()), 0);
		
		m_Notification = notification;
	}
	
	
	/**
	 * Returns the notification associated with the chart data item.
     * @return the item of evidence represented by this chart data item.
     */
    public Evidence getNotification()
    {
    	return m_Notification;
    }

	
}
