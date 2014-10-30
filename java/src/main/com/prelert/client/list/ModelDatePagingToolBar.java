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

package com.prelert.client.list;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar.PagingToolBarImages;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar.PagingToolBarMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;

import com.prelert.client.ClientUtil;
import com.prelert.client.event.DateTimePickerEvent;
import com.prelert.client.gxt.DateTimeMenu;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.ModelDatePagingLoadConfig;


/**
 * A specialized toolbar bound to a {@link ModelDatePagingLoader} containing 
 * controls for paging back and forward through GXT model data ordered by time.
 * 
 * @author Pete Harverson
 * @param <M> the type of the model data being paged.
 */
public class ModelDatePagingToolBar<M extends ModelData> extends ToolBar
{
	protected ModelDatePagingLoader<M> 	m_Loader;
	protected ModelDatePagingLoadConfig m_LoadConfig;
	
	private String 		m_TimePropertyName;
	private String		m_RowIdPropertyName;
	
	protected Date 		m_TopRowDate;	// Date of top row of current page.
	protected int 		m_TopRowId;		// id of top row of current page.
	protected Date 		m_BottomRowDate;// Date of bottom row of current page.
	protected int		m_BottomRowId;	// id of bottom row of current page.
	
	protected Date 		m_StartDate;	// Date of newest model in DB
	protected Date 		m_EndDate;		// Date of oldest model in DB

	protected Button 		m_FirstBtn;
	protected Button 		m_PrevBtn;
	protected Button 		m_TimePickerTool;
	protected DateTimeMenu 	m_DateMenu;
	protected Button 		m_NextBtn;
	protected Button 		m_LastBtn;
	protected Button 		m_RefreshBtn;
	protected LabelToolItem m_PageText;
	
	protected PagingToolBarMessages m_Msgs;
	protected PagingToolBarImages 	m_Images;

	private boolean m_ReuseConfig = true;
	private LoadEvent m_RenderEvent;
	
	protected LoadListener 	m_LoadListener;
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
	
	protected static DateTimeFormat PAGE_TEXT_TIME_FORMAT = 
		DateTimeFormat.getFormat("EEE dd MMM yyyy HH:mm");
	
	
	/**
	 * Creates a new toolbar for paging through GXT ModelData ordered by time.
	 * @param timePropertyName name of the property that returns the time of 
	 * 	the model data.
	 * @param rowIdPropertyName name of the property that returns the unique row
	 * 	identifier.
	 */
	public ModelDatePagingToolBar(String timePropertyName, String rowIdPropertyName)
	{
		m_TimePropertyName = timePropertyName;
		m_RowIdPropertyName = rowIdPropertyName;
		
		m_FirstBtn = new Button();
		m_FirstBtn.addListener(Events.Disable, m_EnableListener);
		m_FirstBtn.addListener(Events.Enable, m_EnableListener);
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
		m_LastBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				last();
			}
		});

		m_RefreshBtn = new Button();
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
				// show data up to and including the selected time.
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
		
		// Customise tooltips on paging buttons.
		PagingToolBarMessages messages = new PagingToolBarMessages();
		messages.setFirstText(ClientUtil.CLIENT_CONSTANTS.newest());
		messages.setPrevText(ClientUtil.CLIENT_CONSTANTS.newer());
		messages.setNextText(ClientUtil.CLIENT_CONSTANTS.older());
		messages.setLastText(ClientUtil.CLIENT_CONSTANTS.oldest());
		
		setMessages(messages);
	    setImages(new PagingToolBarImages());
	}


	/**
	 * Binds the toolbar to the specified loader.
	 * @param loader the loader
	 */
	public void bind(ModelDatePagingLoader<M> loader)
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
						GWT.log("ModelDatePagingToolBar loaderLoadException: " + le.exception);
						
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
				                ClientUtil.CLIENT_CONSTANTS.errorLoadingPage(), null);
						
						m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
						setEnabled(m_SavedEnableState);
					}
				};
			}
			loader.addLoadListener(m_LoadListener);
		}
	}
	
	
	/**
	 * Returns the paging loader bound to the toolbar which makes calls to load
	 * data when paging operations are made via the toolbar's controls.
	 * @return the paging loader.
	 */
	public ModelDatePagingLoader<M> getLoader()
	{
		return m_Loader;
	}


	/**
	 * Returns the toolbar's messages.
	 * @return the messages.
	 */
	public PagingToolBarMessages getMessages()
	{
		return m_Msgs;
	}
	
	
	/**
	 * Sets the toolbar's messages.
	 * @param messages the messages
	 */
	public void setMessages(PagingToolBarMessages messages)
	{
		m_Msgs = messages;
		m_FirstBtn.setToolTip(m_Msgs.getFirstText());
		m_PrevBtn.setToolTip(m_Msgs.getPrevText());
		m_NextBtn.setToolTip(m_Msgs.getNextText());
		m_LastBtn.setToolTip(m_Msgs.getLastText());
		m_RefreshBtn.setToolTip(m_Msgs.getRefreshText());

		if (GXT.isAriaEnabled())
		{
			m_FirstBtn.getAriaSupport().setLabel(m_Msgs.getFirstText());
			m_PrevBtn.getAriaSupport().setLabel(m_Msgs.getPrevText());
			m_NextBtn.getAriaSupport().setLabel(m_Msgs.getNextText());
			m_LastBtn.getAriaSupport().setLabel(m_Msgs.getLastText());
			m_RefreshBtn.getAriaSupport().setLabel(m_Msgs.getRefreshText());
		}
	}
	
	
	/**
	 * Returns the images used in the toolbar.
	 * @return the images.
	 */
	public PagingToolBarImages getImages() 
	{
		if (m_Images == null)
		{
			m_Images = new PagingToolBarImages();
		}
		return m_Images;
	}


	/**
	 * Sets the toolbar images.
	 * @param images
	 */
	public void setImages(PagingToolBarImages images)
	{
		m_Images = images;
		m_RefreshBtn.setIcon(getImages().getRefresh());
		m_LastBtn.setIcon(m_LastBtn.isEnabled() ? getImages().getLast() : getImages().getLastDisabled());
		m_FirstBtn.setIcon(m_FirstBtn.isEnabled() ? getImages().getFirst() : getImages().getFirstDisabled());
		m_PrevBtn.setIcon(m_PrevBtn.isEnabled() ? getImages().getPrev() : getImages().getPrevDisabled());
		m_NextBtn.setIcon(m_NextBtn.isEnabled() ? getImages().getNext() : getImages().getNextDisabled());
	}
	
	
	/**
	 * True to reuse the previous load config (defaults to true).
	 * @param reuseConfig true to reuse the load config
	 */
	public void setReuseConfig(boolean reuseConfig)
	{
		m_ReuseConfig = reuseConfig;
	}


	/**
	 * Returns true if the previous load config is reused.
	 * @return the reuse config state
	 */
	public boolean isReuseConfig()
	{
		return m_ReuseConfig;
	}

	
	/**
	 * Moves to the first page.
	 */
	public void first()
	{
		m_Loader.loadFirstPage();
	}
	

	/**
	 * Moves to the last page.
	 */
	public void last()
	{
		m_Loader.loadLastPage();
	}


	/**
	 * Moves to the next page.
	 */
	public void next()
	{
		// Need to pass the time and id of the bottom row of the current page.
		m_Loader.loadNextPage(m_BottomRowDate, m_BottomRowId);
	}


	/**
	 * Moves to the previous page.
	 */
	public void previous()
	{
		// Need to pass the time and id of the top row of the current page.
		m_Loader.loadPreviousPage(m_TopRowDate, m_TopRowId);
	}


	/**
	 * Refreshes the data, loading up the first (most recent) page of data.
	 */
	public void refresh()
	{
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
			m_Loader.loadAtTime(date);
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
	

	protected void onLoad(LoadEvent event)
	{
		if (!rendered)
		{
			m_RenderEvent = event;
			return;
		}
		m_LoadConfig = event.getConfig();

		DatePagingLoadResult<BaseModelData> result = event.getData();

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
	
	
	/**
	 * Returns the text used to indicate the current page range.
	 * @return text showing the page start time.
	 */
	protected String getPageText()
	{
		if (m_TopRowDate == null)
		{
			return ClientUtil.CLIENT_CONSTANTS.noData();
		}
		
		return PAGE_TEXT_TIME_FORMAT.format(m_TopRowDate);
	}
	
	
	/**
	 * Returns the id of the top row in the specified result set.
	 * @param result DatePagingLoadResult
	 * @return id of the top row, or <code>0</code> if the result set is empty.
	 */
	protected int getTopRowId(DatePagingLoadResult<BaseModelData> result)
	{
		int id = 0;
		List<BaseModelData> dataList = result.getData();
		if (dataList != null && dataList.size() > 0)
		{
			BaseModelData firstRow = dataList.get(0);
			id = firstRow.get(m_RowIdPropertyName);
		}
		
		return id;
	}
	
	
	/**
	 * Returns the time of the bottom row in the specified load result.
	 * @param result the paging load result.
	 * @return the value of the time field in the bottom row, or <code>null</code> 
	 * if the data in the load result is <code>null</code> or empty.
	 */
	protected Date getBottomRowTime(DatePagingLoadResult<BaseModelData> result)
	{
		List<BaseModelData> dataList = result.getData();
		if (dataList == null || dataList.size() == 0)
		{
			return null;
		}
		
		BaseModelData lastRow = dataList.get(dataList.size() - 1);		
		return lastRow.get(m_TimePropertyName);
	}
	
	
	/**
	 * Returns the id of the bottom row in the specified result set.
	 * @param result DatePagingLoadResult
	 * @return id of the bottom row, or <code>0</code> if the result set is empty.
	 */
	protected int getBottomRowId(DatePagingLoadResult<BaseModelData> result)
	{
		int id = 0;
		List<BaseModelData> dataList = result.getData();
		if (dataList != null && dataList.size() > 0)
		{
			BaseModelData lastRow = dataList.get(dataList.size() - 1);
			id = lastRow.get(m_RowIdPropertyName);
		}
		
		return id;
	}
}
