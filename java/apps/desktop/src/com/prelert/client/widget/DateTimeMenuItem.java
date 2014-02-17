package com.prelert.client.widget;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.menu.Item;
import com.google.gwt.user.client.Element;
import com.prelert.client.event.DateTimePickerEvent;


/**
 * A MenuItem that displays a DateTimePicker.
 * <p>
 * Note that this class is based on the Ext GWT work-in-progress at
 * http://fisheye3.atlassian.com/browse/~raw,r=6517/adempiere/contributions/e-Evolution/
 * ADempiereGWT/client/src/com/extjs/gxt/ui/client/widget/menu/DateTimeMenuItem.java
 */
public class DateTimeMenuItem extends Item
{

	protected DateTimePicker picker;


	/**
	 * Creates a new menu item containing a date time picker.
	 */
	public DateTimeMenuItem()
	{
		hideOnClick = true;
		picker = new DateTimePicker();
		picker.addListener(Events.Select, new Listener<DateTimePickerEvent>()
		{
			public void handleEvent(DateTimePickerEvent de)
			{
				parentMenu.fireEvent(Events.Select, de);
				parentMenu.hide();
			}
		});
		
		picker.addListener(Events.Close, new Listener<DateTimePickerEvent>()
		{
			public void handleEvent(DateTimePickerEvent de)
			{
				parentMenu.hide();
			}
		});
	}


	@Override
	protected void onRender(Element target, int index)
	{
		super.onRender(target, index);
		picker.render(target, index);
		setElement(picker.getElement());
	}


	@Override
	protected void handleClick(ComponentEvent be)
	{
		picker.onComponentEvent((ComponentEvent) be);
	}

}
