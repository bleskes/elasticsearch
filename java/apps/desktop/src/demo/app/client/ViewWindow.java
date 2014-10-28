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

package demo.app.client;

import com.extjs.gxt.ui.client.widget.Window;
import com.google.gwt.user.client.ui.Widget;

/**
 * Abstract subclass of the Ext GWT Window which implements the ViewWidget interface
 * defining methods for displaying information from a Prelert data source.
 * @author Pete Harverson
 */
public abstract class ViewWindow extends Window implements ViewWidget
{
	/**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Ext GWT Window itself.
	 */
	public Widget getWidget()
	{
		// Return the Window.
		return this;
	}

}
