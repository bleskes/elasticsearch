/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
import com.extjs.gxt.ui.client.widget.form.SpinnerField;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DefaultDateTimeFormatInfo;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Grid;

import com.prelert.client.event.DateTimePickerEvent;


/**
 * A date/time picker. The component consists of a calendar grid allowing the
 * user to select a date, and a time field consisting of two spinner controls
 * for setting the hours and minutes portion of the time.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>Select</b> : DateTimePickerEvent<br>
 * <div>Fires when a date is selected.</div>
 * <ul>
 * <li>datePicker : this</li>
 * <li>date : the selected date</li>
 * </ul>
 * </dd>
 * </dl>
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
	
	private SpinnerField m_HoursSpinner;
	private SpinnerField m_MinsSpinner;
	
	private DateWrapper activeDate, m_Value;
	private int mpSelMonth, mpSelYear;
	private Button monthBtn;
	private Element[] cells;
	private Element[] textNodes;
	private IconButton prevBtn, nextBtn;
	private CompositeElement mpMonths, mpYears;
	private El monthPicker;
	
	private DefaultDateTimeFormatInfo m_DateFormatInfo;
	private XMessages m_GXTMessages = (XMessages) GWT.create(XMessages.class);
	private DatePickerMessages m_DatePickerMessages;


	/**
	 * Creates a new date and time picker.
	 */
	public DateTimePicker()
	{
		baseStyle = "x-date-picker";
		m_DatePickerMessages = new DatePickerMessages();
		m_DateFormatInfo = new DefaultDateTimeFormatInfo();

		// Initialise date value with current time.
		m_Value = new DateWrapper();
	}

	/*
	// DO NOT override focus() as in GXT com.extjs.gxt.ui.client.widget.DatePicker
	// - does not seem to be a need for it, plus on Safari focus() is called after
	// clicking the spin buttons, reverting the time straight back to its previously
	// set value.
	@Override
	public void focus()
	{
		super.focus();
		update(activeDate);
	}
	*/


	/**
	 * Returns the field's maximum allowed date.
	 * @return the max date
	 */
	public Date getMaxDate()
	{
		return m_MaxDate;
	}
	
	
	/**
	 * Sets the picker's maximum allowed date.
	 * @param maxDate  the max date
	 */
	public void setMaxDate(Date maxDate)
	{
		if (maxDate != null)
		{
			maxDate = new DateWrapper(maxDate).clearTime().asDate();
		}
		m_MaxDate = maxDate;
	}
	
	
	/**
	 * Returns the picker's minimum allowed date.
	 * 
	 * @return the min date
	 */
	public Date getMinDate()
	{
		return m_MinDate;
	}
	
	
	/**
	 * Sets the picker's minimum allowed date.
	 * @param minDate the minimum date
	 */
	public void setMinDate(Date minDate)
	{
		if (minDate != null)
		{
			minDate = new DateWrapper(minDate).clearTime().asDate();
		}
		m_MinDate = minDate;
	}
	
	
	/**
	 * Sets the picker's start day
	 * @param startDay the start day
	 */
	public void setStartDay(int startDay)
	{
		this.startDay = startDay;
	}


	/**
	 * Returns the picker's start day.
	 * @return the start day
	 */
	public int getStartDay()
	{
		return startDay;
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
	 * Returns the date picker messages.
	 * @return the date picker messages
	 */
	public DatePickerMessages getMessages()
	{
		return m_DatePickerMessages;
	}
	

	/**
	 * Sets the data picker messages.
	 * @param messages the date picker messages
	 */
	public void setMessages(DatePickerMessages messages)
	{
		this.m_DatePickerMessages = messages;
	}
	
	
	/**
	 * Returns the date/time currently set in the picker.
	 * @return the date/time in the picker.
	 */
	public Date getValue()
	{
		return m_Value.asDate();
	}


	/**
	 * Sets the date/time currently in the picker.
	 * @param date the date/time.
	 */
	public void setValue(Date date)
	{
		setValue(date, false);
	}


	/**
	 * Sets the date/time currently in the picker.
	 * @param date the date
	 * @param supressEvent true to suppress events.
	 */
	public void setValue(Date date, boolean supressEvent)
	{
		m_Value = new DateWrapper(date);

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
			DateWrapper d = new DateWrapper(mpSelYear, mpSelMonth, activeDate.getDate());
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

		if (m_DatePickerMessages.getMinText() == null)
		{
			m_DatePickerMessages.setMinText(GXT.MESSAGES.datePicker_minText());
		}
		if (m_DatePickerMessages.getMaxText() == null)
		{
			m_DatePickerMessages.setMaxText(GXT.MESSAGES.datePicker_maxText());
		}

		if (m_DatePickerMessages.getTodayText() == null)
		{
			m_DatePickerMessages.setTodayText(GXT.MESSAGES.datePicker_todayText());
		}

		header = new Header();
		header.render(getElement());

		days = new Grid(1, 7);
		days.setStyleName("x-date-days");
		days.setCellPadding(0);
		days.setCellSpacing(0);
		days.setBorderWidth(0);

		firstDOW = startDay != 0 ? startDay : m_DateFormatInfo.firstDayOfTheWeek() - 1;

		String[] dn = m_DateFormatInfo.weekdaysNarrow();
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

		// Add the hour and minute spinner fields.
		m_HoursSpinner = new TimeSpinnerField();
		m_HoursSpinner.setPropertyEditorType(Integer.class);
		m_HoursSpinner.setWidth(40);
		m_HoursSpinner.setAllowDecimals(false);
		m_HoursSpinner.setAllowNegative(false);
		m_HoursSpinner.setMinValue(0);
		m_HoursSpinner.setMaxValue(23);
		m_HoursSpinner.setFormat(NumberFormat.getFormat("00"));
		
		m_MinsSpinner = new TimeSpinnerField();
		m_MinsSpinner.setPropertyEditorType(Integer.class);
		m_MinsSpinner.setWidth(40);
		m_MinsSpinner.setAllowDecimals(false);
		m_MinsSpinner.setAllowNegative(false);
		m_MinsSpinner.setMinValue(0);
		m_MinsSpinner.setMaxValue(59);
		m_MinsSpinner.setFormat(NumberFormat.getFormat("00"));
		
		Listener<FieldEvent> timeChangeListener = new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent be)
            {
	            handleTimeClick();
            }
		};
		
		m_HoursSpinner.addListener(Events.Change, timeChangeListener);
		m_MinsSpinner.addListener(Events.Change, timeChangeListener);


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

		
		Button okBtn = new Button(m_GXTMessages.datePicker_okText());
		okBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{

			@Override
            public void componentSelected(ButtonEvent ce)
			{
				DateTimePickerEvent de = new DateTimePickerEvent(DateTimePicker.this);
				de.setDate(getValue());
				fireEvent(Events.Select, de);
			}

		});

		Button cancelBtn = new Button(m_GXTMessages.datePicker_cancelText());
		cancelBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{

			@Override
            public void componentSelected(ButtonEvent ce)
			{
				DateTimePickerEvent de = new DateTimePickerEvent(DateTimePicker.this);
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
		String ok = m_DatePickerMessages.getOkText() != null ? m_DatePickerMessages.getOkText()
		        : m_GXTMessages.datePicker_okText();
		String cancel = m_DatePickerMessages.getCancelText() != null ? m_DatePickerMessages
		        .getCancelText() : m_GXTMessages.datePicker_cancelText();

		StringBuffer buf = new StringBuffer();
		buf.append("<table border=0 cellspacing=0>");
		String[] monthNames = m_DateFormatInfo.monthsShort();
		for (int i = 0; i < 6; i++)
		{
			buf.append("<tr><td class=x-date-mp-month><a href=#>" + monthNames[i] + "</a></td>");
			buf.append("<td class='x-date-mp-month x-date-mp-sep'><a href=#>" + monthNames[i + 6] + "</a></td>");
			if (i == 0)
			{
				buf.append("<td class=x-date-mp-ybtn align=center><a class=x-date-mp-prev href=#></a></td><td class='x-date-mp-ybtn' align=center><a class='x-date-mp-next'></a></td></tr>");
			}
			else
			{
				buf.append("<td class='x-date-mp-year'><a href='#'></a></td><td class='x-date-mp-year'><a href='#'></a></td></tr>");
			}
		}
		buf.append("<tr class=x-date-mp-btns><td colspan='4'><button type='button' class='x-date-mp-ok'>");
		buf.append(ok);
		buf.append("</button><button type=button class=x-date-mp-cancel>");
		buf.append(cancel);
		buf.append("</button></td></tr></table>");

		monthPicker.update(buf.toString());

		mpMonths = new CompositeElement(Util.toElementArray(monthPicker.select("td.x-date-mp-month")));
		mpYears = new CompositeElement(Util.toElementArray(monthPicker.select("td.x-date-mp-year")));

		mpMonths.each(new CompositeFunction()
		{

			public void doFunction(Element elem, CompositeElement ce, int index)
			{
				index += 1;
				if (index % 2 == 0)
				{
					elem.setPropertyInt("xmonth", (int) (5 + (Math.round(index * .5))));
				}
				else
				{
					elem.setPropertyInt("xmonth", (int) (Math.round((index - 1) * .5)));
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
			
			// Add on the time portion set in the hour and minute spinners.
			Number hoursValue = m_HoursSpinner.getValue();
			dateVal = dateVal.addHours(hoursValue.intValue());
			
			Number minsValue = m_MinsSpinner.getValue();
			dateVal = dateVal.addMinutes(minsValue.intValue());
			
			setValue(dateVal.asDate());
		}
	}
	
	
	private void handleTimeClick()
	{
		DateWrapper dateVal = new DateWrapper(m_Value.asDate()).clearTime();
		
		// Add on the time portion set in the hours and mins spinners.
		Number hoursValue = m_HoursSpinner.getValue();
		dateVal = dateVal.addHours(hoursValue.intValue());
		
		Number minsValue = m_MinsSpinner.getValue();
		dateVal = dateVal.addMinutes(minsValue.intValue());
		
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


	private void setCellStyle(Element cell, Date d, long sel, long min, long max)
	{
		long t = d.getTime();
		El cellEl = new El(cell);
		cellEl.firstChild().dom.setPropertyString("dateValue", "" + t);
		if (t == today)
		{
			cellEl.addStyleName("x-date-today");
			cellEl.setTitle(m_DatePickerMessages.getTodayText());
		}
		if (t == sel)
		{
			cellEl.addStyleName("x-date-selected");
		}
		if (t < min)
		{
			cellEl.addStyleName("x-date-disabled");
			cellEl.setTitle(m_DatePickerMessages.getMinText());
		}
		if (t > max)
		{
			cellEl.addStyleName("x-date-disabled");
			cellEl.setTitle(m_DatePickerMessages.getMaxText());
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
			if (vd.getMonth() == activeDate.getMonth() && 
					vd.getFullYear() == activeDate.getFullYear())
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
			long min = m_MinDate != null ? new DateWrapper(m_MinDate).getTime() : Long.MIN_VALUE;
			long max = m_MaxDate != null ? new DateWrapper(m_MaxDate).getTime() : Long.MAX_VALUE;

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
			monthBtn.setText(m_DateFormatInfo.monthsFullStandalone()[month] + " " + activeDate.getFullYear());
			
			m_HoursSpinner.enableEvents(false);
			m_MinsSpinner.enableEvents(false);
			
			m_HoursSpinner.setValue(date.getHours());
			m_MinsSpinner.setValue(date.getMinutes());
			
			m_HoursSpinner.enableEvents(true);
			m_MinsSpinner.enableEvents(true);
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
		 * @return the cancel button text
		 */
		public String getCancelText()
		{
			return cancelText;
		}


		/**
		 * Returns the max text.
		 * @return the max text
		 */
		public String getMaxText()
		{
			return maxText;
		}


		/**
		 * Returns the min text.
		 * @return the min text
		 */
		public String getMinText()
		{
			return minText;
		}


		/**
		 * Returns the month year text.
		 * @return the month year text
		 */
		public String getMonthYearText()
		{
			return monthYearText;
		}


		/**
		 * Returns the next text.
		 * @return the next text
		 */
		public String getNextText()
		{
			return nextText;
		}


		/**
		 * Returns the ok text.
		 * @return the ok text
		 */
		public String getOkText()
		{
			return okText;
		}


		/**
		 * Returns the prev text.
		 * @return the prev text
		 */
		public String getPrevText()
		{
			return prevText;
		}


		/**
		 * Returns the today text.
		 * @return the today text
		 */
		public String getTodayText()
		{
			return todayText;
		}


		/**
		 * Returns the today tip.
		 * @return the tip
		 */
		public String getTodayTip()
		{
			return todayTip;
		}


		/**
		 * Sets the cancel text (default to "Cancel").
		 * @param cancelText the cancel text
		 */
		public void setCancelText(String cancelText)
		{
			this.cancelText = cancelText;
		}


		/**
		 * Sets the error text to display if the maxDate validation fails
		 * (defaults to "This date is after the maximum date").
		 * @param maxText the max error text
		 */
		public void setMaxText(String maxText)
		{
			this.maxText = maxText;
		}


		/**
		 * Sets the error text to display if the minDate validation fails
		 * (defaults to "This date is before the minimum date").
		 * @param minText the min error text
		 */
		public void setMinText(String minText)
		{
			this.minText = minText;
		}


		/**
		 * Sets the header month selector tooltip (defaults to 'Choose a month
		 * (Control+Up/Down to move years)').
		 * @param monthYearText the month year text
		 */
		public void setMonthYearText(String monthYearText)
		{
			this.monthYearText = monthYearText;
		}


		/**
		 * Sets the next month navigation button tooltip (defaults to 'Next
		 * Month (Control+Right)').
		 * @param nextText the next text
		 */
		public void setNextText(String nextText)
		{
			this.nextText = nextText;
		}


		/**
		 * Sets the text to display on the ok button.
		 * @param okText the ok text
		 */
		public void setOkText(String okText)
		{
			this.okText = okText;
		}


		/**
		 * Sets the previous month navigation button tooltip (defaults to
		 * 'Previous Month (Control+Left)').
		 * @param prevText the prev text
		 */
		public void setPrevText(String prevText)
		{
			this.prevText = prevText;
		}


		/**
		 * Sets the text to display on the button that selects the current date
		 * (defaults to "Today").
		 * @param todayText the today text
		 */
		public void setTodayText(String todayText)
		{
			this.todayText = todayText;
		}


		/**
		 * Sets the tooltip to display for the button that selects the current
		 * date (defaults to "{current date} (Spacebar)").
		 * @param todayTip the today tool tip
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

			String pt = m_DatePickerMessages.getPrevText() != null ? m_DatePickerMessages.getPrevText() : 
							m_GXTMessages.datePicker_prevText();
			String nt = m_DatePickerMessages.getNextText() != null ? m_DatePickerMessages.getNextText() : 
							m_GXTMessages.datePicker_nextText();

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
	
	
	/**
	 * Extension of the GXT SpinnerField to provide wrapping on min/max
	 * values on the spin arrow buttons.
	 */
	class TimeSpinnerField extends SpinnerField
	{
		protected void doSpin(boolean up)
		{
			int value = getValue().intValue();
			if (up)
			{
				if (value == getMaxValue().intValue())
				{
					setValue(getMinValue());
				}
				else
				{
					super.doSpin(up);
				}
			}
			else
			{
				if (value == getMinValue().intValue())
				{
					setValue(getMaxValue());
				}
				else
				{
					super.doSpin(up);
				}
			}
		}
	}
}
