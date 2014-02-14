package demo.app.client;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.messages.XMessages;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.toolbar.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;

import demo.app.data.*;

/**
 * A specialized toolbar for use in a window displaying the history of an item
 * of evidence. It is bound to a {@link EvidenceViewPagingLoader} and provides
 * automatic paging controls to navigate through the history of the piece of
 * evidence. The toolbar contains a DateTimePicker for the selecting the date of
 * the evidence displayed, and a ComboBox control for selecting the time frame of
 * the evidence data i.e. WEEK, DAY, HOUR, MINUTE or real-time (SECOND).
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
public class EvidenceHistoryPagingToolBar extends Component implements Listener
{
	protected EvidenceHistoryWindow 		m_HistoryWindow;
	protected EvidenceViewPagingLoader 		m_Loader;
	protected DatePagingLoadConfig 			m_LoadConfig;

	protected TimeFrame	m_TimeFrame;
	protected TimeFrame[] m_TimeFrameOptions;
	
	protected Date 		m_TopRowDate;	// Date of top row of current page.
	protected String 	m_TopRowId;		// id of top row of current page.
	protected Date 		m_BottomRowDate;// Date of bottom row of current page.
	protected String	m_BottomRowId;	// id of bottom row of current page.
	
	protected Date m_StartDate;		// Date of newest row of evidence in DB
	protected Date m_EndDate;		// Date of oldest row of evidence in DB

	protected ToolBar 				m_ToolBar;
	protected ComboBox<BaseModelData> 	m_TimeFrameCombo;
	private SelectionChangedListener<BaseModelData>	m_TimeFrameComboListener;
	protected Button 				m_FirstTool;
	protected Button 				m_PrevTool;
	protected Button 				m_TimePickerTool;
	protected DateTimeMenu 			m_DateMenu;
	protected Button 				m_NextTool;
	protected Button 				m_LastTool;
	protected Button 				m_RefreshTool;
	protected LabelToolItem 		m_PageText;
	protected PagingToolBarMessages m_Msgs;
	protected boolean 				m_ShowToolTips = true;

	private boolean m_ReuseConfig = true;
	private LoadEvent 				m_RenderEvent;


	/**
	 * Creates a new time frame paging tool bar, with the specified initial time frame.
	 * 
	 * @param timeFrame time frame to use when first rendered.
	 */
	public EvidenceHistoryPagingToolBar(EvidenceHistoryWindow historyWindow)
	{
		m_HistoryWindow = historyWindow;
		m_TimeFrame = historyWindow.getTimeFrame();
		m_Msgs = new PagingToolBarMessages();
		m_TimeFrameOptions = new TimeFrame[]{TimeFrame.DAY, 
				TimeFrame.HOUR, TimeFrame.MINUTE, TimeFrame.SECOND};
	}


	/**
	 * Binds the toolbar to the loader.
	 * 
	 * @param loader
	 *            the loader
	 */
	public void bind(EvidenceViewPagingLoader loader)
	{
		if (m_Loader != null)
		{
			m_Loader.removeListener(Loader.BeforeLoad, this);
			m_Loader.removeListener(Loader.Load, this);
			m_Loader.removeListener(Loader.LoadException, this);
		}
		m_Loader = loader;

		if (loader != null)
		{
			loader.setTimeFrame(m_TimeFrame);
			loader.addListener(Loader.BeforeLoad, this);
			loader.addListener(Loader.Load, this);
			loader.addListener(Loader.LoadException, this);
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
	 * Moves to the first page.
	 */
	public void first()
	{
		m_Loader.loadFirstPage(m_StartDate, m_TimeFrame);
	}
	
	
	/**
	 * Moves the the previous page.
	 */
	public void previous()
	{
		// Need to pass the time and id of the top row of the current page.
		m_Loader.loadPreviousPage(m_TopRowDate, m_TopRowId, m_TimeFrame);
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
	 * Moves to the last page.
	 */
	public void last()
	{
		m_Loader.loadLastPage(m_EndDate, m_TimeFrame);
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
	 * Sets the active page.
	 * @param date date/time for first row in the active page.
	 */
	public void setActivePage(Date date)
	{
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		if (date.after(m_StartDate))
		{
			first();
			return;
		}
		else if (date.before(m_EndDate))
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
	 * Returns the tool bar's messages.
	 * 
	 * @return the messages
	 */
	public PagingToolBarMessages getMessages()
	{
		return m_Msgs;
	}
	
	
	/**
	 * Sets the time frame.
	 * @return the time frame
	 */
	public void setTimeFrame(TimeFrame timeFrame)
	{
		m_TimeFrame = timeFrame;
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
			if (m_RefreshTool != null)
			{
				m_RefreshTool.setIcon(IconHelper.createStyle("x-tbar-loading"));
			}
		}
		else if (be.getType().equals(Loader.Load))
		{
			m_RefreshTool.setIcon(IconHelper.createStyle("x-tbar-refresh"));
			onLoad((LoadEvent) be);
			enable();
		}
		else if (be.getType().equals(Loader.LoadException))
		{
			GWT.log("EvidenceHistoryPagingToolBar handleEvent() LoadException: " + ((LoadEvent)be).exception, null);
			m_RefreshTool.setIcon(IconHelper.createStyle("x-tbar-refresh"));
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
	 * Sets the tool bar's messages.
	 * 
	 * @param messages
	 *            the messages
	 */
	public void setMessages(PagingToolBarMessages messages)
	{
		m_Msgs = messages;
	}


	/**
	 * Returns the choice time frame options for the toolbar.
     * @return array of TimeFrames which are available in this toolbar.
     */
    public TimeFrame[] getTimeFrameOptions()
    {
    	return m_TimeFrameOptions;
    }


	/**
	 * Sets the choice time frame options for the toolbar.
     * @param timeFrameOptions array of TimeFrames which will be available 
     * for selection in this toolbar.
     */
    public void setTimeFrameOptions(TimeFrame[] timeFrameOptions)
    {
    	m_TimeFrameOptions = timeFrameOptions;
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


	protected void doAttachChildren()
	{
		ComponentHelper.doAttach(m_ToolBar);
	}


	protected void doDetachChildren()
	{
		ComponentHelper.doDetach(m_ToolBar);
	}


	protected void onLoad(LoadEvent event)
	{
		if (!rendered)
		{
			m_RenderEvent = event;
			return;
		}
		m_LoadConfig = event.getConfig();

		DatePagingLoadResult<EventRecord> result = event.getData();

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
		
		//m_FirstTool.setEnabled(m_TopRowDate.before(m_StartDate));
		//m_PrevTool.setEnabled(m_TopRowDate.before(m_StartDate));
		
		boolean enableLastNext = 
			( (m_BottomRowDate != null) && (m_BottomRowDate.after(m_EndDate)) );
		m_NextTool.setEnabled(enableLastNext);
		m_LastTool.setEnabled(enableLastNext);
		
		// Set the minimum date on the Date Time picker as the start of the earliest day.
		if (m_EndDate != null)
		{
			m_TimePickerTool.enable();
			
			DateWrapper endDateWrapper = new DateWrapper(m_EndDate);
			DateWrapper endDay = endDateWrapper.clearTime();
			m_DateMenu.getDateTimePicker().setMinDate(endDay.asDate());
			m_DateMenu.getDateTimePicker().setMaxDate(m_StartDate);
		}
		else
		{
			m_TimePickerTool.disable();
		}
		
		setTimeFrameSelection(m_TimeFrame);
	}


	@Override
	protected void onRender(Element target, int index)
	{
		XMessages msg = GXT.MESSAGES;

		m_Msgs.setRefreshText(m_Msgs.getRefreshText() == null ? 
				msg.pagingToolBar_refreshText() : m_Msgs.getRefreshText());
		m_Msgs.setNextText(m_Msgs.getNextText() == null ?
				msg.pagingToolBar_nextText() : m_Msgs.getNextText());
		m_Msgs.setPrevText(m_Msgs.getPrevText() == null ? 
				msg.pagingToolBar_prevText() : m_Msgs.getPrevText());
		m_Msgs.setFirstText(m_Msgs.getFirstText() == null ? 
				msg.pagingToolBar_firstText() : m_Msgs.getFirstText());
		m_Msgs.setLastText(m_Msgs.getLastText() == null ? 
				msg.pagingToolBar_lastText() : m_Msgs.getLastText());
		m_Msgs.setBeforePageText(m_Msgs.getBeforePageText() == null ? 
				msg.pagingToolBar_beforePageText() : m_Msgs.getBeforePageText());
		m_Msgs.setEmptyMsg(m_Msgs.getEmptyMsg() == null ? 
				msg.pagingToolBar_emptyMsg() : m_Msgs.getEmptyMsg());

		m_ToolBar = new ToolBar();

		m_FirstTool = new Button();
		m_FirstTool.setIconStyle("x-tbar-page-first");
		if (m_ShowToolTips)
			m_FirstTool.setToolTip(m_Msgs.getFirstText());
		m_FirstTool.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			public void componentSelected(ButtonEvent ce)
			{
				first();
			}
		});

		m_PrevTool = new Button();
		m_PrevTool.setIconStyle("x-tbar-page-prev");
		if (m_ShowToolTips)
			m_PrevTool.setToolTip(m_Msgs.getPrevText());
		m_PrevTool.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			public void componentSelected(ButtonEvent ce)
			{
				previous();
			}
		});

		m_NextTool = new Button();
		m_NextTool.setIconStyle("x-tbar-page-next");
		if (m_ShowToolTips)
			m_NextTool.setToolTip(m_Msgs.getNextText());
		m_NextTool.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			public void componentSelected(ButtonEvent ce)
			{
				next();
			}
		});

		m_LastTool = new Button();
		m_LastTool.setIconStyle("x-tbar-page-last");
		if (m_ShowToolTips)
			m_LastTool.setToolTip(m_Msgs.getLastText());
		m_LastTool.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			public void componentSelected(ButtonEvent ce)
			{
				last();
			}
		});

		m_RefreshTool = new Button();
		m_RefreshTool.setIconStyle("x-tbar-loading");
		if (m_ShowToolTips)
			m_RefreshTool.setToolTip(m_Msgs.getRefreshText());
		m_RefreshTool.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			public void componentSelected(ButtonEvent ce)
			{
				refresh();
			}
		});
		
		m_TimePickerTool = new Button();
		m_TimePickerTool.setIconStyle("icon-calendar");
		m_DateMenu = new DateTimeMenu();
		m_TimePickerTool.setMenu(m_DateMenu);  
		m_DateMenu.getDateTimePicker().addListener(Events.Select, new Listener<ComponentEvent>()
		{
			public void handleEvent(ComponentEvent be)
			{
				Date selectedDate = m_DateMenu.getDate();
				
				// Format the selected time so that the page that is returned will
				// show evidence up to and including the selected time.
				switch (m_TimeFrame)
				{
					case SECOND:
						DateTimeFormat minFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy HH:mm:59");
						String timeAsText = minFormatter.format(selectedDate);
						DateTimeFormat reFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy HH:mm:ss");
						selectedDate = reFormatter.parse(timeAsText);
						break;
					default:
						break;
				}
				setActivePage(selectedDate);
			}

		});

		LabelToolItem beforePage = new LabelToolItem(m_Msgs.getBeforePageText());
		beforePage.setStyleName("my-paging-text");
		
		m_PageText = new LabelToolItem();
		m_PageText.setWidth("30px");
		
		// Create a ComboBox for setting the time frame e.g. Week, Daily, Hourly.
		m_TimeFrameCombo = new ComboBox<BaseModelData>();
		m_TimeFrameCombo.setEditable(false);
		m_TimeFrameCombo.setListStyle("prelert-combo-list");
		m_TimeFrameCombo.setDisplayField("timeFrame");
		m_TimeFrameCombo.setWidth(90);
		
		// Populate the ComboBox with the available time frame options.
		ListStore<BaseModelData> timeFramesStore = new ListStore<BaseModelData>();
		BaseModelData timeFrameData;
		for (TimeFrame timeFrame : m_TimeFrameOptions)
		{
			timeFrameData = new BaseModelData();
			timeFrameData.set("timeFrame", timeFrame);
			timeFramesStore.add(timeFrameData);
		}
		
		m_TimeFrameCombo.setStore(timeFramesStore);	
		
		m_TimeFrameComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedOption = se.getSelectedItem();
		    	  m_TimeFrame = selectedOption.get("timeFrame"); 
		    	  m_HistoryWindow.setTimeFrame(m_TimeFrame);
		    	  m_HistoryWindow.loadAtTime(m_TopRowDate);
		      }
		};
		m_TimeFrameCombo.addSelectionChangedListener(m_TimeFrameComboListener);
		

		m_ToolBar.add(m_FirstTool);
		m_ToolBar.add(m_PrevTool);
		m_ToolBar.add(new SeparatorToolItem());
		m_ToolBar.add(m_PageText);
		m_ToolBar.add(m_TimePickerTool);
		m_ToolBar.add(new SeparatorToolItem());
		m_ToolBar.add(m_NextTool);
		m_ToolBar.add(m_LastTool);
		m_ToolBar.add(new SeparatorToolItem());
		m_ToolBar.add(m_RefreshTool);
		
		m_ToolBar.add(new FillToolItem());
		
		m_ToolBar.add(new LabelToolItem("Show by: "));
		
		m_ToolBar.add(m_TimeFrameCombo);

		m_ToolBar.render(target, index);
		setElement(m_ToolBar.getElement());

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
			return "No data";
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
	
	
	protected String getDisplayText()
	{
		if (m_TopRowDate == null || m_TimeFrame == null)
		{
			return "";
		}
		
		DateTimeFormat formatter = DateTimeFormat.getFormat("EEE dd MMM yyyy");
		return formatter.format(m_TopRowDate);
	}
	
	
	/**
	 * Returns the id of the top row in the specified result set.
	 * @param result DatePagingLoadResult
	 * @return id of the top row, or -1 if the result set is empty.
	 */
	protected String getTopRowId(DatePagingLoadResult<EventRecord> result)
	{
		String id = null;
		List<EventRecord> evidence = result.getData();
		if (evidence != null && evidence.size() > 0)
		{
			EventRecord firstRow = evidence.get(0);
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
	 * @param result DatePagingLoadResult
	 * @return the value of the time field in the bottom row, or <code>null</code> 
	 * if the data in the load result is <code>null</code> or empty.
	 */
	protected Date getBottomRowTime(DatePagingLoadResult<EventRecord> result)
	{
		List<EventRecord> evidence = result.getData();
		if (evidence == null || evidence.size() == 0)
		{
			return null;
		}
		
		EventRecord lastRow = evidence.get(evidence.size() - 1);	
		
		// Identify the name of the 'time' column for this TimeFrame 
		// which should have been passed in the LoadConfig.
		String timeColumnName = "time";
		SortInfo configSortInfo = m_LoadConfig.getSortInfo();
		if (configSortInfo != null)
		{
			timeColumnName = configSortInfo.getSortField();
		}
		else
		{
			switch (m_TimeFrame)
			{
				case DAY:
					timeColumnName = "day";
					break;
				case HOUR:
					timeColumnName = "hour";
					break;
				case MINUTE:
					timeColumnName = "minute";
					break;
				case SECOND:
				default:
					timeColumnName = "time";
					break;
			}
		}
		
		// NB: Current procs return time field as a String - convert to Date object.
		String bottomRowTimeVal = lastRow.get(timeColumnName);	
		DateTimeFormat dateFormatter = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss");
		switch (m_TimeFrame)
		{
			case DAY:
				dateFormatter = DateTimeFormat.getFormat("yyyy-MM-dd");
				break;
			case HOUR:
				dateFormatter = DateTimeFormat.getFormat("yyyy-MM-dd HH:00-59");
				break;
			case MINUTE:
				dateFormatter = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm");
				break;
			case SECOND:
				dateFormatter = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss");
				break;
		}
			

		return dateFormatter.parse(bottomRowTimeVal);
	}
	
	
	/**
	 * Returns the id of the bottom row in the specified result set.
	 * @param result DatePagingLoadResult
	 * @return id of the bottom row, or -1 if the result set is empty.
	 */
	protected String getBottomRowId(DatePagingLoadResult<EventRecord> result)
	{
		String id = null;
		List<EventRecord> evidence = result.getData();
		if (evidence != null && evidence.size() > 0)
		{
			EventRecord lastRow = evidence.get(evidence.size() - 1);
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
	 * Sets the Time Frame ComboBox to the selected value. Note that this 
	 * simply updates the ComboBox field, and does not update the history view.
	 * @param timeFrame time frame to set.
	 */
	protected void setTimeFrameSelection(TimeFrame timeFrame)
	{
		ListStore<BaseModelData> valueStore = m_TimeFrameCombo.getStore();
		m_TimeFrameCombo.disableEvents(true);
		
		List<BaseModelData> selectedValues = m_TimeFrameCombo.getSelection();
		TimeFrame currentlySelectedVal = null;
		if (selectedValues.size() > 0)
		{
			BaseModelData selectedValData = selectedValues.get(0);
			if (m_TimeFrameCombo.getStore().indexOf(selectedValData) != 0)
	  	  	{
				currentlySelectedVal = selectedValData.get("timeFrame");
	  	  	}
		}
		
		if (timeFrame.equals(currentlySelectedVal) == false)
		{
			BaseModelData valueData;
			for (int i = 0; i < valueStore.getCount(); i++)
			{
				valueData = valueStore.getAt(i);
				if (valueData.get("timeFrame").equals(timeFrame))
				{
					m_TimeFrameCombo.setValue(valueData);
					break;
				}
			}
		}
		
		m_TimeFrameCombo.enableEvents(true);
	}
	

	public class PagingToolBarMessages
	{
		private String afterPageText;
		private String beforePageText;
		private String displayMsg;
		private String emptyMsg;
		private String firstText;
		private String lastText;
		private String nextText;
		private String prevText;
		private String refreshText;


		/**
		 * Returns the after page text.
		 * 
		 * @return the after page text
		 */
		public String getAfterPageText()
		{
			return afterPageText;
		}


		/**
		 * Returns the before page text.
		 * 
		 * @return the before page text
		 */
		public String getBeforePageText()
		{
			return beforePageText;
		}


		/**
		 * Returns the display message.
		 * 
		 * @return the display message.
		 */
		public String getDisplayMsg()
		{
			return displayMsg;
		}


		/**
		 * Returns the empty message.
		 * 
		 * @return the empty message
		 */
		public String getEmptyMsg()
		{
			return emptyMsg;
		}


		public String getFirstText()
		{
			return firstText;
		}


		/**
		 * Returns the last text.
		 * 
		 * @return the last text
		 */
		public String getLastText()
		{
			return lastText;
		}


		/**
		 * Returns the next text.
		 * 
		 * @return the next ext
		 */
		public String getNextText()
		{
			return nextText;
		}


		/**
		 * Returns the previous text.
		 * 
		 * @return the previous text
		 */
		public String getPrevText()
		{
			return prevText;
		}


		/**
		 * Returns the refresh text.
		 * 
		 * @return the refresh text
		 */
		public String getRefreshText()
		{
			return refreshText;
		}


		/**
		 * Customizable piece of the default paging text (defaults to "of {0}").
		 * 
		 * @param afterPageText
		 *            the after page text
		 */
		public void setAfterPageText(String afterPageText)
		{
			this.afterPageText = afterPageText;
		}


		/**
		 * Customizable piece of the default paging text (defaults to "Page").
		 * 
		 * @param beforePageText
		 *            the before page text
		 */
		public void setBeforePageText(String beforePageText)
		{
			this.beforePageText = beforePageText;
		}


		/**
		 * The paging status message to display (defaults to "Displaying {0} -
		 * {1} of {2}"). Note that this string is formatted using the braced
		 * numbers 0-2 as tokens that are replaced by the values for start, end
		 * and total respectively. These tokens should be preserved when
		 * overriding this string if showing those values is desired.
		 * 
		 * @param displayMsg
		 *            the display message
		 */
		public void setDisplayMsg(String displayMsg)
		{
			this.displayMsg = displayMsg;
		}


		/**
		 * The message to display when no records are found (defaults to "No
		 * data to display").
		 * 
		 * @param emptyMsg
		 *            the empty message
		 */
		public void setEmptyMsg(String emptyMsg)
		{
			this.emptyMsg = emptyMsg;
		}


		/**
		 * Customizable piece of the default paging text (defaults to
		 * "First Page").
		 * 
		 * @param firstText
		 *            the first text
		 */
		public void setFirstText(String firstText)
		{
			this.firstText = firstText;
		}


		/**
		 * Customizable piece of the default paging text (defaults to
		 * "Last Page").
		 * 
		 * @param lastText
		 *            the last text
		 */
		public void setLastText(String lastText)
		{
			this.lastText = lastText;
		}


		/**
		 * Customizable piece of the default paging text (defaults to
		 * "Next Page").
		 * 
		 * @param nextText
		 *            the next text
		 */
		public void setNextText(String nextText)
		{
			this.nextText = nextText;
		}


		/**
		 * Customizable piece of the default paging text (defaults to "Previous
		 * Page").
		 * 
		 * @param prevText
		 *            the prev text
		 */
		public void setPrevText(String prevText)
		{
			this.prevText = prevText;
		}


		/**
		 * Customizable piece of the default paging text (defaults to
		 * "Refresh").
		 * 
		 * @param refreshText
		 *            the refresh text
		 */
		public void setRefreshText(String refreshText)
		{
			this.refreshText = refreshText;
		}

	}

}
