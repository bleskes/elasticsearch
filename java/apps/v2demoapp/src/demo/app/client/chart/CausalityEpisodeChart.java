package demo.app.client.chart;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.BaseObservable;
import com.extjs.gxt.ui.client.event.EventType;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.client.CSSSeverityColors;
import demo.app.client.ClientUtil;
import demo.app.data.CausalityEpisode;
import demo.app.data.CausalityEpisodeLayoutData;
import demo.app.data.Severity;
import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;


/**
 * Extension of the Google GChart used for plotting evidence 'episodes' in the
 * Causality View. A chart will consist of one or more episodes on a plot of 
 * episode probability against time. Each episode consists of one or more related
 * items of evidence, joined together in a line, to indicate a probable cause.
 * @author Pete Harverson
 */
public class CausalityEpisodeChart extends GChart
{
	
	private HashMap<Point, EvidenceModel>		m_Points;			// Map of evidence vs Points.
	private HashMap<Point, Integer>			m_EpisodePoints;	// Map of episode ids vs Points.
	private Point 							m_LastSelected;
	
	private int					m_SymbolSize;
	
	private Menu 				m_ContextMenu;
	//private BaseObservable 		m_Observable;
	
	private String				m_EvidenceHoverTemplate;
	private DateTimeFormat 		m_XAxisFmt;


	public static final String[] PROBABILITY_SCALE = {"#000080", "#4949C9", "#8686F1"};
	

	
	/**
	 * Creates a new causality episode chart.
	 * @param width the width of the chart display region, in pixels.
	 * @param height the height of the chart display region, in pixels.
	 */
	public CausalityEpisodeChart(int width, int height)
	{
		super(width, height);
		
		addStyleName("usage-chart");

		setBackgroundColor(USE_CSS);
		setBorderStyle(USE_CSS); 
		
		// Initialise the HashMaps of Points against their evidence and episodes on the chart.
		m_Points = new HashMap<Point, EvidenceModel>();
		m_EpisodePoints = new HashMap<Point, Integer>();
		
		// Create the Observable object for registering listeners and firing events.
		//m_Observable = new BaseObservable();
		
		m_XAxisFmt = DateTimeFormat.getFormat("HH:mm");
		m_SymbolSize = 10;
		
		initChart();
	}
	
	
	/**
	 * Initialises the chart, configuring properties such as the title and axes.
	 */
	protected void initChart()
	{
	    setChartTitle("");
	    setChartTitleThickness(20);
	    
	    setHoverParameterInterpreter(new EpisodeHoverParameterInterpreter());
	    m_EvidenceHoverTemplate = GChart.formatAsHovertext("${description}");
	     
	    // Set the X axis properties.
	    getXAxis().setAxisLabel("<b>Time</b>");
	    getXAxis().setAxisLabelThickness(25);
		getXAxis().setHasGridlines(false);
		getXAxis().setTickCount(2);
		getXAxis().setTickLabelFormat("=(Date)HH:mm");
		     
		// Set the Y axis properties.
		getYAxis().setAxisLabel("<img src=\"desktopapp/desktop/images/probability_text.png\" />");
		
		getYAxis().setAxisLabelThickness(30);
		getYAxis().setHasGridlines(false);
		getYAxis().setTickCount(2);
		getYAxis().setAxisMin(0);
		getYAxis().setAxisMax(100);
		
		// Listen for click events to clearly highlight the selected evidence.
		addClickHandler(new ClickHandler(){

            public void onClick(ClickEvent e)
            {
            	Point touchedPoint = getTouchedPoint();
        		if ( (touchedPoint != null) && (m_Points.containsKey(touchedPoint) == true))
        		{
        			if (m_LastSelected != null)
        			{
        				m_LastSelected.getParent().getSymbol().setBorderWidth(0);
        			}
        			
        			m_LastSelected = touchedPoint;
        			touchedPoint.getParent().getSymbol().setBorderColor("black");
        			touchedPoint.getParent().getSymbol().setBorderWidth(-2);
        			update();
        		}
            }
			
		});
	}
	
	
	/**
	 * Clears all the data in the chart.
	 * Note a separate call to <code>update()</code> is needed to update
	 * the chart itself.
	 */
	public void clearChart()
	{
		clearCurves();
		getXAxis().clearTicks();
		m_Points.clear();
		m_EpisodePoints.clear();
		m_LastSelected = null;
	}
	
	
	/**
	 * Sets the episodes for display in the chart. 
	 * Note a separate call to <code>update()</code> is needed to update
	 * the chart itself.
	 * @param episodes the causality episodes to be plotted on the chart.
	 * @param evidence the 'entry point' evidence whose probable cause episodes
	 * are being displayed.
	 */
	public void setEpisodes(List<CausalityEpisode> episodes, EvidenceModel evidence)
	{
		clearChart();
		
		for (int i = 0; i < episodes.size(); i++)
		{
			if (i == 0)
			{
				addEpisode(episodes.get(i), evidence.getId());
			}
			else
			{
				addEpisode(episodes.get(i), -1);
			}
		}
		
		configureXAxisRange(episodes);
		configureYAxisRange(episodes);
		
		// Add timeline to indicate 'entry point' evidence.
		Date evidenceTime = ClientUtil.parseTimeField(evidence, TimeFrame.SECOND);
		addTimeLine(evidenceTime);
		
	}
	
	
	/**
	 * Sets the episode layout data for display in the chart. 
	 * Note a separate call to <code>update()</code> is needed to update
	 * the chart itself.
	 * @param episodeLayoutData object encapsulating the episodes and layout
	 *  data (x- and y-coordinates of evidence items etc).
	 * @param evidence the 'entry point' evidence whose probable cause episodes
	 * are being displayed.
	 */
	public void setEpisodeLayoutData(List<CausalityEpisodeLayoutData> episodeLayoutData, 
			EvidenceModel evidence)
	{	
		clearChart();
		
		ArrayList<CausalityEpisode> episodes = new ArrayList<CausalityEpisode>();
		
		CausalityEpisodeLayoutData layoutData;
		String lineColour;
		for (int i = 0; i < episodeLayoutData.size(); i++)
		{
			layoutData = episodeLayoutData.get(i);
			if (i == 0)
			{
				addEpisodeWithLayoutData(layoutData, PROBABILITY_SCALE[0], evidence.getId());
			}
			else
			{
				if (i < PROBABILITY_SCALE.length)
				{
					lineColour = PROBABILITY_SCALE[i];
				}
				else
				{
					lineColour = PROBABILITY_SCALE[PROBABILITY_SCALE.length-1];
				}
				
				addEpisodeWithLayoutData(layoutData, lineColour, -1);
			}
			
			episodes.add(layoutData.getEpisode());
		}
		
		configureXAxisRange(episodes);
		configureYAxisRange(episodes);
		
		// Add timeline to indicate 'entry point' evidence.
		layoutData = episodeLayoutData.get(0);
		double timeLineX = layoutData.getEvidenceX(evidence.getId());
		addTimeLine(new Date((long)timeLineX));
		
	}
	
	
	/**
	 * Returns the size (width and height) of the symbol used to denote evidence.
     * @return the symbol size.
     */
    public int getSymbolSize()
    {
    	return m_SymbolSize;
    }


	/**
	 * Sets the size (width and height) of the symbol used to denote evidence.
     * @param symbolSize the symbol size.
     */
    public void setSymbolSize(int symbolSize)
    {
    	m_SymbolSize = symbolSize;
    }


	/**
	 * Returns the item of evidence that the mouse "brush" is 
	 * currently "touching" (the so-called "hovered over" point).
	 * @return the hovered over item of evidence, or <code>null</code> if no
	 * point is currently hovered over.
	 */
	public EvidenceModel getTouchedPointEvidence()
	{
		EvidenceModel evidence = null;
		Point touchedPoint = getTouchedPoint();
		if ( (touchedPoint != null) && (m_Points.containsKey(touchedPoint) == true) )
		{
			evidence = m_Points.get(touchedPoint);
		}
		
		return evidence;
	}
	
	
	/**
	 * Returns the id of the episode for the item of evidence that the mouse "brush" 
	 * is currently "touching" (the so-called "hovered over" point).
	 * @return the hovered over episode id, or <code>0</code> if the touched item
	 * 	of evidence is not part of an episode.
	 */
	public int getTouchedPointEpisodeId()
	{
		int episodeId = 0;
		Point touchedPoint = getTouchedPoint();
		if ( (touchedPoint != null) && (m_EpisodePoints.containsKey(touchedPoint) == true) )
		{
			episodeId = m_EpisodePoints.get(touchedPoint);
		}
		
		return episodeId;
	}
	
	
	/**
	 * Sets the chart's context menu.
	 * @param menu the context menu.
	 */
	public void setContextMenu(Menu menu)
	{
		m_ContextMenu = menu;
		sinkEvents(GXT.isSafari && GXT.isMac ? Event.ONMOUSEDOWN : Event.ONMOUSEUP);
		sinkEvents(Event.ONCONTEXTMENU);
	}
	
	
	/**
	 * Adds an episode onto the chart by adding points for each item of evidence,
	 * and then a horizontal line joining the points.
	 * @param episode the causality episode to add.
	 * @param selectEvidenceId id of the evidence item to select.
	 */
	protected void addEpisode(CausalityEpisode episode, int selectEvidenceId)
	{
		// Add the line linking the episode.
		addCurve();
		getCurve().getSymbol().setSymbolType(SymbolType.BOX_CENTER);
		getCurve().getSymbol().setWidth(2);
		getCurve().getSymbol().setHeight(2);
		getCurve().getSymbol().setBorderWidth(0);
		getCurve().getSymbol().setBackgroundColor("navy");
		getCurve().getSymbol().setFillThickness(4);
		getCurve().getSymbol().setFillSpacing(1);
		getCurve().addPoint(episode.getStartTime().getTime(), episode.getProbability());
		getCurve().addPoint(episode.getEndTime().getTime(), 
				episode.getProbability());
		
		
		// Add in points for each item of evidence in the episode.
		List<EvidenceModel> evidenceList = episode.getEvidenceList();
		Severity severity;
		Date evidenceTime;
		for (EvidenceModel evidence : evidenceList)
		{
			addCurve();
			
			severity = evidence.getSeverity();
			
			getCurve().getSymbol().setBackgroundColor(CSSSeverityColors.getColor(severity));
			getCurve().getSymbol().setHeight(m_SymbolSize);
			getCurve().getSymbol().setWidth(m_SymbolSize);
			getCurve().getSymbol().setHovertextTemplate(m_EvidenceHoverTemplate);
			getCurve().getSymbol().setHoverSelectionBorderColor("black");
			evidenceTime = ClientUtil.parseTimeField(evidence, TimeFrame.SECOND);
			getCurve().addPoint(evidenceTime.getTime(), episode.getProbability());
			if (evidence.getId() != selectEvidenceId)
			{
				getCurve().getSymbol().setBorderColor(CSSSeverityColors.getColor(severity));
			}
			else
			{
				getCurve().getSymbol().setBorderColor("black");
				getCurve().getSymbol().setBorderWidth(-2);
				m_LastSelected = getCurve().getPoint();
			}
			getCurve().getPoint().setAnnotationVisible(false);
			getCurve().getPoint().setAnnotationText(evidence.getDescription());
			m_Points.put(getCurve().getPoint(), evidence);
			m_EpisodePoints.put(getCurve().getPoint(), episode.getId());
		}
	}
	
	
	/**
	 * Adds an episode onto the chart by adding points for each item of evidence,
	 * and then a horizontal line joining the points. The x- and y-coordinates for
	 * each item of evidence are contained within the CausalityEpisodeLayoutData object.
	 * @param episodeLayout the causality episode to add.
	 * @param lineColour background colour to use for line linking episode evidence.
	 * @param selectEvidenceId id of the evidence item to select.
	 */
	protected void addEpisodeWithLayoutData(CausalityEpisodeLayoutData episodeLayout, 
			String lineColour, int selectEvidenceId)
	{
		CausalityEpisode episode = episodeLayout.getEpisode();
		
		// Add the line linking the episode.
		addCurve();
		getCurve().getSymbol().setSymbolType(SymbolType.BOX_CENTER);
		getCurve().getSymbol().setWidth(2);
		getCurve().getSymbol().setHeight(2);
		getCurve().getSymbol().setBorderWidth(0);
		getCurve().getSymbol().setBackgroundColor(lineColour);
		getCurve().getSymbol().setFillThickness(4);
		getCurve().getSymbol().setFillSpacing(1);
		getCurve().addPoint(episodeLayout.getMinX(), episodeLayout.getY());
		getCurve().addPoint(episodeLayout.getMaxX(), episodeLayout.getY());
		
		
		// Add in points for each item of evidence in the episode.
		List<EvidenceModel> evidenceList = episode.getEvidenceList();
		Severity severity;
		Symbol evSymbol;
		int idx = 0;
		EvidenceModel nextEvidence;
		for (EvidenceModel evidence : evidenceList)
		{
			// If this is the start of a contiguous sequence, connect to the end.
			if ( (CausalityEpisode.isContiguousEvidence(evidence)) && (idx < (evidenceList.size() - 1) ))
			{
				nextEvidence = evidenceList.get(idx+1);	
				if (CausalityEpisode.isContiguousEvidence(nextEvidence))
				{
					addCurve();
					getCurve().getSymbol().setSymbolType(SymbolType.BOX_CENTER);
					getCurve().getSymbol().setWidth(1);
					getCurve().getSymbol().setHeight(m_SymbolSize);
					getCurve().getSymbol().setBorderWidth(0);
					getCurve().getSymbol().setBackgroundColor(lineColour);
					getCurve().getSymbol().setFillThickness(m_SymbolSize);
					getCurve().getSymbol().setFillSpacing(1);
					getCurve().addPoint(episodeLayout.getEvidenceX(evidence.getId()), episodeLayout.getY());
					getCurve().addPoint(episodeLayout.getEvidenceX(nextEvidence.getId()), episodeLayout.getY());
				}
			}
			
			addCurve();
			
			severity = evidence.getSeverity();
			evSymbol = getCurve().getSymbol();
			
			evSymbol.setBackgroundColor(CSSSeverityColors.getColor(severity));
			evSymbol.setHeight(m_SymbolSize);
			evSymbol.setWidth(m_SymbolSize);
			evSymbol.setHovertextTemplate(m_EvidenceHoverTemplate);
			evSymbol.setHoverSelectionBorderColor("black");
			getCurve().addPoint(episodeLayout.getEvidenceX(evidence.getId()), episodeLayout.getY());
			if (evidence.getId() != selectEvidenceId)
			{
				evSymbol.setBorderColor(CSSSeverityColors.getColor(severity));
			}
			else
			{
				evSymbol.setBorderColor("black");
				evSymbol.setBorderWidth(-2);
				m_LastSelected = getCurve().getPoint();
			}
			
			getCurve().getPoint().setAnnotationVisible(false);
			getCurve().getPoint().setAnnotationText(evidence.getDescription());
			m_Points.put(getCurve().getPoint(), evidence);
			m_EpisodePoints.put(getCurve().getPoint(), episode.getId());
			
			idx++;
		}
	}
	
	
	/**
	 * Adds a 'timeline' to the episode chart to indicate the time of the item
	 * of evidence whose probable cause(s) are being displayed.
	 * @param date date for the time line.
	 */
	protected void addTimeLine(Date date)
	{
		addCurve();
		getCurve().getSymbol().setSymbolType(SymbolType.BOX_CENTER);
	    getCurve().getSymbol().setWidth(2);
	    getCurve().getSymbol().setHeight(2);
	    getCurve().getSymbol().setBorderWidth(0);
	    getCurve().getSymbol().setBackgroundColor("navy");
	    getCurve().getSymbol().setFillThickness(2);
	    getCurve().getSymbol().setFillSpacing(5);
		getCurve().getSymbol().setHovertextTemplate("${x}");
		
		getCurve().addPoint(date.getTime(), getYAxis().getAxisMin());
		getCurve().addPoint(date.getTime(), getYAxis().getAxisMax());
		
	}
	
	
	/**
	 * Configures the x-axis range and axis labels for the display of the
	 * supplied episodes.
	 */
	protected void configureXAxisRange(List<CausalityEpisode> episodes)
	{
		Date minDate = episodes.get(0).getStartTime();
		Date maxDate = episodes.get(0).getEndTime();
		
		for (CausalityEpisode episode : episodes)
		{
			if (episode.getStartTime().before(minDate))
			{
				minDate = episode.getStartTime();
			}
			
			if (episode.getEndTime().after(maxDate))
			{
				maxDate = episode.getEndTime();
			}
		}
		
		// Add padding at either end - 10% or a minimum of half a second.
		int timeGap = (int)(maxDate.getTime() - minDate.getTime());
		int padding = Math.max(timeGap/10, 500);
		
		DateWrapper minTime = (new DateWrapper(minDate)).addMillis(-padding);
		DateWrapper maxTime = (new DateWrapper(maxDate)).addMillis(padding);	

		getXAxis().setAxisMin(minTime.getTime());
		getXAxis().addTick(minTime.getTime(), m_XAxisFmt.format(minTime.asDate()));
		getXAxis().setAxisMax(maxTime.getTime());
		getXAxis().addTick(maxTime.getTime(), m_XAxisFmt.format(maxTime.addMinutes(1).asDate()));

	}
	
	
	/**
	 * Configures the y-axis range for the display of the supplied episodes.
	 */
	protected void configureYAxisRange(List<CausalityEpisode> episodes)
	{
		double minProb = episodes.get(episodes.size() -1).getProbability();
		double maxProb = episodes.get(0).getProbability();
		int remainder = (int) (minProb % 5);
		if (remainder == 0)
		{
			minProb-=5;
		}
		else
		{
			minProb-=remainder;
		}
		
		remainder = (int)(maxProb % 5);
		if (remainder > 0)
		{
			maxProb += (5-remainder);
		}
		
		getYAxis().setAxisMin(minProb);
		getYAxis().setAxisMax(maxProb);
	}
	
	
	/**
     * Fires whenever a browser event is received.
     * @param event the browser event that has been received.
     */
    public void onBrowserEvent(Event event) 
    {  	
    	// Note that the ONCONTEXTMENU does not fire in Opera.
    	if (event.getTypeInt() == Event.ONCONTEXTMENU)
		{
        	onContextMenu(event);

		}
    	
    	//fireEvent(new EventType(event.getTypeInt()), new BaseEvent(this));
    	
    	// Pass event on (a GChart should respond to mouse activity  - but never eat it).
        super.onBrowserEvent(event);
    }
    
    

	
	
    /**
     * Fires when a context menu event is received, displaying the context
     * menu at the coordinates on the chart that the event was triggered.
     * @param event the context menu event.
     */
    protected void onContextMenu(Event event)
	{
		if (m_ContextMenu != null)
		{
			// Stop the event from being propagated to prevent the browser's
			// normal context menu being displayed.
			event.cancelBubble(true);
        	event.preventDefault();
        	
        	Point touchedPoint = getTouchedPoint();
        	
        	// Check that the point corresponds to an item of evidence.
        	if ( (touchedPoint != null) && (m_Points.containsKey(touchedPoint) == true) )
        	{        		
				final int x = event.getClientX();
				final int y = event.getClientY();
	
					DeferredCommand.addCommand(new Command()
					{
						public void execute()
						{
							m_ContextMenu.showAt(x, y);
						}
					});
        	}
		}
	}
	
	
	/** 
	 * Custom GChart HoverParameterInterpreter to display the evidence description
	 * as the tooltip when hovvering over a point in the Episode Chart,
	 * getHoverParameter() method.
	 */
	class EpisodeHoverParameterInterpreter implements HoverParameterInterpreter
	{

		public String getHoverParameter(String paramName,
		        GChart.Curve.Point hoveredOver)
		{

			// Returning null tells GChart "I don't know how to expand that
			// parameter name". The built-in parameters (${x}, ${y}, etc.) won't
			// be processed correctly unless you return null for this "no
			// matching parameter" case.
			String result = null;
			if (paramName.equals("description"))
			{
		        result = hoveredOver.getAnnotationText();
			}

			return result;
		}

	}
}
