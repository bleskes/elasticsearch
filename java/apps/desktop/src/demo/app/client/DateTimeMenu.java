package demo.app.client;

import java.util.Date;

import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.core.client.GWT;

/**
 * A Menu for choosing a date.
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

