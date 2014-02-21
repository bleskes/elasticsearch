package com.prelert.client.widget;

import java.util.Date;

import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.menu.Menu;

/**
 * A Menu for choosing a date and time.
 * <p>
 * Note that this class is based on the Ext GWT work-in-progress at
 * http://fisheye3.atlassian.com/browse/~raw,r=6517/adempiere/contributions/
 * e-Evolution/ADempiereGWT/client/src/com/extjs/gxt/ui/client/widget/menu/DateTimeMenu.java
 * 
 */
public class DateTimeMenu extends Menu
{

	/**
	 * The internal date time picker.
	 */
	protected DateTimePicker picker;

	private DateTimeMenuItem m_MenuItem;


	public DateTimeMenu()
	{
		m_MenuItem = new DateTimeMenuItem();
		picker = m_MenuItem.picker;
		add(m_MenuItem);
		baseStyle = "x-date-menu";
		setAutoHeight(true);
	}


	/**
	 * Returns the selected date/time.
	 * 
	 * @return the date
	 */
	public Date getDate()
	{
		return m_MenuItem.picker.getValue();
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


	@Override
	protected void doAttachChildren()
	{
		super.doAttachChildren();
		ComponentHelper.doAttach(picker);
	}


	@Override
	protected void doDetachChildren()
	{
		super.doDetachChildren();
		ComponentHelper.doDetach(picker);
	}

}
