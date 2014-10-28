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

package demo.app.client.event;

import com.extjs.gxt.ui.client.event.EventType;

/**
 * Defines specialised Ext GWT (GXT) event types found in Prelert clients.
 * @author Pete Harverson
 */
public final class GXTEvents
{
	  /**
	   * Probable cause click event type.
	   */
	  public static final EventType OpenCausalityViewClick = new EventType();
	  
	  
	  public static final EventType OpenNotificationViewClick = new EventType();
	  
	  
	  public static final EventType OpenTimeSeriesViewClick = new EventType();
	  
	  
	  private GXTEvents()
	  {
		  
	  }

}
