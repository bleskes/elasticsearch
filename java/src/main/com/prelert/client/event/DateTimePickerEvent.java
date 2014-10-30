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

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.prelert.client.gxt.DateTimePicker;


public class DateTimePickerEvent extends ComponentEvent
{
	private DateTimePicker m_DateTimePicker;

	private Date m_Date;


	public DateTimePickerEvent(DateTimePicker datePicker)
	{
		super(datePicker);
		m_DateTimePicker = datePicker;
	}


	/**
	 * Returns the source date picker.
	 * 
	 * @return the date picker
	 */
	public DateTimePicker getDatePicker()
	{
		return m_DateTimePicker;
	}
	
	
	/**
	 * Returns the date.
	 * 
	 * @return the date
	 */
	public Date getDate()
	{
		return m_Date;
	}


	/**
	 * Sets the date.
	 * 
	 * @param date the date
	 */
	public void setDate(Date date)
	{
		this.m_Date = date;
	}
}
