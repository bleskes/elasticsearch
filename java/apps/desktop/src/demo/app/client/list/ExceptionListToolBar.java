package demo.app.client.list;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import demo.app.client.ApplicationResponseHandler;
import demo.app.client.DateTimeMenu;
import demo.app.client.DateTimePickerEvent;
import demo.app.client.SliderComponent;
import demo.app.client.SliderComponentEvent;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.EvidenceViewPagingLoader;
import demo.app.data.ExceptionPagingLoadConfig;
import demo.app.data.ExceptionListPagingLoader;
import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.EvidenceQueryServiceAsync;

/**
 * A specialized toolbar for use in the Exception List window. The Exception
 * List is an evidence list that is automatically filtered by the Prelert engine
 * so only exceptions or anomalies are shown.
 * <p>
 * The toolbar contains a slider bar for setting the 'Noise' filter, and a
 * drop-down ComboBox control for selecting the time window of the exception
 * calculation i.e. ALL, WEEK, DAY, HOUR, MINUTE.
 * <p>
 * It is bound to an {@link EvidenceViewPagingLoader} and provides automatic
 * paging controls to navigate through the date range of evidence in the Prelert
 * database and a DateTimePicker for the selecting the date of the evidence
 * displayed.
 * 
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
public class ExceptionListToolBar extends ToolBar
{
	protected String	m_EvidenceDataType;
	
	protected Date 		m_TopRowDate;	// Date of top row of current page.
	protected String 	m_TopRowId;		// id of top row of current page.
	protected Date 		m_BottomRowDate;// Date of bottom row of current page.
	protected String	m_BottomRowId;	// id of bottom row of current page.
	
	protected Date m_StartDate;		// Date of newest row of evidence for current config.
	protected Date m_EndDate;		// Date of oldest row of evidence for current config.
	protected Date m_EarliestEvidenceDate;	// Date of oldest item of evidence in DB.

	protected SliderComponent			m_NoiseSlider;
	protected TimeFrame[] 				m_TimeWindowOptions;
	protected ComboBox<BaseModelData> 	m_TimeWindowCombo;
	private SelectionChangedListener<BaseModelData> m_TimeWindowComboListener;

	protected ExceptionListPagingLoader<?> m_Loader;
	protected ExceptionPagingLoadConfig config;
	protected Button m_FirstBtn;
	protected Button m_PrevBtn;
	protected Button m_TimePickerTool;
	protected DateTimeMenu m_DateMenu;
	protected Button m_NextBtn;
	protected Button m_LastBtn;
	protected Button m_RefreshBtn;
	protected LabelToolItem m_PageText;
	protected ExceptionListToolBarMessages m_Msgs;
	protected boolean showToolTips = true;
	protected LoadListener loadListener;
	protected ExceptionListToolBarImages images;

	private boolean reuseConfig = true;
	private LoadEvent renderEvent;
	private boolean savedEnableState = true;
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
	 * Creates a new paging tool bar for the specified Exception List.
	 * @param dataType the data type, such as 'apache_logs' or 'error_logs', which
	 * 	is used to identify the particular type of evidence data being displayed.
	 */
	public ExceptionListToolBar(String dataType)
	{
		m_EvidenceDataType = dataType;
		
		m_TimeWindowOptions = new TimeFrame[] { TimeFrame.ALL, 
				TimeFrame.DAY, TimeFrame.HOUR, TimeFrame.MINUTE };
		
		m_Msgs = new ExceptionListToolBarMessages();

		m_FirstBtn = new Button();
		m_FirstBtn.addListener(Events.Disable, m_EnableListener);
		m_FirstBtn.addListener(Events.Enable, m_EnableListener);
		m_FirstBtn.setIcon(getImages().getFirst());
		if (showToolTips)
			m_FirstBtn.setToolTip(m_Msgs.getFirstText());
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
		if (showToolTips)
			m_PrevBtn.setToolTip(m_Msgs.getPrevText());
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
		if (showToolTips)
			m_NextBtn.setToolTip(m_Msgs.getNextText());
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
		if (showToolTips)
			m_LastBtn.setToolTip(m_Msgs.getLastText());
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
		if (showToolTips)
			m_RefreshBtn.setToolTip(m_Msgs.getRefreshText());
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
		configureDateTimePicker();
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

				setActivePage(selectedDate);
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
		
		
		// Add a Slider for setting the noise level.
		m_NoiseSlider = new SliderComponent();
		m_NoiseSlider.setWidth(120);
		m_NoiseSlider.setIncrement(1);
		m_NoiseSlider.setMinValue(1);
		m_NoiseSlider.setMaxValue(100);
		m_NoiseSlider.setUseTip(false);
		//m_NoiseSlider.setDraggable(false);
		m_NoiseSlider.addListener(Events.Change, new Listener<SliderComponentEvent>()
		{
            public void handleEvent(SliderComponentEvent se)
            {
	            m_Loader.setNoiseLevel(se.getNewValue());
	            first();
            }

		});

		add(new LabelToolItem("Noise filter: "));
		add(m_NoiseSlider);

		LabelToolItem spacerItem = new LabelToolItem(" ");
		spacerItem.setWidth(10);
		add(spacerItem);

		// Create a ComboBox for setting the time frame e.g. Week, Daily,
		// Hourly.
		m_TimeWindowCombo = new ComboBox<BaseModelData>();
		m_TimeWindowCombo.setEditable(false);
		m_TimeWindowCombo.setListStyle("prelert-combo-list");
		m_TimeWindowCombo.setDisplayField("timeWindow");
		m_TimeWindowCombo.setTriggerAction(TriggerAction.ALL);
		m_TimeWindowCombo.setWidth(90);

		// Populate the ComboBox with the available time window options.
		ListStore<BaseModelData> timeWindowsStore = new ListStore<BaseModelData>();
		BaseModelData timeFrameData;

		for (TimeFrame timeFrame : m_TimeWindowOptions)
		{
			timeFrameData = new BaseModelData();
			timeFrameData.set("timeWindow", timeFrame);
			timeWindowsStore.add(timeFrameData);
		}

		m_TimeWindowCombo.setStore(timeWindowsStore);

		m_TimeWindowComboListener = new SelectionChangedListener<BaseModelData>()
		{
			@Override
            public void selectionChanged(SelectionChangedEvent<BaseModelData> se)
			{
				BaseModelData selectedOption = se.getSelectedItem();
				TimeFrame timeWindow = selectedOption.get("timeWindow");
				m_Loader.setTimeWindow(timeWindow);
				if (m_TopRowDate != null)
				{
					m_Loader.loadAtTime(m_TopRowDate, TimeFrame.SECOND);
				}
				else
				{
					m_Loader.load();
				}
			}
		};
		m_TimeWindowCombo.addSelectionChangedListener(m_TimeWindowComboListener);

		add(new LabelToolItem(" Time window: "));
		add(m_TimeWindowCombo);
	}


	/**
	 * Binds the toolbar to the loader.
	 * 
	 * @param loader the loader
	 */
	public void bind(ExceptionListPagingLoader<?> loader)
	{
		if (this.m_Loader != null)
		{
			this.m_Loader.removeLoadListener(loadListener);
		}
		this.m_Loader = loader;
		if (loader != null)
		{
			if (loadListener == null)
			{
				loadListener = new LoadListener()
				{
					@Override
                    public void loaderBeforeLoad(LoadEvent le)
					{
						savedEnableState = isEnabled();
						setEnabled(false);
						m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-loading"));
					}


					@Override
                    public void loaderLoad(LoadEvent le)
					{
						m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
						setEnabled(savedEnableState);
						onLoad(le);
					}


					@Override
                    public void loaderLoadException(LoadEvent le)
					{
						m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
						setEnabled(savedEnableState);
					}
				};
			}
			loader.addLoadListener(loadListener);
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
	 * Returns the date of the top row in the list.
	 * @return the date of the top row.
	 */
	public Date getActivePage()
	{
		return m_TopRowDate;
	}


	/**
	 * Returns the images used in the toolbar.
	 * 
	 * @return the image
	 */
	public ExceptionListToolBarImages getImages()
	{
		if (images == null)
		{
			images = new ExceptionListToolBarImages();
		}
		return images;
	}


	/**
	 * Returns the toolbar's messages.
	 * 
	 * @return the messages
	 */
	public ExceptionListToolBarMessages getMessages()
	{
		return m_Msgs;
	}


	/**
	 * Returns true if the previous load config is reused.
	 * 
	 * @return the reuse config state
	 */
	public boolean isReuseConfig()
	{
		return reuseConfig;
	}


	/**
	 * Returns true if tooltip are enabled.
	 * 
	 * @return the show tooltip state
	 */
	public boolean isShowToolTips()
	{
		return showToolTips;
	}
	
	
	/**
	 * Moves to the first page.
	 */
	public void first()
	{
		m_Loader.loadFirstPage(m_StartDate, TimeFrame.SECOND);
	}


	/**
	 * Moves to the last page.
	 */
	public void last()
	{
		m_Loader.loadLastPage(m_EndDate, TimeFrame.SECOND);
	}


	/**
	 * Moves to the last page.
	 */
	public void next()
	{
		// Need to pass the time and id of the bottom row of the current page.
		m_Loader.loadNextPage(m_BottomRowDate, m_BottomRowId, TimeFrame.SECOND);
	}


	/**
	 * Moves the the previous page.
	 */
	public void previous()
	{
		// Need to pass the time and id of the top row of the current page.
		if (m_TopRowDate != null)
		{
			m_Loader.loadPreviousPage(m_TopRowDate, m_TopRowId, TimeFrame.SECOND);
		}
		else
		{
			first();
		}
	}


	/**
	 * Refreshes the data for 'now' using the current configuration (noise level/time window).
	 */
	public void refresh()
	{
		setActivePage(new Date());
	}


	/**
	 * Sets the active page.
	 * @param date date/time for first row in the active page.
	 */
	public void setActivePage(Date date)
	{
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date now = new Date();
		if (date.after(now))
		{
			first();
			return;
		}
		else if (date.before(m_EarliestEvidenceDate))
		{
			m_Loader.loadAtTime(m_EarliestEvidenceDate, TimeFrame.SECOND);
			return;
		}
		else
		{
			m_Loader.loadAtTime(date, TimeFrame.SECOND);
		}
	}


	/**
	 * Sets the images to be used on the toolbar.
	 * @param images toolbar images.
	 */
	public void setImages(ExceptionListToolBarImages images)
	{
		this.images = images;
	}


	/**
	 * Sets the messages to be used on the toolbar.
	 * 
	 * @param messages the messages
	 */
	public void setMessages(ExceptionListToolBarMessages messages)
	{
		m_Msgs = messages;
	}


	/**
	 * True to reuse the previous load config (defaults to true).
	 * 
	 * @param reuseConfig true to reuse the load config
	 */
	public void setReuseConfig(boolean reuseConfig)
	{
		this.reuseConfig = reuseConfig;
	}


	/**
	 * Sets if the button tool tips should be displayed (defaults to true,
	 * pre-render).
	 * 
	 * @param showToolTips true to show tool tips
	 */
	public void setShowToolTips(boolean showToolTips)
	{
		this.showToolTips = showToolTips;
	}


	protected void onLoad(LoadEvent event)
	{
		if (!rendered)
		{
			renderEvent = event;
			return;
		}
		config = (ExceptionPagingLoadConfig) event.getConfig();
		DatePagingLoadResult<EvidenceModel> result = event.getData();
		
		m_TopRowDate = result.getDate();
		
		if (result.getStartDate() != null)
		{
			m_StartDate = result.getStartDate(); // i.e. time of most recent item of evidence.
		}
		else
		{
			m_StartDate = m_Loader.getDate();
		}

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
		
		setTimeWindowSelection(m_Loader.getTimeWindow());
		m_NoiseSlider.setValue(m_Loader.getNoiseLevel(), true);
		
		enable();
		
		boolean enableLastNext = 
			( (m_BottomRowDate != null) && (m_BottomRowDate.after(m_EndDate)) );
		m_NextBtn.setEnabled(enableLastNext);
		m_LastBtn.setEnabled(enableLastNext);
		
		// Set the maximum date on the DateTimePicker as now,
		// and its value to match the time of the top row.
		m_DateMenu.getDateTimePicker().setMaxDate(new Date());	
		if (m_TopRowDate != null)
		{
			m_DateMenu.getDateTimePicker().setValue(m_TopRowDate);
		}
	}


	@Override
	protected void onRender(Element target, int index)
	{
		super.onRender(target, index);

		if (renderEvent != null)
		{
			onLoad(renderEvent);
			renderEvent = null;
		}
	}
	
	
	protected String getPageText()
	{
		String text = "No data";
		Date dateToFormat = (m_TopRowDate != null ? m_TopRowDate : m_Loader.getDate());
		
		if (dateToFormat != null)
		{	
			DateTimeFormat minFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy HH:mm");
			text = minFormatter.format(dateToFormat);
		}
		
		return text;
	}


	/**
	 * Sets the Time Window ComboBox to the selected value. Note that this simply
	 * updates the ComboBox field, and does not update the history view.
	 * 
	 * @param timeWindow time window to set.
	 */
	protected void setTimeWindowSelection(TimeFrame timeWindow)
	{
		ListStore<BaseModelData> valueStore = m_TimeWindowCombo.getStore();
		m_TimeWindowCombo.disableEvents(true);

		List<BaseModelData> selectedValues = m_TimeWindowCombo.getSelection();
		TimeFrame currentlySelectedVal = null;
		if (selectedValues.size() > 0)
		{
			BaseModelData selectedValData = selectedValues.get(0);
			if (m_TimeWindowCombo.getStore().indexOf(selectedValData) != 0)
			{
				currentlySelectedVal = selectedValData.get("timeWindow");
			}
		}

		if (timeWindow.equals(currentlySelectedVal) == false)
		{
			BaseModelData valueData;
			for (int i = 0; i < valueStore.getCount(); i++)
			{
				valueData = valueStore.getAt(i);
				if (valueData.get("timeWindow").equals(timeWindow))
				{
					m_TimeWindowCombo.setValue(valueData);
					break;
				}
			}
		}

		m_TimeWindowCombo.enableEvents(true);
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
			id = "" + firstRow.getId();
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
		
		// NB: Current procs return time field as a String - convert to Date object.
		String bottomRowTimeVal = lastRow.getTime(TimeFrame.SECOND);	
		DateTimeFormat dateFormatter = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss");

		return dateFormatter.parse(bottomRowTimeVal);
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
			id = "" + lastRow.getId();
		}
		
		return id;
	}
	
	
	/**
	 * Configures the minimum date on the DateTimePicker by querying the
	 * evidence query service for the date of the earliest item of evidence
	 * in the Prelert database.
	 */
	private void configureDateTimePicker()
	{
		EvidenceQueryServiceAsync evidenceQueryService = 
			DatabaseServiceLocator.getInstance().getEvidenceQueryService();
		evidenceQueryService.getEarliestDate(m_EvidenceDataType, null,
				new ApplicationResponseHandler<Date>(){

	        public void uponFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server.", null);
	        }


	        public void uponSuccess(Date earliestDate)
	        {
	        	m_EarliestEvidenceDate = earliestDate;
	        	m_DateMenu.getDateTimePicker().setMinDate(earliestDate);
	        }
        });
		
	}
	

	/**
	 * ToolBar images.
	 */
	public class ExceptionListToolBarImages
	{
		private AbstractImagePrototype first = GXT.IMAGES.paging_toolbar_first();
		private AbstractImagePrototype prev = GXT.IMAGES.paging_toolbar_prev();
		private AbstractImagePrototype next = GXT.IMAGES.paging_toolbar_next();
		private AbstractImagePrototype last = GXT.IMAGES.paging_toolbar_last();
		private AbstractImagePrototype refresh = GXT.IMAGES.paging_toolbar_refresh();

		private AbstractImagePrototype firstDisabled = GXT.IMAGES.paging_toolbar_first_disabled();
		private AbstractImagePrototype prevDisabled = GXT.IMAGES.paging_toolbar_prev_disabled();
		private AbstractImagePrototype nextDisabled = GXT.IMAGES.paging_toolbar_next_disabled();
		private AbstractImagePrototype lastDisabled = GXT.IMAGES.paging_toolbar_last_disabled();


		public void setFirst(AbstractImagePrototype first)
		{
			this.first = first;
		}


		public AbstractImagePrototype getFirst()
		{
			return first;
		}


		public void setPrev(AbstractImagePrototype prev)
		{
			this.prev = prev;
		}


		public AbstractImagePrototype getPrev()
		{
			return prev;
		}


		public void setNext(AbstractImagePrototype next)
		{
			this.next = next;
		}


		public AbstractImagePrototype getNext()
		{
			return next;
		}


		public void setLast(AbstractImagePrototype last)
		{
			this.last = last;
		}


		public AbstractImagePrototype getLast()
		{
			return last;
		}


		public void setRefresh(AbstractImagePrototype refresh)
		{
			this.refresh = refresh;
		}


		public AbstractImagePrototype getRefresh()
		{
			return refresh;
		}


		public void setFirstDisabled(AbstractImagePrototype firstDisabled)
		{
			this.firstDisabled = firstDisabled;
		}


		public AbstractImagePrototype getFirstDisabled()
		{
			return firstDisabled;
		}


		public void setPrevDisabled(AbstractImagePrototype prevDisabled)
		{
			this.prevDisabled = prevDisabled;
		}


		public AbstractImagePrototype getPrevDisabled()
		{
			return prevDisabled;
		}


		public void setNextDisabled(AbstractImagePrototype nextDisabled)
		{
			this.nextDisabled = nextDisabled;
		}


		public AbstractImagePrototype getNextDisabled()
		{
			return nextDisabled;
		}


		public void setLastDisabled(AbstractImagePrototype lastDisabled)
		{
			this.lastDisabled = lastDisabled;
		}


		public AbstractImagePrototype getLastDisabled()
		{
			return lastDisabled;
		}

	}

	/**
	 * PagingToolBar messages.
	 */
	class ExceptionListToolBarMessages
	{
		private String emptyMsg = GXT.MESSAGES.pagingToolBar_emptyMsg();
		private String firstText = GXT.MESSAGES.pagingToolBar_firstText();
		private String lastText = GXT.MESSAGES.pagingToolBar_lastText();
		private String nextText = GXT.MESSAGES.pagingToolBar_nextText();
		private String prevText = GXT.MESSAGES.pagingToolBar_prevText();
		private String refreshText = GXT.MESSAGES.pagingToolBar_refreshText();



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
