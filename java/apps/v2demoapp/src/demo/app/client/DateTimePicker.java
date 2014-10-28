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

import java.util.Date;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.core.XDOM;
import com.extjs.gxt.ui.client.Style.Direction;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.core.CompositeElement;
import com.extjs.gxt.ui.client.core.CompositeFunction;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.fx.FxConfig;
import com.extjs.gxt.ui.client.messages.XMessages;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.Size;
import com.extjs.gxt.ui.client.util.Util;
import com.extjs.gxt.ui.client.widget.BoxComponent;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.IconButton;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.constants.DateTimeConstants;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.widgetideas.client.SpinnerListener;
import com.google.gwt.widgetideas.client.ValueSpinner;

/**
 * A date/time picker. The component consists of a calendar grid allowing the
 * user to select a date, and a time field consisting of two spinner controls
 * for setting the hours and minutes portion of the time.
 * 
 * 
 * <dd><b>Select</b> : DatePickerEvent(datePicker, date)<br>
 * <div>Fires when a date is selected.</div>
 * <ul>
 * <li>datePicker : this</li>
 * <li>date : the selected date</li>
 * </ul>
 * </dd> </dt>
 */
/**
 * Simple date picker.
 * 
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>Select</b> : DatePickerEvent(datePicker, date)<br>
 * <div>Fires when a date is selected.</div>
 * <ul>
 * <li>datePicker : this</li>
 * <li>date : the selected date</li>
 * </ul>
 * </dd> </dt>
 */
public class DateTimePicker extends BoxComponent
{

	private int firstDOW;
	private Date m_MinDate;
	private Date m_MaxDate;
	private int startDay;
	private long today;
	private int mpyear;
	private Grid days, grid;
	private Component header;
	private HorizontalPanel timePanel;
	private HorizontalPanel footer;
	private ValueSpinner m_HoursSpinner;
	private ValueSpinner m_MinsSpinner;
	private DateWrapper activeDate, m_Value;
	private int mpSelMonth, mpSelYear;
	private Button todayBtn, monthBtn;
	private Element[] cells;
	private Element[] textNodes;
	private IconButton prevBtn, nextBtn;
	private CompositeElement mpMonths, mpYears;
	private El monthPicker;
	//private DateTimeConstants constants = 
	//	(DateTimeConstants) GWT.create(DateTimeConstants.class);
	
	private DateTimeConstants constants = 
		LocaleInfo.getCurrentLocale().getDateTimeConstants();
	
	private XMessages myMessages = (XMessages) GWT.create(XMessages.class);
	private DatePickerMessages messages;


	/**
	 * Creates a new date picker.
	 */
	public DateTimePicker()
	{
		baseStyle = "x-date-picker";
		messages = new DatePickerMessages();

		// Initialise date value with current time.
		m_Value = new DateWrapper();

	}


	@Override
	public void focus()
	{
		super.focus();
		{
			update(activeDate);
		}
	}


	/**
	 * Returns the field's maximum allowed date.
	 * 
	 * @return the max date
	 */
	public Date getMaxDate()
	{
		return m_MaxDate;
	}


	/**
	 * Returns the data picker messages.
	 * 
	 * @return the date picker messages
	 */
	public DatePickerMessages getMessages()
	{
		return messages;
	}


	/**
	 * Returns the picker's minimum data.
	 * 
	 * @return the min date
	 */
	public Date getMinDate()
	{
		return m_MinDate;
	}


	/**
	 * Returns the picker's start day.
	 * 
	 * @return the start day
	 */
	public int getStartDay()
	{
		return startDay;
	}


	/**
	 * Gets the current selected value of the date field.
	 * 
	 * @return the date
	 */
	public Date getValue()
	{
		return m_Value.asDate();
	}


	@Override
	public void onComponentEvent(ComponentEvent ce)
	{
		super.onComponentEvent(ce);
		switch (ce.getEventTypeInt())
		{
			case Event.ONCLICK:
				onClick(ce);
				return;
		}
	}


	/**
	 * Sets the picker's maximum allowed date.
	 * 
	 * @param maxDate  the max date
	 */
	public void setMaxDate(Date maxDate)
	{
		if (maxDate != null)
		{
			maxDate = new DateWrapper(maxDate).clearTime().asDate();
		}
		this.m_MaxDate = maxDate;
	}


	/**
	 * Sets the data picker messages.
	 * 
	 * @param messages the date picker messages
	 */
	public void setMessages(DatePickerMessages messages)
	{
		this.messages = messages;
	}


	/**
	 * Sets the picker's minimum allowed date.
	 * 
	 * @param minDate
	 *            the min date
	 */
	public void setMinDate(Date minDate)
	{
		if (minDate != null)
		{
			minDate = new DateWrapper(minDate).clearTime().asDate();
		}
		this.m_MinDate = minDate;
	}


	/**
	 * Sets the picker's start day
	 * 
	 * @param startDay
	 *            the start day
	 */
	public void setStartDay(int startDay)
	{
		this.startDay = startDay;
	}


	/**
	 * Sets the value of the date field.
	 * 
	 * @param date
	 *            the date
	 */
	public void setValue(Date date)
	{
		setValue(date, false);
	}


	/**
	 * Sets the value of the date field.
	 * 
	 * @param date
	 *            the date
	 * @param supressEvent true to supress the select event
	 */
	public void setValue(Date date, boolean supressEvent)
	{
		this.m_Value = new DateWrapper(date);// .clearTime();

		if (rendered)
		{
			update(m_Value);
		}
	}


	@Override
	protected void doAttachChildren()
	{
		super.doAttachChildren();
		ComponentHelper.doAttach(header);
		ComponentHelper.doAttach(timePanel);
		ComponentHelper.doAttach(grid);
		ComponentHelper.doAttach(footer);
	}


	@Override
	protected void doDetachChildren()
	{
		super.doDetachChildren();
		ComponentHelper.doDetach(header);
		ComponentHelper.doDetach(timePanel);
		ComponentHelper.doDetach(grid);
		ComponentHelper.doDetach(footer);
		monthPicker.setVisible(false);
	}


	protected void onClick(ComponentEvent be)
	{
		be.stopEvent();
		El target = be.getTargetEl();
		El pn = null;
		String cls = target.getStyleName();
		if (cls.equals("x-date-left-a"))
		{
			showPrevMonth();
		}
		else if (cls.equals("x-date-right-a"))
		{
			showNextMonth();
		}
		if ((pn = target.findParent("td.x-date-mp-month", 2)) != null)
		{
			mpMonths.removeStyleName("x-date-mp-sel");
			El elem = target.findParent("td.x-date-mp-month", 2);
			elem.addStyleName("x-date-mp-sel");
			mpSelMonth = pn.dom.getPropertyInt("xmonth");
		}
		else if ((pn = target.findParent("td.x-date-mp-year", 2)) != null)
		{
			mpYears.removeStyleName("x-date-mp-sel");
			El elem = target.findParent("td.x-date-mp-year", 2);
			elem.addStyleName("x-date-mp-sel");
			mpSelYear = pn.dom.getPropertyInt("xyear");
		}
		else if (target.is("button.x-date-mp-ok"))
		{
			DateWrapper d = new DateWrapper(mpSelYear, mpSelMonth, activeDate
			        .getDate());
			update(d);
			hideMonthPicker();
		}
		else if (target.is("button.x-date-mp-cancel"))
		{
			hideMonthPicker();
		}
		else if (target.is("a.x-date-mp-prev"))
		{
			updateMPYear(mpyear - 10);
		}
		else if (target.is("a.x-date-mp-next"))
		{
			updateMPYear(mpyear + 10);
		}

		if (GXT.isSafari)
		{
			focus();
		}
	}


	protected void onDayClick(ComponentEvent ce)
	{
		ce.stopEvent();
		El target = ce.getTargetEl();
		El e = target.findParent("a", 5);
		if (e != null)
		{
			String dt = e.dom.getPropertyString("dateValue");
			if (dt != null)
			{
				handleDateClick(e, dt);
				return;
			}
		}
	}


	@Override
	protected void onRender(Element target, int index)
	{
		setElement(DOM.createDiv(), target, index);

		if (messages.getMinText() == null)
		{
			messages.setMinText(GXT.MESSAGES.datePicker_minText());
		}
		if (messages.getMaxText() == null)
		{
			messages.setMaxText(GXT.MESSAGES.datePicker_maxText());
		}

		if (messages.getTodayText() == null)
		{
			messages.setTodayText(GXT.MESSAGES.datePicker_todayText());
		}

		header = new Header();
		header.render(getElement());

		days = new Grid(1, 7);
		days.setStyleName("x-date-days");
		days.setCellPadding(0);
		days.setCellSpacing(0);
		days.setBorderWidth(0);

		String[] dn = constants.narrowWeekdays();
		firstDOW = startDay != 0 ? startDay : Integer.parseInt(constants
		        .firstDayOfTheWeek()) - 1;

		days.setHTML(0, 0, "<span>" + dn[(0 + firstDOW) % 7] + "</span>");
		days.setHTML(0, 1, "<span>" + dn[(1 + firstDOW) % 7]);
		days.setHTML(0, 2, "<span>" + dn[(2 + firstDOW) % 7]);
		days.setHTML(0, 3, "<span>" + dn[(3 + firstDOW) % 7]);
		days.setHTML(0, 4, "<span>" + dn[(4 + firstDOW) % 7]);
		days.setHTML(0, 5, "<span>" + dn[(5 + firstDOW) % 7]);
		days.setHTML(0, 6, "<span>" + dn[(6 + firstDOW) % 7]);

		grid = new Grid(6, 7);
		grid.setStyleName("x-date-inner");
		grid.setCellSpacing(0);
		grid.setCellPadding(0);		
		grid.addClickHandler(new ClickHandler(){

            public void onClick(ClickEvent event)
            {
            	Event domEvent = DOM.eventGetCurrentEvent();
				ComponentEvent be = new ComponentEvent(DateTimePicker.this, domEvent);
				onDayClick(be); 
            }	
		});

		for (int row = 0; row < 6; row++)
		{
			for (int col = 0; col < 7; col++)
			{
				grid.setHTML(row, col, "<a href=#><span></span></a>");
			}
		}

		timePanel = new HorizontalPanel();
		timePanel.setHorizontalAlign(HorizontalAlignment.CENTER);
		timePanel.setStyleName("time-picker-time-panel");

		if (GXT.isIE)
		{
			timePanel.setWidth(175);
		}

		m_HoursSpinner = new ValueSpinner(new Date().getTime())
		{
			protected String formatValue(long value)
			{
				String hourText = DateTimeFormat.getFormat("HH").format(new Date(value));
				return hourText;
			}


			protected long parseValue(String value)
			{
				return DateTimeFormat.getFormat("HH").parse(value).getTime();
			}
		};// Min step = milliseconds per hour
		m_HoursSpinner.getSpinner().setMinStep(3600000);
		m_HoursSpinner.getTextBox().setWidth("25px");
		m_HoursSpinner.getSpinner().addSpinnerListener(new SpinnerListener(){
			public void onSpinning(long value)
			{
				handleTimeClick();
			}
		});

		m_MinsSpinner = new ValueSpinner(new Date().getTime())
		{
			protected String formatValue(long value)
			{
				return DateTimeFormat.getFormat("mm").format(new Date(value));
			}


			protected long parseValue(String value)
			{
				return DateTimeFormat.getFormat("mm").parse(value).getTime();
			}
		};// Min step = milliseconds per min
		m_MinsSpinner.getSpinner().setMinStep(60000);
		m_MinsSpinner.getTextBox().setWidth("25px");
		m_MinsSpinner.getSpinner().addSpinnerListener(new SpinnerListener(){
			public void onSpinning(long value)
			{
				handleTimeClick();
			}
		});

		LabelField spacerLeft = new LabelField(" ");
		spacerLeft.setWidth(40);

		timePanel.add(spacerLeft, new TableData(HorizontalAlignment.CENTER,
		        VerticalAlignment.MIDDLE));
		timePanel.add(m_HoursSpinner, new TableData(HorizontalAlignment.CENTER,
		        VerticalAlignment.MIDDLE));
		timePanel.add(new LabelField(":"), new TableData(
		        HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE));
		timePanel.add(m_MinsSpinner, new TableData(HorizontalAlignment.CENTER,
		        VerticalAlignment.MIDDLE));

		footer = new HorizontalPanel();
		footer.setTableWidth("100%");
		footer.setHorizontalAlign(HorizontalAlignment.CENTER);
		footer.setSpacing(2);

		footer.setStyleName("x-date-bottom");

		if (GXT.isIE)
		{
			footer.setWidth(175);
		}

		
		Button okBtn = new Button(myMessages.datePicker_okText());
		okBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{

			public void componentSelected(ButtonEvent ce)
			{
				DateTimePickerEvent de = new DateTimePickerEvent(
				        DateTimePicker.this);
				de.setDate(getValue());
				fireEvent(Events.Select, de);
			}

		});

		Button cancelBtn = new Button(myMessages.datePicker_cancelText());
		cancelBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{

			public void componentSelected(ButtonEvent ce)
			{
				DateTimePickerEvent de = new DateTimePickerEvent(
				        DateTimePicker.this);
				de.setDate(getValue());
				fireEvent(Events.CancelEdit, de);
			}

		});

		LabelField footerLeft = new LabelField(" ");
		footerLeft.setWidth(37);
		
		LabelField footerRight = new LabelField(" ");
		footerRight.setWidth(37);
		
		footer.add(footerLeft);
		footer.add(okBtn);
		footer.add(cancelBtn);
		footer.add(footerRight);

		monthPicker = new El(DOM.createDiv());
		monthPicker.dom.setClassName("x-date-mp");

		getElement().appendChild(header.getElement());
		getElement().appendChild(days.getElement());
		getElement().appendChild(grid.getElement());
		getElement().appendChild(timePanel.getElement());
		getElement().appendChild(footer.getElement());
		getElement().appendChild(monthPicker.dom);

		el().setWidth(175);

		cells = Util.toElementArray(el().select("table.x-date-inner tbody td"));
		textNodes = Util.toElementArray(el().select(
		        "table.x-date-inner tbody span"));

		activeDate = m_Value != null ? m_Value : new DateWrapper();
		update(activeDate);

		el().addEventsSunk(Event.ONCLICK);
		el().makePositionable();

	}


	private void createMonthPicker()
	{
		String ok = messages.getOkText() != null ? messages.getOkText()
		        : myMessages.datePicker_okText();
		String cancel = messages.getCancelText() != null ? messages
		        .getCancelText() : myMessages.datePicker_cancelText();

		StringBuffer buf = new StringBuffer();
		buf.append("<table border=0 cellspacing=0>");
		String[] monthNames = constants.shortMonths();
		for (int i = 0; i < 6; i++)
		{
			buf.append("<tr><td class=x-date-mp-month><a href=#>"
			        + monthNames[i] + "</a></td>");
			buf.append("<td class='x-date-mp-month x-date-mp-sep'><a href=#>"
			        + monthNames[i + 6] + "</a></td>");
			if (i == 0)
			{
				buf
				        .append("<td class=x-date-mp-ybtn align=center><a class=x-date-mp-prev href=#></a></td><td class='x-date-mp-ybtn' align=center><a class='x-date-mp-next'></a></td></tr>");
			}
			else
			{
				buf
				        .append("<td class='x-date-mp-year'><a href='#'></a></td><td class='x-date-mp-year'><a href='#'></a></td></tr>");
			}
		}
		buf
		        .append("<tr class=x-date-mp-btns><td colspan='4'><button type='button' class='x-date-mp-ok'>");
		buf.append(ok);
		buf.append("</button><button type=button class=x-date-mp-cancel>");
		buf.append(cancel);
		buf.append("</button></td></tr></table>");

		monthPicker.update(buf.toString());

		mpMonths = new CompositeElement(Util.toElementArray(monthPicker
		        .select("td.x-date-mp-month")));
		mpYears = new CompositeElement(Util.toElementArray(monthPicker
		        .select("td.x-date-mp-year")));

		mpMonths.each(new CompositeFunction()
		{

			public void doFunction(Element elem, CompositeElement ce, int index)
			{
				index += 1;
				if (index % 2 == 0)
				{
					elem.setPropertyInt("xmonth", (int) (5 + (Math
					        .round(index * .5))));
				}
				else
				{
					elem.setPropertyInt("xmonth", (int) (Math
					        .round((index - 1) * .5)));
				}
			}

		});

	}


	private void handleDateClick(El target, String dt)
	{
		Date d = new Date(Long.valueOf(dt));
		if (d != null && !target.getParent().hasStyleName("x-date-disabled"))
		{
			DateWrapper dateVal = new DateWrapper(d);
			dateVal.clearTime();
			
			// Add on the time portion set in the hours and mins spinners.
			long hoursValue = m_HoursSpinner.getSpinner().getValue();	
			DateWrapper hourWrapper = new DateWrapper(hoursValue);
			dateVal = dateVal.addHours(hourWrapper.getHours());
			
			long minsValue = m_MinsSpinner.getSpinner().getValue();
			DateWrapper minWrapper = new DateWrapper(minsValue);
			dateVal = dateVal.addMinutes(minWrapper.getMinutes());
			
			setValue(dateVal.asDate());
		}
	}
	
	
	private void handleTimeClick()
	{
		DateWrapper dateVal = new DateWrapper(m_Value.asDate()).clearTime();
		
		// Add on the time portion set in the hours and mins spinners.
		long hoursValue = m_HoursSpinner.getSpinner().getValue();	
		DateWrapper hourWrapper = new DateWrapper(hoursValue);
		dateVal = dateVal.addHours(hourWrapper.getHours());
		
		long minsValue = m_MinsSpinner.getSpinner().getValue();
		DateWrapper minWrapper = new DateWrapper(minsValue);
		dateVal = dateVal.addMinutes(minWrapper.getMinutes());
		
		setValue(dateVal.asDate());
	}


	private void hideMonthPicker()
	{
		monthPicker.slideOut(Direction.UP, new FxConfig(300, new Listener<FxEvent>()
        {
	        public void handleEvent(FxEvent ce)
	        {
		        monthPicker.setVisible(false);
	        }
        }));
	}


	private void selectToday()
	{
		setValue(new DateWrapper().asDate());
	}


	private void setCellStyle(Element cell, Date d, long sel, long min, long max)
	{
		long t = d.getTime();
		El cellEl = new El(cell);
		cellEl.firstChild().dom.setPropertyString("dateValue", "" + t);
		if (t == today)
		{
			cellEl.addStyleName("x-date-today");
			cellEl.setTitle(messages.getTodayText());
		}
		if (t == sel)
		{
			cellEl.addStyleName("x-date-selected");
		}
		if (t < min)
		{
			cellEl.addStyleName("x-date-disabled");
			cellEl.setTitle(messages.getMinText());
		}
		if (t > max)
		{
			cellEl.addStyleName("x-date-disabled");
			cellEl.setTitle(messages.getMaxText());
		}
	}


	private void showMonthPicker()
	{

		createMonthPicker();

		Size s = el().getSize(true);
		s.height -= 2;
		monthPicker.setTop(1);
		monthPicker.setSize(s.width, s.height);
		monthPicker.firstChild().setSize(s.width, s.height, true);

		mpSelMonth = (activeDate != null ? activeDate : m_Value).getMonth();

		updateMPMonth(mpSelMonth);
		mpSelYear = (activeDate != null ? activeDate : m_Value).getFullYear();
		updateMPYear(mpSelYear);

		monthPicker.enableDisplayMode("block");
		monthPicker.makePositionable(true);
		monthPicker.slideIn(Direction.DOWN, FxConfig.NONE);

	}


	private void showNextMonth()
	{
		update(activeDate.addMonths(1));
	}


	private void showPrevMonth()
	{
		update(activeDate.addMonths(-1));
	}


	private void update(DateWrapper date)
	{
		DateWrapper vd = activeDate;
		activeDate = date;
		if (vd != null && el() != null)
		{
			if (vd.getMonth() == activeDate.getMonth()
			        && vd.getFullYear() == activeDate.getFullYear())
			{

			}
			int days = date.getDaysInMonth();
			DateWrapper firstOfMonth = date.getFirstDayOfMonth();
			int startingPos = firstOfMonth.getDayInWeek() - firstDOW;

			if (startingPos <= startDay)
			{
				startingPos += 7;
			}

			DateWrapper pm = activeDate.addMonths(-1);
			int prevStart = pm.getDaysInMonth() - startingPos;

			days += startingPos;

			DateWrapper d = new DateWrapper(pm.getFullYear(), pm.getMonth(),
			        prevStart).clearTime();
			today = new DateWrapper().clearTime().getTime();
			long sel = activeDate.clearTime().getTime();
			long min = m_MinDate != null ? new DateWrapper(m_MinDate).getTime()
			        : Long.MIN_VALUE;
			long max = m_MaxDate != null ? new DateWrapper(m_MaxDate).getTime()
			        : Long.MAX_VALUE;

			int i = 0;
			for (; i < startingPos; i++)
			{
				fly(textNodes[i]).update("" + ++prevStart);
				d = d.addDays(1);
				cells[i].setClassName("x-date-prevday");
				setCellStyle(cells[i], d.asDate(), sel, min, max);
			}
			for (; i < days; i++)
			{
				int intDay = i - startingPos + 1;
				fly(textNodes[i]).update("" + intDay);
				d = d.addDays(1);
				cells[i].setClassName("x-date-active");
				setCellStyle(cells[i], d.asDate(), sel, min, max);
			}
			int extraDays = 0;
			for (; i < 42; i++)
			{
				fly(textNodes[i]).update("" + ++extraDays);
				d = d.addDays(1);
				cells[i].setClassName("x-date-nextday");
				setCellStyle(cells[i], d.asDate(), sel, min, max);
			}

			int month = activeDate.getMonth();
			monthBtn.setText(constants.standaloneMonths()[month] + " "
			        + activeDate.getFullYear());
			
			m_HoursSpinner.getSpinner().setValue(date.getTime(), false);
			m_HoursSpinner.getTextBox().setValue(DateTimeFormat.getFormat("HH").format(date.asDate()));
			
			m_MinsSpinner.getSpinner().setValue(date.getTime(), false);
			m_MinsSpinner.getTextBox().setValue(DateTimeFormat.getFormat("mm").format(date.asDate()));
		}
	}


	private void updateMPMonth(int month)
	{
		for (int i = 0; i < mpMonths.getCount(); i++)
		{
			Element elem = mpMonths.item(i);
			int xmonth = elem.getPropertyInt("xmonth");
			fly(elem).setStyleName("x-date-mp-sel", xmonth == month);
		}
	}


	private void updateMPYear(int year)
	{
		mpyear = year;

		for (int i = 1; i <= 10; i++)
		{
			El td = new El(mpYears.item(i - 1));
			int y2;
			if (i % 2 == 0)
			{
				y2 = (int) (year + (Math.round(i * .5)));
				td.firstChild().update("" + y2);
				td.dom.setPropertyInt("xyear", y2);
			}
			else
			{
				y2 = (int) (year - (5 - Math.round(i * .5)));
				td.firstChild().update("" + y2);
				td.dom.setPropertyInt("xyear", y2);
			}
			fly(mpYears.item(i - 1)).setStyleName("x-date-mp-sel", y2 == year);
		}
	}

	/**
	 * DatePicker messages.
	 */
	public class DatePickerMessages
	{
		private String todayText;
		private String okText = "&#160;OK&#160;";
		private String cancelText;
		private String todayTip;
		private String minText;
		private String maxText;
		private String prevText;
		private String nextText;
		private String monthYearText;


		/**
		 * Sets the text to display on the cancel button.
		 * 
		 * @return the cancel button text
		 */
		public String getCancelText()
		{
			return cancelText;
		}


		/**
		 * Returns the max text.
		 * 
		 * @return the max text
		 */
		public String getMaxText()
		{
			return maxText;
		}


		/**
		 * Returns the min text.
		 * 
		 * @return the min text
		 */
		public String getMinText()
		{
			return minText;
		}


		/**
		 * Returns the month year text.
		 * 
		 * @return the month year text
		 */
		public String getMonthYearText()
		{
			return monthYearText;
		}


		/**
		 * Returns the next text.
		 * 
		 * @return the next text
		 */
		public String getNextText()
		{
			return nextText;
		}


		/**
		 * Returns the ok text.
		 * 
		 * @return the ok text
		 */
		public String getOkText()
		{
			return okText;
		}


		/**
		 * Returns the prev text.
		 * 
		 * @return the prev text
		 */
		public String getPrevText()
		{
			return prevText;
		}


		/**
		 * Returns the today text.
		 * 
		 * @return the today text
		 */
		public String getTodayText()
		{
			return todayText;
		}


		/**
		 * Returns the today tip.
		 * 
		 * @return the tip
		 */
		public String getTodayTip()
		{
			return todayTip;
		}


		/**
		 * Sets the cance text (default to "Cancel").
		 * 
		 * @param cancelText
		 *            the cancel text
		 */
		public void setCancelText(String cancelText)
		{
			this.cancelText = cancelText;
		}


		/**
		 * Sets the error text to display if the maxDate validation fails
		 * (defaults to "This date is after the maximum date").
		 * 
		 * @param maxText
		 *            the max error text
		 */
		public void setMaxText(String maxText)
		{
			this.maxText = maxText;
		}


		/**
		 * Sets the error text to display if the minDate validation fails
		 * (defaults to "This date is before the minimum date").
		 * 
		 * @param minText
		 *            the min error text
		 */
		public void setMinText(String minText)
		{
			this.minText = minText;
		}


		/**
		 * Sets the header month selector tooltip (defaults to 'Choose a month
		 * (Control+Up/Down to move years)').
		 * 
		 * @param monthYearText
		 *            the month year text
		 */
		public void setMonthYearText(String monthYearText)
		{
			this.monthYearText = monthYearText;
		}


		/**
		 * Sets the next month navigation button tooltip (defaults to 'Next
		 * Month (Control+Right)').
		 * 
		 * @param nextText
		 *            the next text
		 */
		public void setNextText(String nextText)
		{
			this.nextText = nextText;
		}


		/**
		 * Sets the text to display on the ok button.
		 * 
		 * @param okText
		 *            the ok text
		 */
		public void setOkText(String okText)
		{
			this.okText = okText;
		}


		/**
		 * Sets the previous month navigation button tooltip (defaults to
		 * 'Previous Month (Control+Left)').
		 * 
		 * @param prevText
		 *            the prev text
		 */
		public void setPrevText(String prevText)
		{
			this.prevText = prevText;
		}


		/**
		 * Sets the text to display on the button that selects the current date
		 * (defaults to "Today").
		 * 
		 * @param todayText
		 *            the today text
		 */
		public void setTodayText(String todayText)
		{
			this.todayText = todayText;
		}


		/**
		 * Sets the tooltip to display for the button that selects the current
		 * date (defaults to "{current date} (Spacebar)").
		 * 
		 * @param todayTip
		 *            the today tool tip
		 */
		public void setTodayTip(String todayTip)
		{
			this.todayTip = todayTip;
		}

	}

	class Header extends Component
	{

		protected void doAttachChildren()
		{
			ComponentHelper.doAttach(monthBtn);
			ComponentHelper.doAttach(prevBtn);
			ComponentHelper.doAttach(nextBtn);
		}


		protected void doDetachChildren()
		{
			ComponentHelper.doDetach(monthBtn);
			ComponentHelper.doDetach(prevBtn);
			ComponentHelper.doDetach(nextBtn);
		}


		@Override
		protected void onRender(Element target, int index)
		{
			StringBuffer sb = new StringBuffer();
			sb.append("<table width=100% cellpadding=0 cellspacing=0><tr>");
			sb.append("<td class=x-date-left></td><td class=x-date-middle align=center></td>");
			sb.append("<td class=x-date-right></td></tr></table>");

			setElement(XDOM.create(sb.toString()));
			el().insertInto(target, index);

			el().setWidth(175, true);

			String pt = messages.getPrevText() != null ? messages.getPrevText()
			        : myMessages.datePicker_prevText();
			String nt = messages.getNextText() != null ? messages.getNextText()
			        : myMessages.datePicker_nextText();

			monthBtn = new Button("&#160;",
			        new SelectionListener<ButtonEvent>()
			        {
				        public void componentSelected(ButtonEvent ce)
				        {
					        showMonthPicker();
				        }
			        });

			monthBtn.render(el().selectNode(".x-date-middle").dom);
			monthBtn.el().child("tr").addStyleName("x-btn-with-menu");

			prevBtn = new IconButton("x-date-left-icon",
			        new SelectionListener<IconButtonEvent>()
			        {
				        public void componentSelected(IconButtonEvent ce)
				        {
					        showPrevMonth();
				        }
			        });
			prevBtn.setToolTip(pt);
			prevBtn.render(el().selectNode(".x-date-left").dom);

			nextBtn = new IconButton("x-date-right-icon",
			        new SelectionListener<IconButtonEvent>()
			        {
				        public void componentSelected(IconButtonEvent ce)
				        {
					        showNextMonth();
				        }
			        });
			nextBtn.setToolTip(nt);
			nextBtn.render(el().selectNode(".x-date-right").dom);
		}

	}
}
