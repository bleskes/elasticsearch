package demo.app.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.messages.XMessages;
import com.extjs.gxt.ui.client.util.Format;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Util;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.SplitButton;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.WidgetComponent;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.extjs.gxt.ui.client.widget.menu.CheckMenuItem;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

import demo.app.data.*;

/**
 * A specialized toolbar that is bound to a {@link ListLoader} and provides
 * automatic paging controls.
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
public class DatePagingToolBar extends Component implements Listener
{
	protected DatePagingLoader m_Loader;
	protected DatePagingLoadConfig m_LoadConfig;

	protected TimeFrame m_TimeFrame;
	protected Date m_Date;
	protected Date m_StartDate; // i.e. time of earliest usage record in DB.
	protected Date m_EndDate;	// i.e. time of most recent usage record in DB.

	protected ToolBar m_ToolBar;
	protected Radio m_WeekRadio;
	protected Radio m_DayRadio;
	protected Radio m_HourRadio;
	
	protected Button 		m_FirstTool;
	protected Button 		m_PrevTool;
	protected Button 		m_NextTool;
	protected Button 		m_LastTool;
	protected SplitButton 	m_RefreshTool;
	protected CheckMenuItem m_AutoRefreshMenuItem;
	
	protected LabelToolItem m_DisplayText;
	protected LabelToolItem m_PageText;
	protected PagingToolBarMessages m_Msgs;
	protected boolean m_ShowToolTips = true;

	private boolean m_ReuseConfig = true;
	private LoadEvent m_RenderEvent;
	private List<Component> m_CustomTools = new ArrayList<Component>();
	
	private RefreshTimer	m_RefreshTimer;
	private boolean			m_IsAutoRefreshing;
	
	
	/**
	 * Creates a new paging tool bar. If enabled, the auto-refresh
	 * frequency will be running at 1 minute.
	 * @param timeFrame the initial TimeFrame to use for paging.
	 */
	public DatePagingToolBar(TimeFrame timeFrame)
	{
		this(timeFrame, 60000);
	}
	
	
	/**
	 * Creates a new paging tool bar, with the auto-refresh
	 * frequency set to the specified value.
	 * @param timeFrame the initial TimeFrame to use for paging.
	 * @param autoRefreshFrequency frequency to use for the auto-refresh functionality.
	 */
	public DatePagingToolBar(TimeFrame timeFrame, int autoRefreshFrequency)
	{
		m_TimeFrame = timeFrame;
		m_Msgs = new PagingToolBarMessages();
		m_RefreshTimer = new RefreshTimer(autoRefreshFrequency);
	}
	


	/**
	 * Binds the toolbar to the loader.
	 * @param loader the loader
	 */
	public void bind(DatePagingLoader loader)
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
			m_DisplayText.setLabel("");
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
			m_RefreshTimer.setRepeating(false);
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
			if (isAutoRefreshing() == true)
			{
				m_RefreshTimer.setRepeating(true);
			}
		}
		else if (be.getType().equals(Loader.LoadException))
		{
			LoadEvent le = (LoadEvent)be;
			
			MessageBox.alert("Prelert - Error",
	                "Error loading data from server (" + 
	                le.exception.getClass().getName() + ")", null);
			
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
	 * Moves to the first page.
	 */
	public void first()
	{
		setAutoRefreshing(false);
		doLoadRequest(m_StartDate, m_TimeFrame);
	}
	

	/**
	 * Moves to the last page.
	 */
	public void last()
	{
		doLoadRequest(m_EndDate, m_TimeFrame);
	}


	/**
	 * Moves to the next page.
	 */
	public void next()
	{
		doLoadRequest(new Date(m_Date.getTime() + m_TimeFrame.getInterval()),
		        m_TimeFrame);
	}


	/**
	 * Moves the the previous page.
	 */
	public void previous()
	{
		setAutoRefreshing(false);
		doLoadRequest(new Date(m_Date.getTime() - m_TimeFrame.getInterval()),
		        m_TimeFrame);
	}


	/**
	 * Refreshes the data, loading up the most recent usage data for the
	 * currently configured time frame.
	 */
	public void refresh()
	{
		setActivePage(new Date());
	}


	/**
	 * Sets the active page.
	 * @param date starting date/time for usage data to display.
	 */
	public void setActivePage(Date date)
	{
		if (m_EndDate != null && date.after(m_EndDate))
		{
			last();
			return;
		}
		else if (m_StartDate != null && date.before(m_StartDate))
		{
			first();
			return;
		}
		else
		{
			setAutoRefreshing(false);
			m_Loader.load(date, m_TimeFrame);
		}
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
	
	
	/**
	 * Returns whether the paging toolbar is currently auto-refreshing.
	 * @return <code>true</code> if the toolbar is auto-refreshing,
	 * 			<code>false</code> otherwise.
	 */
	public boolean isAutoRefreshing()
	{
		return m_IsAutoRefreshing;
	}
	
	
	/**
	 * Enables or disables auto-refreshing on the DatePagingToolBar.
	 * @param doAutoRefresh <code>true</code> to enable auto-refreshing,
	 * 			<code>false</code> to disable it.
	 */
	public void setAutoRefreshing(boolean doAutoRefresh)
	{
		if (m_IsAutoRefreshing != doAutoRefresh)
		{
			m_IsAutoRefreshing = doAutoRefresh;
			m_RefreshTimer.setRepeating(doAutoRefresh);
			m_AutoRefreshMenuItem.setChecked(doAutoRefresh, true);
		}
	}
	
	
	/**
	 * Pauses auto-refreshing if the toolbar has been set to auto-refresh.
	 */
	public void pauseAutoRefreshing()
	{
		if (isAutoRefreshing())
		{
			m_RefreshTimer.setRepeating(false);
		}
	}
	
	
	/**
	 * Resumes auto-refreshing if the toolbar has been set to auto-refresh.
	 */
	public void resumeAutoRefreshing()
	{
		if (isAutoRefreshing())
		{
			m_RefreshTimer.setRepeating(true);
		}
	}


	protected void doAttachChildren()
	{
		ComponentHelper.doAttach(m_ToolBar);
	}


	protected void doDetachChildren()
	{
		ComponentHelper.doDetach(m_ToolBar);
	}


	protected void doLoadRequest(Date date, TimeFrame timeFrame)
	{
		if (m_ReuseConfig && m_LoadConfig != null)
		{
			m_LoadConfig.setDate(date);
			m_LoadConfig.setTimeFrame(timeFrame);
			m_Loader.load(m_LoadConfig);
		}
		else
		{
			m_Loader.load(date, timeFrame);
		}
	}


	protected void onLoad(LoadEvent event)
	{	
		if (!rendered)
		{
			m_RenderEvent = event;
			return;
		}
		m_LoadConfig = event.getConfig();

		DatePagingLoadResult result = event.getData();

		m_TimeFrame = result.getTimeFrame();
		m_Date = result.getDate();	// Will correspond to date of first item in result set.
		m_StartDate = result.getStartDate(); 	// i.e. time of earliest usage record in DB.
		m_EndDate = result.getEndDate();		// i.e. time of most recent usage record in DB.
		
		m_PageText.setLabel(getPageText());

		// Enable or disable navigation buttons depending on comparison of the
		// start/end times of the current time frame.
		Date timeFrameEnd = getTimeFrameEndTime();
		if (m_Date != null)
		{
			m_FirstTool.setEnabled(m_Date.after(m_StartDate));
			m_PrevTool.setEnabled(m_Date.after(m_StartDate));
			m_NextTool.setEnabled(timeFrameEnd.before(m_EndDate));
			m_LastTool.setEnabled(timeFrameEnd.before(m_EndDate));
		}
		else
		{
			m_FirstTool.setEnabled(false);
			m_PrevTool.setEnabled(false);
			m_NextTool.setEnabled(false);
			m_LastTool.setEnabled(false);
		}
		
		// Set the appropriate Time Frame radio button.
		switch (m_TimeFrame)
		{
			case WEEK:
				m_WeekRadio.setValue(true);
				break;
			case DAY:
				m_DayRadio.setValue(true);
				break;
			case HOUR:
				m_HourRadio.setValue(true);
				break;
		}

		m_DisplayText.setLabel(getDisplayText());
		
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

		m_RefreshTool = new SplitButton();
		m_RefreshTool.setIconStyle("x-tbar-loading");
		if (m_ShowToolTips)
		{
			m_RefreshTool.setToolTip(m_Msgs.getRefreshText());
		}
	    Menu refreshMenu = new Menu();  
	    m_AutoRefreshMenuItem = new CheckMenuItem("Auto refresh");
	    refreshMenu.add(m_AutoRefreshMenuItem);   
	    m_RefreshTool.setMenu(refreshMenu);
		m_RefreshTool.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			public void componentSelected(ButtonEvent ce)
			{
				refresh();
			}
		});
		m_AutoRefreshMenuItem.addListener(Events.CheckChange, new Listener<MenuEvent>()
		{
            public void handleEvent(MenuEvent be)
            {
                setAutoRefreshing(m_AutoRefreshMenuItem.isChecked());
                if (m_AutoRefreshMenuItem.isChecked() == true)
                {
                	// Kick off a refresh straight away.
                	refresh();
                }
            }
	
		});

		LabelToolItem beforePage = new LabelToolItem(m_Msgs.getBeforePageText());
		beforePage.setStyleName("my-paging-text");
		
		m_PageText = new LabelToolItem();

		m_DisplayText = new LabelToolItem();
		m_DisplayText.setStyleName("my-paging-display");
		
		// Create radio buttons for setting the time frame of the chart
		// e.g. Week, Daily, Hourly.
		RadioGroup timeFrameGroup = new RadioGroup();
		m_WeekRadio = new Radio();
		m_WeekRadio.addInputStyleName("tool-radio-button");
		m_WeekRadio.setName("graphTimeFrame");
		m_WeekRadio.setBoxLabel("Week");
		m_WeekRadio.addListener(Events.OnClick, new Listener<FieldEvent>() 
		{
            public void handleEvent(FieldEvent fe) 
            {
            	setAutoRefreshing(false);
                m_Loader.load(m_Loader.getDate(), TimeFrame.WEEK);
            }
        });
		timeFrameGroup.add(m_WeekRadio);
		
		m_DayRadio = new Radio();
		m_DayRadio.setName("graphTimeFrame");
		m_DayRadio.addInputStyleName("tool-radio-button");
		m_DayRadio.setBoxLabel("Daily");
		m_DayRadio.addListener(Events.OnClick, new Listener<FieldEvent>() 
		{
            public void handleEvent(FieldEvent fe) 
            {
            	setAutoRefreshing(false);
                m_Loader.load(m_Loader.getDate(), TimeFrame.DAY);
            }
        });
		timeFrameGroup.add(m_DayRadio);
		
		m_HourRadio = new Radio();
		m_HourRadio.setName("graphTimeFrame");
		m_HourRadio.addInputStyleName("tool-radio-button");
		m_HourRadio.setBoxLabel("Hourly");
		m_HourRadio.addListener(Events.OnClick, new Listener<FieldEvent>() 
		{
            public void handleEvent(FieldEvent fe) 
            {
            	setAutoRefreshing(false);
                m_Loader.load(m_Loader.getDate(), TimeFrame.HOUR);
            }
        });
		timeFrameGroup.add(m_HourRadio);
		
		timeFrameGroup.setWidth("180px");
		
		m_ToolBar.add(timeFrameGroup);
		m_ToolBar.add(new SeparatorToolItem());

		m_ToolBar.add(m_FirstTool);
		m_ToolBar.add(m_PrevTool);
		m_ToolBar.add(new SeparatorToolItem());
		m_ToolBar.add(new WidgetComponent(m_PageText));
		m_ToolBar.add(new SeparatorToolItem());
		m_ToolBar.add(m_NextTool);
		m_ToolBar.add(m_LastTool);
		m_ToolBar.add(new SeparatorToolItem());
		m_ToolBar.add(m_RefreshTool);

		for (Component tool : m_CustomTools)
		{
			m_ToolBar.add(tool);
		}
		

		m_ToolBar.add(new FillToolItem());
		m_ToolBar.add(m_DisplayText);

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
		if (m_Date == null || m_TimeFrame == null)
		{
			return "No data";
		}
		
		String text = new String();
		
		switch (m_TimeFrame)
		{
			case WEEK:
				DateTimeFormat weekStartFormatter = DateTimeFormat.getFormat("EEE dd");
				DateTimeFormat weekEndFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy");
				text += weekStartFormatter.format(m_Date);
				text += " - ";
				Date weekEndDate = new Date(m_Date.getTime() + (6*24*60*60*1000));
				text += weekEndFormatter.format(weekEndDate);
				break;
				
			case DAY:
				DateTimeFormat dayFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy");
				text += dayFormatter.format(m_Date);
				break;
				
			case HOUR:
				DateTimeFormat hourFormatter = DateTimeFormat.getFormat("EEE dd MMM yyyy HH:'00'");
				text += hourFormatter.format(m_Date);
				break;
		}
		
		return text;
	}
	
	
	protected String getDisplayText()
	{
		if (m_Date == null || m_TimeFrame == null)
		{
			return "";
		}
		
		DateTimeFormat formatter = DateTimeFormat.getFormat("EEE dd MMM yyyy");
		return formatter.format(m_Date);
	}
	
	
	/**
	 * Returns the time corresponding to the end of the time frame currently in view.
	 * @return the end time of the current time frame, or today's date/time if the
	 * time frame or start date of the time frame are <code>null</code>.
	 */
	protected Date getTimeFrameEndTime()
	{
		if (m_Date == null || m_TimeFrame == null)
		{
			return new Date();
		}
		
		Date startDate;
		Date endDate = new Date();
		switch (m_TimeFrame)
		{
			case WEEK:
				startDate = new Date(m_Date.getYear(), m_Date.getMonth(), m_Date.getDate());
				endDate = new Date(startDate.getTime() + (7*24*60*60*1000));
				break;
				
			case DAY:
				startDate = new Date(m_Date.getYear(), m_Date.getMonth(), m_Date.getDate());
				endDate = new Date(startDate.getTime() + (24*60*60*1000));
				break;
				
			case HOUR:
				startDate = new Date(m_Date.getYear(), m_Date.getMonth(), 
						m_Date.getDate(), m_Date.getHours(), 0);
				endDate = new Date(startDate.getTime() + (60*60*1000));
				break;
		}
		
		return endDate;
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
		
		
        public void run()
        {
        	refresh();
        }
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
		 * @param refreshText the refresh text
		 */
		public void setRefreshText(String refreshText)
		{
			this.refreshText = refreshText;
		}

	}

}
