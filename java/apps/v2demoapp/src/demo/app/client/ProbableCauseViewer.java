package demo.app.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.*;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import com.googlecode.gchart.client.GChart.SymbolType;
import com.googlecode.gchart.client.GChart.TouchedPointUpdateOption;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.data.gxt.GridRowInfo;

import pl.balon.gwt.diagrams.client.connection.Connection;
import pl.balon.gwt.diagrams.client.connection.RectilinearTwoEndedConnection;
import pl.balon.gwt.diagrams.client.connection.StraightTwoEndedConnection;
import pl.balon.gwt.diagrams.client.connector.Connector;
import pl.balon.gwt.diagrams.client.connector.UIObjectConnector;


/**
 * An extension of the Ext JS GXT Window for a Prelert Probable Cause Viewer Window.
 * The Window contains a graphical view of probable cause 'episodes', with an
 * information panel below to display details on the selected evidence.
 * @author Pete Harverson
 */
public class ProbableCauseViewer extends Window
{
	private EpisodeChart 	m_EpisodeChart;
	private Point 			m_LastSelected;
	
	
	/**
	 * Creates new Probable Cause Viewer window.
	 */
	public ProbableCauseViewer()
	{
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Probable Cause Viewer : User disconnected");
		setSize(600, 520);
		setResizable(true);
		
		setIconStyle("prob-cause-win-icon");
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		setLayout(new BorderLayout());
		
		ContentPanel north = createFlowchart();
	    ContentPanel center = createInfoPanel(); 
	    
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(5, 0, 5, 0));   
	  
	    BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 200);   
	    northData.setSplit(true);    
	    northData.setMargins(new Margins(5));   
	   
	    add(north, northData);
	    add(center, centerData);      
	     
	}
	
	
	/**
	 * Creates an episode chart visual for display of a probable cause,
	 * using the GChart Library for the Google Web Toolkit.
	 * See: http://code.google.com/p/gchart/
	 * @return ContentPanel containing the Episode visual.
	 */
	protected ContentPanel createEpisodeChart()
	{
		ContentPanel panel = new ContentPanel(); 
		panel.setHeaderVisible(false);
	
		m_EpisodeChart = new EpisodeChart();
		m_EpisodeChart.addClickHandler(new ClickHandler(){

            public void onClick(ClickEvent e)
            {
            	Point touchedPoint = m_EpisodeChart.getTouchedPoint();
        		if (touchedPoint != null)
        		{
        			if (m_LastSelected != null)
        			{
        				m_LastSelected.getParent().getSymbol().setBorderWidth(0);
        			}
        			
        			m_LastSelected = touchedPoint;
        			touchedPoint.getParent().getSymbol().setBorderColor("black");
        			touchedPoint.getParent().getSymbol().setBorderWidth(2);
        			m_EpisodeChart.update();
        		}
            }
			
		});
		
		EpisodeEvidence[] episode1 = {
			    new EpisodeEvidence("Output threshold breach",
			    		"6/28/2009 03:29", 90.0, "#FFB429"),
			    new EpisodeEvidence("User disconnected",
			    		"6/28/2009 03:31", 90.0, "#FF0000"),
			    new EpisodeEvidence("User usage drops to 0",
			    		"6/28/2009 03:32", 90.0, "#63B8FF")
			};
			
		EpisodeEvidence[] episode2 = {
				    new EpisodeEvidence("Exceeded max users",
				    		"6/28/2009 03:30", 70.0, "#FFFF00"),
				    new EpisodeEvidence("User disconnected",
				    		"6/28/2009 03:31", 70.0, "#FF0000"),
				    new EpisodeEvidence("User usage drops to 0",
				    		"6/28/2009 03:32", 70.0, "#63B8FF")
				};
		
		EpisodeEvidence[] episode3 = {
			    new EpisodeEvidence("High CPU Utilisation",
			    		"6/28/2009 03:28:30", 60.0, "#FFFF00"),
			    new EpisodeEvidence("Service full",
					    		"6/28/2009 03:30", 60.0, "#FFFF00"),
			    new EpisodeEvidence("User disconnected",
			    		"6/28/2009 03:31", 60.0, "#FF0000"),
			    new EpisodeEvidence("Service has shutdown",
			    		"6/28/2009 03:32:30", 60.0, "#FF0000")
			};
		
		addEpisode(episode1, "#000080");
		addEpisode(episode2, "#B0C4DE");
		addEpisode(episode3, "#E6E6FA");
		
		// Add a timeline to indicate the entrance point piece of evidence.
		m_EpisodeChart.addCurve(0);
		m_EpisodeChart.getCurve(0).getSymbol().setSymbolType(SymbolType.BOX_CENTER);
		m_EpisodeChart.getCurve(0).getSymbol().setWidth(2);
		m_EpisodeChart.getCurve(0).getSymbol().setHeight(2);
		m_EpisodeChart.getCurve(0).getSymbol().setBorderWidth(0);
		m_EpisodeChart.getCurve(0).getSymbol().setBackgroundColor("navy");
		m_EpisodeChart.getCurve(0).getSymbol().setFillThickness(2);
		m_EpisodeChart.getCurve(0).getSymbol().setFillSpacing(5);
		m_EpisodeChart.getCurve(0).getSymbol().setHovertextTemplate("${x}");
		m_EpisodeChart.getCurve(0).addPoint(new Date("6/28/2009 03:31").getTime(), 50d);
		m_EpisodeChart.getCurve(0).addPoint(new Date("6/28/2009 03:31").getTime(), 100d);
		
		panel.add(m_EpisodeChart);
		m_EpisodeChart.update();
		
		return panel;
	}
	
	
	private void addEpisode(EpisodeEvidence[] episode, String hexColor)
	{
		String hoverTemplate = GChart.formatAsHovertext("${description}");
		
		// Add the line linking the episode.
		m_EpisodeChart.addCurve();
		m_EpisodeChart.getCurve().getSymbol().setSymbolType(SymbolType.BOX_CENTER);
		m_EpisodeChart.getCurve().getSymbol().setWidth(2);
		m_EpisodeChart.getCurve().getSymbol().setHeight(2);
		m_EpisodeChart.getCurve().getSymbol().setBorderWidth(0);
		m_EpisodeChart.getCurve().getSymbol().setBackgroundColor(hexColor);
		m_EpisodeChart.getCurve().getSymbol().setFillThickness(4);
		m_EpisodeChart.getCurve().getSymbol().setFillSpacing(1);
		m_EpisodeChart.getCurve().getSymbol().setHovertextTemplate("${x}");
		m_EpisodeChart.getCurve().addPoint(episode[0].getDate().getTime(), episode[0].getProbability());
		m_EpisodeChart.getCurve().addPoint(episode[episode.length - 1].getDate().getTime(), 
				episode[episode.length - 1].getProbability());
		
		for (int i = 0; i < episode.length; i++)
		{
			m_EpisodeChart.addCurve();
			m_EpisodeChart.getCurve().getSymbol().setBorderColor(episode[i].getBackgroundColour());
			m_EpisodeChart.getCurve().getSymbol().setBackgroundColor(episode[i].getBackgroundColour());
			m_EpisodeChart.getCurve().getSymbol().setHeight(10);
			m_EpisodeChart.getCurve().getSymbol().setWidth(10);
			m_EpisodeChart.getCurve().getSymbol().setHovertextTemplate(hoverTemplate);
			m_EpisodeChart.getCurve().addPoint(episode[i].getDate().getTime(), episode[i].getProbability());
			m_EpisodeChart.getCurve().getPoint().setAnnotationVisible(false);
			m_EpisodeChart.getCurve().getPoint().setAnnotationText(episode[i].getDescription());
		}
	}
	
	
	/**
	 * Creates the Info Panel to display details of the piece of evidence that 
	 * has been selected in the graphical display.
	 * @return
	 */
	protected ContentPanel createInfoPanel()
	{
		ContentPanel panel = new ContentPanel();
		panel.setLayout(new FitLayout());
				
		ListStore<GridRowInfo> store = new ListStore<GridRowInfo>();
		store.add(new GridRowInfo("id", "10066"));	
		store.add(new GridRowInfo("time", "2009-06-24 13:30:15"));	
		store.add(new GridRowInfo("description", "user disconnected due to overflow"));	
		store.add(new GridRowInfo("severity", "major"));	
		store.add(new GridRowInfo("count", "1"));	
		store.add(new GridRowInfo("probable_cause", "43"));	
		store.add(new GridRowInfo("appid", "Smart Order Router"));	
		store.add(new GridRowInfo("ipaddress", "159.156.141.46"));	
		store.add(new GridRowInfo("node", ""));	
		store.add(new GridRowInfo("service", ""));	
		store.add(new GridRowInfo("source", "sol30m-8201.1.p2ps"));	
		store.add(new GridRowInfo("username", "286_sor_emea_dev_0"));
		store.add(new GridRowInfo("message", ""));
		
	    ColumnConfig propColumn = new ColumnConfig("columnName", "Property", 150);
	    ColumnConfig valueColumn = new ColumnConfig("columnValue", "Value", 350);

	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(propColumn);
	    config.add(valueColumn);

	    ColumnModel columnModel = new ColumnModel(config);
		
	    Grid<GridRowInfo> infoGrid = new Grid<GridRowInfo>(store, columnModel);
	    infoGrid.setLoadMask(true);
	    infoGrid.setBorders(true);
	    
	    panel.add(infoGrid);
	    
	    return panel;
	}
	
	
	/**
	 * Creates an alternative Flowchart visual for display of a probable cause,
	 * using the Diagrams Library for the Google Web Toolkit.
	 * See: http://code.google.com/p/gwt-diagrams/
	 * @return ContentPanel containing the Flowchart visual.
	 */
	protected ContentPanel createFlowchart()
	{
		ContentPanel panel = new ContentPanel(); 
		panel.setHeaderVisible(false);
		
		BorderLayoutData centerLayoutData = new BorderLayoutData(LayoutRegion.CENTER);
	    centerLayoutData.setMargins(new Margins(0, 0, 0, 0));
		
		AbsolutePanel absPanel = new AbsolutePanel(); 
		
		Label label1 = new HTML("Output threshold breach<br>ID=10001");    
		
		label1.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent arg0)
            {
				Info.display("Probable Cause Viewer", "Output threshold breach");
            }

			
		});
		
		Widget label2 = new HTML("User disconnected<br>ID=10066");      
		Widget label3 = new Label("Output threshold OK");      
		Widget label4 = new HTML("User usage drops to 0<br>ID=10223");      
		
		label1.addStyleName("example-connector");
		label2.addStyleName("example-connector-selected");
		label3.addStyleName("example-connector");
		label4.addStyleName("example-connector");
		
		absPanel.add(label1, 50, 100);        
		absPanel.add(label2, 300, 100);        
		absPanel.add(label4, 500, 100);        
		absPanel.add(label3, 300, 150);        
       
		
		// gwt-diagrams stuff 
		Connector c1 = UIObjectConnector.wrap(label1);        
		Connector c2 = UIObjectConnector.wrap(label2);        
		Connector c3 = UIObjectConnector.wrap(label3);        
		Connector c4 = UIObjectConnector.wrap(label4);        
		Connection connection = new StraightTwoEndedConnection(c1, c2); 
		Connection connection2 = new StraightTwoEndedConnection(c1, c3); 
		Connection connection3 = new RectilinearTwoEndedConnection(c2, c4); 
		connection.appendTo(absPanel);
		connection2.appendTo(absPanel);
		connection3.appendTo(absPanel);
		
		panel.add(absPanel);
		
		return panel;
	}
	
	
	/**
	 * Extension of GChart to display connected pieces of evidence as an 'episode'.
	 * @author Pete
	 *
	 */
	class EpisodeChart extends GChart
	{
		EpisodeChart() 
		{
		    super(450,150); 
		     
		    addStyleName("peteGChart");

			setBackgroundColor(USE_CSS);
			setBorderStyle(USE_CSS); 
		     
		    setChartTitle("");
		    setChartTitleThickness(20);
		    //setPadding("5px");
		    
		    setHoverParameterInterpreter(new EpisodeHoverParameterInterpreter());
		    String hoverTemplate = GChart.formatAsHovertext("${description}");
		     
		    getXAxis().setAxisLabel("<small><i>Time</i></small>");
			getXAxis().setHasGridlines(false);
			getXAxis().setTickCount(3);
			// Except for "=(Date)", a standard GWT DateTimeFormat string
			getXAxis().setTickLabelFormat("=(Date)HH:mm:ss");
			getXAxis().setAxisMin((new Date("6/28/2009 03:28").getTime()));
			     
			getYAxis().setAxisLabel("<small><i>Probability</i></small>");
			getYAxis().setAxisLabelThickness(50);
			getYAxis().setHasGridlines(false);
			getYAxis().setTickCount(3);
			getYAxis().setAxisMin(50);
			getYAxis().setAxisMax(100);
			
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
	
	
	class EpisodeEvidence
	{
		String m_Description;
		Date m_EvidenceDate;
		double m_Prob;
		String m_BgHex;


		public EpisodeEvidence(String description, String dateTimeString, 
				double prob, String bgHex)
		{
			m_Description = description;
			m_EvidenceDate = new Date(dateTimeString);
			m_Prob = prob;
			m_BgHex = bgHex;
		}
		
		
		public String getDescription()
		{
			return m_Description;
		}
		
		
		public Date getDate()
		{
			return m_EvidenceDate;
		}
		
		
		public double getProbability()
		{
			return m_Prob;
		}
		
		
		public String getBackgroundColour()
		{
			return m_BgHex;
		}
	}
	
	
	

}

	