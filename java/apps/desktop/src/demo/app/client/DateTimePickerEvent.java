package demo.app.client;

import java.util.Date;

import com.extjs.gxt.ui.client.event.ComponentEvent;


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
