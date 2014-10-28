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

package com.prelert.client.list;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.toolbar.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.DateTimePickerEvent;
import com.prelert.client.gxt.DateTimeMenu;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.DatePagingLoadConfig;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;


/**
 * A specialized toolbar for use in an Evidence Window, containing controls for
 * paging back and forward through the data.
 * <p>
 * It is bound to an {@link EvidencePagingLoader} and provides automatic paging
 * controls to navigate through the date range of evidence in the Prelert database,
 * plus a DateTimePicker for the selecting the date of the evidence displayed.
 * 
 * <dl>
 * <dt>Inherited Events:</dt>
 * <dd>Component Enable</dd>
 * <dd>Component Disable</dd>
 * <dd>Component BeforeHide</dd>
 * <dd>Component Hide</dd>
 * <dd>Component BeforeShow</dd>
 * <dd>Component Show</dd>
 * <dd>Component Attach</dd>
 * <dd>Component Detach</dd>
 * <dd>Component BeforeRender</dd>
 * <dd>Component Render</dd>
 * <dd>Component BrowserEvent</dd>
 * <dd>Component BeforeStateRestore</dd>
 * <dd>Component StateRestore</dd>
 * <dd>Component BeforeStateSave</dd>
 * <dd>Component SaveState</dd>
 * </dl>
 */
public class EvidenceViewPagingToolBar extends ToolBar
{
	protected EvidencePagingLoader m_Loader;
	protected DatePagingLoadConfig m_LoadConfig;

	protected String		m_DataType;
	protected TimeFrame 	m_TimeFrame;
	
	protected Date 		m_TopRowDate;	// Date of top row of current page.
	protected String 	m_TopRowId;		// id of top row of current page.
	protected Date 		m_BottomRowDate;// Date of bottom row of current page.
	protected String	m_BottomRowId;	// id of bottom row of current page.
	
	protected Date m_StartDate;		// Date of newest row of evidence in DB
	protected Date m_EndDate;		// Date of oldest row of evidence in DB

	protected Button m_FirstBtn;
	protected Button m_PrevBtn;
	protected Button m_TimePickerTool;
	protected DateTimeMenu m_DateMenu;
	protected Button m_NextBtn;
	protected Button m_LastBtn;
	protected Button m_RefreshBtn;
	protected LabelToolItem m_PageText;
	protected ListToolBarMessages m_Msgs;
	protected ListToolBarImages m_Images;
	protected boolean m_ShowToolTips = true;

	private boolean m_ReuseConfig = true;
	private LoadEvent m_RenderEvent;
	
	private RefreshTimer	m_RefreshTimer;
	private boolean			m_TimerStarted;
	
	protected LoadListener m_LoadListener;
	private boolean 		m_SavedEnableState = true;
	private Listener<ComponentEvent> m_EnableListener = new Listener<ComponentEvent>()
	{

		public void handleEvent(ComponentEvent be)
		{
			Component c = be.getComponent();
			if (be.getType() == Events.Disable)
			{
				if (c == m_FirstBtn)
				{
					m_FirstBtn.setIcon(getImages().getFirstDisabled());
				}
				else if (c == m_PrevBtn)
				{
					m_PrevBtn.setIcon(getImages().getPrevDisabled());
				}
				else if (c == m_NextBtn)
				{
					m_NextBtn.setIcon(getImages().getNextDisabled());
				}
				else if (c == m_LastBtn)
				{
					m_LastBtn.setIcon(getImages().getLastDisabled());
				}
			}
			else
			{
				if (c == m_FirstBtn)
				{
					m_FirstBtn.setIcon(getImages().getFirst());
				}
				else if (c == m_PrevBtn)
				{
					m_PrevBtn.setIcon(getImages().getPrev());
				}
				else if (c == m_NextBtn)
				{
					m_NextBtn.setIcon(getImages().getNext());
				}
				else if (c == m_LastBtn)
				{
					m_LastBtn.setIcon(getImages().getLast());
				}
			}
		}
	};
	
	
	/**
	 * Creates a new paging tool bar for a list of evidence data with the 
	 * specified data type and time frame.
	 * @param timeFrame time frame of the list of evidence e.g. SECOND, MINUTE, HOUR.
	 */
	public EvidenceViewPagingToolBar(TimeFrame timeFrame)
	{
		m_TimeFrame = timeFrame;
		
		m_Msgs = new ListToolBarMessages();
		
		m_FirstBtn = new Button();
		m_FirstBtn.addListener(Events.Disable, m_EnableListener);
		m_FirstBtn.addListener(Events.Enable, m_EnableListener);
		m_FirstBtn.setIcon(getImages().getFirst());
		if (m_ShowToolTips)
		{
			m_FirstBtn.setToolTip(m_Msgs.getFirstText());
		}
		m_FirstBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				first();
			}
		});

		m_PrevBtn = new Button();
		m_PrevBtn.addListener(Events.Disable, m_EnableListener);
		m_PrevBtn.addListener(Events.Enable, m_EnableListener);
		m_PrevBtn.setIcon(getImages().getPrev());
		if (m_ShowToolTips)
		{
			m_PrevBtn.setToolTip(m_Msgs.getPrevText());
		}
		m_PrevBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				previous();
			}
		});

		m_NextBtn = new Button();
		m_NextBtn.addListener(Events.Disable, m_EnableListener);
		m_NextBtn.addListener(Events.Enable, m_EnableListener);
		m_NextBtn.setIcon(getImages().getNext());
		if (m_ShowToolTips)
		{
			m_NextBtn.setToolTip(m_Msgs.getNextText());
		}
		m_NextBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				next();
			}
		});

		m_LastBtn = new Button();
		m_LastBtn.addListener(Events.Disable, m_EnableListener);
		m_LastBtn.addListener(Events.Enable, m_EnableListener);
		m_LastBtn.setIcon(getImages().getLast());
		if (m_ShowToolTips)
		{
			m_LastBtn.setToolTip(m_Msgs.getLastText());
		}
		m_LastBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				last();
			}
		});

		m_RefreshBtn = new Button();
		m_RefreshBtn.setIcon(getImages().getRefresh());
		if (m_ShowToolTips)
		{
			m_RefreshBtn.setToolTip(m_Msgs.getRefreshText());
		}
		m_RefreshBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				refresh();
			}
		});
		
		
		// Create the Date/Time picker tool.
		m_TimePickerTool = new Button();
		m_TimePickerTool.setIconStyle("icon-calendar");
		m_DateMenu = new DateTimeMenu();
		m_TimePickerTool.setMenu(m_DateMenu); 
		m_DateMenu.getDateTimePicker().addListener(Events.Select, new Listener<DateTimePickerEvent>()
		{
			public void handleEvent(DateTimePickerEvent e)
			{
				Date selectedDate = e.getDate();
				
				// Format the selected time so that the page that is returned will
				// show evidence up to and including the selected time.
				DateTimeFormat minFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy HH:mm:59");
				String timeAsText = minFormatter.format(selectedDate);
				DateTimeFormat reFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy HH:mm:ss");
				selectedDate = reFormatter.parse(timeAsText);

				moveToTime(selectedDate);
			}

		});


		m_PageText = new LabelToolItem();

		add(m_FirstBtn);
		add(m_PrevBtn);
		add(new SeparatorToolItem());
		add(m_PageText);
		add(m_TimePickerTool);
		add(new SeparatorToolItem());
		add(m_NextBtn);
		add(m_LastBtn);
		add(new SeparatorToolItem());
		add(m_RefreshBtn);

		add(new FillToolItem());
		
		// Test out refreshing every 15 secs.
		m_RefreshTimer = new RefreshTimer(15000);
	}


	/**
	 * Binds the toolbar to the loader.
	 * 
	 * @param loader the loader
	 */
	public void bind(EvidencePagingLoader loader)
	{
		if (m_Loader != null)
		{
			m_Loader.removeLoadListener(m_LoadListener);
		}
		m_Loader = loader;
		if (loader != null)
		{
			if (m_LoadListener == null)
			{
				m_LoadListener = new LoadListener()
				{
					@Override
                    public void loaderBeforeLoad(LoadEvent le)
					{
						m_SavedEnableState = isEnabled();
						setEnabled(false);
						m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-loading"));
					}


					@Override
                    public void loaderLoad(LoadEvent le)
					{
						m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
						setEnabled(m_SavedEnableState);
						onLoad(le);
					}


					@Override
                    public void loaderLoadException(LoadEvent le)
					{
						GWT.log("PagingToolBar Load exception: " + le.exception);
						
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
				                ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
						
						m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
						setEnabled(m_SavedEnableState);
					}
				};
			}
			loader.addLoadListener(m_LoadListener);
		}
	}


	/**
	 * Clears the current toolbar text.
	 */
	public void clear()
	{
		if (rendered)
		{
			m_PageText.setLabel("");
		}
	}


	/**
	 * Returns the tool bar's messages.
	 * 
	 * @return the messages
	 */
	public ListToolBarMessages getMessages()
	{
		return m_Msgs;
	}


	/**
	 * Returns the current time frame.
	 * 
	 * @return the time frame
	 */
	public TimeFrame getTimeFrame()
	{
		return m_TimeFrame;
	}


	public void handleEvent(BaseEvent be)
	{
		if (be.getType().equals(Loader.BeforeLoad))
		{
			disable();
			if (m_RefreshBtn != null)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-loading"));
			}
		}
		else if (be.getType().equals(Loader.Load))
		{
			m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
			onLoad((LoadEvent) be);
			enable();
		}
		else if (be.getType().equals(Loader.LoadException))
		{
			if (m_RefreshBtn != null)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
			}
			enable();
		}
	}


	/**
	 * Returns true if the previous load config is reused.
	 * 
	 * @return the reuse config state
	 */
	public boolean isReuseConfig()
	{
		return m_ReuseConfig;
	}


	/**
	 * Returns true if tooltip are enabled.
	 * 
	 * @return the show tooltip state
	 */
	public boolean isShowToolTips()
	{
		return m_ShowToolTips;
	}

	
	/**
	 * Moves to the first page.
	 */
	public void first()
	{
		m_Loader.loadFirstPage(m_StartDate, m_TimeFrame);
	}
	

	/**
	 * Moves to the last page.
	 */
	public void last()
	{
		m_Loader.loadLastPage(m_EndDate, m_TimeFrame);
	}


	/**
	 * Moves to the next page.
	 */
	public void next()
	{
		// Need to pass the time and id of the bottom row of the current page.
		m_Loader.loadNextPage(m_BottomRowDate, m_BottomRowId, m_TimeFrame);
	}


	/**
	 * Moves to the previous page.
	 */
	public void previous()
	{
		// Need to pass the time and id of the top row of the current page.
		m_Loader.loadPreviousPage(m_TopRowDate, m_TopRowId, m_TimeFrame);
	}


	/**
	 * Refreshes the data, loading up the first (most recent) page of evidence data.
	 */
	public void refresh()
	{
		// Just refresh the first page of evidence.
		first();
	}


	/**
	 * Moves to the page where the time of the top row will be equal to or earlier 
	 * than the specified date/time. If there is no data at or earlier than this time,
	 * then the first page of data at a later time will be loaded.
	 * @param date date/time for first row in the page to load.
	 */
	public void moveToTime(Date date)
	{
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		if ( (date != null) && (m_StartDate != null) && (date.after(m_StartDate)) )
		{
			first();
			return;
		}
		else if ( (date != null) && (m_EndDate != null) && (date.before(m_EndDate)) )
		{
			last();
			return;
		}
		else
		{
			m_Loader.loadAtTime(date, m_TimeFrame);
		}
	}
	
	
	/**
	 * Returns the date/time of the top row in the list.
	 * @return the date/time of the top row in the list, or <code>null</code>
	 * 		if no data is currently displayed.
	 */
	public Date getTopRowTime()
	{
		return m_TopRowDate;
	}


	/**
	 * Returns the images used in the toolbar.
	 * 
	 * @return the image
	 */
	public ListToolBarImages getImages()
	{
		if (m_Images == null)
		{
			m_Images = new ListToolBarImages();
		}
		return m_Images;
	}
	

	/**
	 * Sets the tool bar's messages.
	 * 
	 * @param messages
	 *            the messages
	 */
	public void setMessages(ListToolBarMessages messages)
	{
		m_Msgs = messages;
	}


	/**
	 * True to reuse the previous load config (defaults to true).
	 * 
	 * @param reuseConfig
	 *            true to reuse the load config
	 */
	public void setReuseConfig(boolean reuseConfig)
	{
		this.m_ReuseConfig = reuseConfig;
	}


	/**
	 * Sets if the button tool tips should be displayed (defaults to true,
	 * pre-render).
	 * 
	 * @param showToolTips
	 *            true to show tool tips
	 */
	public void setShowToolTips(boolean showToolTips)
	{
		this.m_ShowToolTips = showToolTips;
	}


	protected void onLoad(LoadEvent event)
	{
		if (!rendered)
		{
			m_RenderEvent = event;
			return;
		}
		m_LoadConfig = event.getConfig();

		DatePagingLoadResult<EvidenceModel> result = event.getData();

		m_TimeFrame = result.getTimeFrame();
		m_TopRowDate = result.getDate();
		m_StartDate = result.getStartDate();
		m_EndDate = result.getEndDate();
		
		m_PageText.setLabel(getPageText());

		// First/Previous buttons are always enabled as in normal operation the 
		// database is constantly being updated.
		// Next/Last buttons are enabled by comparison of the bottom row date
		// with the the date of the earliest record in the database.
		// Note that the first page is the LATEST date as it orders by time DESC.
		m_TopRowId = getTopRowId(result);
		m_BottomRowDate = getBottomRowTime(result);
		m_BottomRowId = getBottomRowId(result);
		
		enable();
		
		boolean enableLastNext = result.isEarlierResult();
		m_NextBtn.setEnabled(enableLastNext);
		m_LastBtn.setEnabled(enableLastNext);
		
		// Set the minimum date on the Date Time picker as the start of the earliest day.
		if (m_EndDate != null)
		{
			m_TimePickerTool.enable();
			
			DateWrapper endDateWrapper = new DateWrapper(m_EndDate);
			DateWrapper endDay = endDateWrapper.clearTime();
			m_DateMenu.getDateTimePicker().setMinDate(endDay.asDate());
			m_DateMenu.getDateTimePicker().setMaxDate(m_StartDate);
			
			if (m_TopRowDate != null)
			{
				m_DateMenu.getDateTimePicker().setValue(m_TopRowDate);
			}
		}
		else
		{
			m_TimePickerTool.disable();
		}
		
		
//		if (m_TimerStarted == false)
//		{
//			m_RefreshTimer.setRepeating(true);
//			m_TimerStarted = true;
//		}
	}


	@Override
	protected void onRender(Element target, int index)
	{
		super.onRender(target, index);

		if (m_RenderEvent != null)
		{
			onLoad(m_RenderEvent);
			m_RenderEvent = null;
		}
	}
	
	
	protected String getPageText()
	{
		if (m_TopRowDate == null || m_TimeFrame == null)
		{
			return ClientUtil.CLIENT_CONSTANTS.noData();
		}
		
		String text = new String();
		
		switch (m_TimeFrame)
		{
			case WEEK:
				DateTimeFormat weekStartFormatter = DateTimeFormat.getFormat("EEE dd");
				DateTimeFormat weekEndFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy");
				text += weekStartFormatter.format(m_TopRowDate);
				text += " - ";
				Date weekEndDate = new Date(m_TopRowDate.getTime() + (6*24*60*60*1000));
				text += weekEndFormatter.format(weekEndDate);
				break;
				
			case DAY:
				DateTimeFormat dayFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy");
				text += dayFormatter.format(m_TopRowDate);
				break;
				
			case HOUR:
				DateTimeFormat hourFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy HH:'00'");
				text += hourFormatter.format(m_TopRowDate);
				break;
				
			case MINUTE:
			case SECOND:
				DateTimeFormat minFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy HH:mm");
				text += minFormatter.format(m_TopRowDate);
				break;
		}
		
		return text;
	}
	
	
	/**
	 * Returns the id of the top row in the specified result set.
	 * @param result DatePagingLoadResult
	 * @return id of the top row, or <code>null</code> if the result set is empty.
	 */
	protected String getTopRowId(DatePagingLoadResult<EvidenceModel> result)
	{
		String id = null;
		List<EvidenceModel> evidence = result.getData();
		if (evidence != null && evidence.size() > 0)
		{
			EvidenceModel firstRow = evidence.get(0);
			switch (m_TimeFrame)
			{
				case SECOND:
					id = "" + firstRow.getId();
					break;
				case MINUTE:
				case HOUR:
				case DAY:
				case WEEK:
					id = firstRow.get("description");
					break;
					
			}
		}
		
		return id;
	}
	
	
	/**
	 * Returns the time of the bottom row in the specified load result.
	 * @param result the paging load result.
	 * @return the value of the time field in the bottom row, or <code>null</code> 
	 * if the data in the load result is <code>null</code> or empty.
	 */
	protected Date getBottomRowTime(DatePagingLoadResult<EvidenceModel> result)
	{
		List<EvidenceModel> evidence = result.getData();
		if (evidence == null || evidence.size() == 0)
		{
			return null;
		}
		
		EvidenceModel lastRow = evidence.get(evidence.size() - 1);		
		return lastRow.getTime(m_TimeFrame);
	}
	
	
	/**
	 * Returns the id of the bottom row in the specified result set.
	 * @param result DatePagingLoadResult
	 * @return id of the bottom row, or <code>null</code> if the result set is empty.
	 */
	protected String getBottomRowId(DatePagingLoadResult<EvidenceModel> result)
	{
		String id = null;
		List<EvidenceModel> evidence = result.getData();
		if (evidence != null && evidence.size() > 0)
		{
			EvidenceModel lastRow = evidence.get(evidence.size() - 1);
			switch (m_TimeFrame)
			{
				case SECOND:
					id = "" + lastRow.getId();
					break;
				case MINUTE:
				case HOUR:
				case DAY:
				case WEEK:
					id = lastRow.get("description");
					break;
			}
		}
		
		return id;
	}
	

	/**
	 * Timer class to provide automatic refresh.
	 */
	class RefreshTimer extends Timer
	{
		private int m_Frequency; // Refresh frequency, in millis.
		
		
		/**
		 * Creates a new RefreshTimer to automatically refresh at the specified frequency.
		 * @param frequencyMillis refresh frequency, in milliseconds.
		 */
		public RefreshTimer(int frequencyMillis)
		{
			m_Frequency = frequencyMillis;
		}
		
		
		/**
		 * Sets whether the timer should be elapsing repeatedly.
		 * @param repeating <code>true</code> to elapse repeatedly, 
		 * 		<code>false</code> to stop the timer.
		 */
		public void setRepeating(boolean repeating)
		{
			if (repeating == true)
			{
				this.scheduleRepeating(m_Frequency);
			}
			else
			{
				cancel();
			}
		}
		
		
        @Override
        public void run()
        {
        	refresh();
        }
	}
}

