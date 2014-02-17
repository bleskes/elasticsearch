package com.prelert.client.event;

import java.util.Date;

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.prelert.client.widget.DateTimePicker;

/**
 * Event generated when a value is set in a Date Time picker component.
 * <p>
 * Note that this class is based on the Ext GWT work-in-progress at
 * http://fisheye3.atlassian.com/browse/~raw,r=6517/adempiere/contributions/
 * e-Evolution/ADempiereGWT/client/src/com/extjs/gxt/ui/client/event/DateTimePickerEvent.java
 * 
 * @author Pete Harverson
 */
public class DateTimePickerEvent extends ComponentEvent
{
	private DateTimePicker 	m_DateTimePicker;

	private Date 			m_Date;


	/**
	 * Creates a new DateTimePickerEvent.
	 * @param datePicker the DateTimePicker component that generated the event.
	 */
	public DateTimePickerEvent(DateTimePicker datePicker)
	{
		super(datePicker);
		m_DateTimePicker = datePicker;
	}
	
	
	/**
	 * Returns the source date/time picker.
	 * 
	 * @return the date picker
	 */
	public DateTimePicker getDatePicker()
	{
		return m_DateTimePicker;
	}
	
	
	/**
	 * Returns the date that has been set in the DateTimePicker.
	 * @return the date
	 */
	public Date getDate()
	{
		return m_Date;
	}


	/**
	 * Sets the date encapsulated in this event.
	 * @param date the date
	 */
	public void setDate(Date date)
	{
		m_Date = date;
	}
}
