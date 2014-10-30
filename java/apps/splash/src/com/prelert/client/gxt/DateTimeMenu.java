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

package com.prelert.client.gxt;

import java.util.Date;

import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.prelert.client.event.DateTimePickerEvent;

/**
 * A Menu for choosing a date and time.
 * <p>
 * Note that this class is based on the Ext GWT work-in-progress at
 * http://fisheye3.atlassian.com/browse/~raw,r=6517/adempiere/contributions/
 * e-Evolution/ADempiereGWT/client/src/com/extjs/gxt/ui/client/widget/menu/DateTimeMenu.java
 * 
 */
public class DateTimeMenu extends Menu {

	/**
	 * The internal date time picker.
	 */
	protected DateTimePicker picker;


	/**
	 * Creates a new menu containing a DateTimePicker for
	 * selecting a date/time.
	 */
	public DateTimeMenu()
	{
	    super();
	    picker = new DateTimePicker();
	    
	    // Add listeners for Select or Cancel on the DateTimePicker 
	    // so that the menu can be hidden.
	    picker.addListener(Events.Select, new Listener<DateTimePickerEvent>()
		{
			public void handleEvent(DateTimePickerEvent be)
			{
				hide(true);
			}
		});
	    
	    picker.addListener(Events.CancelEdit, new Listener<DateTimePickerEvent>()
		{
			public void handleEvent(DateTimePickerEvent be)
			{
				hide(true);
			}
		});
	    
	    add(picker);
	    addStyleName("x-date-menu");
	    setAutoHeight(true);
	    plain = true;
	    showSeparator = false;
	    setEnableScrolling(false);
	}


	/**
	 * Returns the selected date/time.
	 * 
	 * @return the date
	 */
	public Date getDate()
	{
		return picker.getValue();
	}


	/**
	 * Returns the date time picker in this menu.
	 * 
	 * @return the date time picker
	 */
	public DateTimePicker getDateTimePicker()
	{
		return picker;
	}

}

