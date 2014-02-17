package demo.app.client.list;

import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;

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
public class EvidenceHistoryPagingToolBar extends EvidenceViewPagingToolBar
{
	protected EvidenceHistoryWindow 		m_HistoryWindow;

	protected TimeFrame[] 				m_TimeFrameOptions;
	protected ComboBox<BaseModelData> 	m_TimeFrameCombo;
	private SelectionChangedListener<BaseModelData>	m_TimeFrameComboListener;
	
	
	/**
	 * Creates a new paging tool bar for the specified Evidence History Window.
	 * 
	 * @param historyWindow time frame to use when first rendered.
	 */
	public EvidenceHistoryPagingToolBar(EvidenceHistoryWindow historyWindow)
	{
		super(historyWindow.getView().getDataType(), historyWindow.getTimeFrame());
		
		m_HistoryWindow = historyWindow;
		m_TimeFrame = historyWindow.getTimeFrame();
		m_TimeFrameOptions = new TimeFrame[]{TimeFrame.DAY, 
				TimeFrame.HOUR, TimeFrame.MINUTE, TimeFrame.SECOND};
		
		// Create a ComboBox for setting the time frame e.g. Week, Daily, Hourly.
		m_TimeFrameCombo = new ComboBox<BaseModelData>();
		m_TimeFrameCombo.setEditable(false);
		m_TimeFrameCombo.setListStyle("prelert-combo-list");
		m_TimeFrameCombo.setDisplayField("timeFrame");
		m_TimeFrameCombo.setWidth(90);
		m_TimeFrameCombo.setTriggerAction(TriggerAction.ALL);
		
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
		
		add(new LabelToolItem("Show by: "));
		add(m_TimeFrameCombo);
	}
	
	
	/**
	 * Returns the choice time frame options for the toolbar.
     * @return array of TimeFrames which are available in this toolbar.
     */
    public TimeFrame[] getTimeFrameOptions()
    {
    	return m_TimeFrameOptions;
    }
    
    
	protected void onLoad(LoadEvent event)
	{
		super.onLoad(event);
		
		setTimeFrameSelection(m_TimeFrame);
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
}
