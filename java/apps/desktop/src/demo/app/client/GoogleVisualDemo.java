package demo.app.client;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window.CloseAction;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.Query.Callback;
import com.google.gwt.visualization.client.events.RangeChangeHandler;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.*;
import com.google.gwt.visualization.client.formatters.ArrowFormat;
import com.google.gwt.visualization.client.visualizations.*;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.WindowMode;
import com.google.gwt.visualization.client.visualizations.Table.Options;

import demo.app.data.UsageRecord;


public class GoogleVisualDemo extends ViewWindow
{
	
	private DesktopApp				m_Desktop;
	
	public GoogleVisualDemo(DesktopApp desktop)
	{
		m_Desktop = desktop;
		
		setLayout(new BorderLayout());
		setCloseAction(CloseAction.HIDE);
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Google Visualizations Demo");
		setSize(750, 520);
		setResizable(true);
		
		VerticalPanel vp = new VerticalPanel();
	    vp.getElement().getStyle().setPropertyPx("margin", 15);
	    vp.add(new Label("Google Visualization with GWT demo."));
	    TabPanel tabPanel = new TabPanel();
	    vp.add(tabPanel);
	    tabPanel.setWidth("800");
	    tabPanel.setHeight("500");
	    tabPanel.add(createServerUsageTimeLine(), "Serverload");
	    tabPanel.add(createAnnotatedTimeLine(), "AnnotatedTimeLine");
	    tabPanel.add(createPieChart(), "Pie Chart");
	    tabPanel.add(createTable(), "Table");
	    //tabPanel.add(createDataView(), "DataView");
	    
	    tabPanel.selectTab(0);
	    
	    BorderLayoutData centerLayoutData = new BorderLayoutData(LayoutRegion.CENTER);
		centerLayoutData.setMargins(new Margins(0, 0, 0, 0));
		add(vp, centerLayoutData); 
		
	}

	@Override
	public void load()
	{
		// TO DO.
	}


	/**
	 * Creates a table and a view and shows both next to each other.
	 * 
	 * @return a panel with two tables.
	 */
	private Widget createDataView()
	{
		Panel panel = new HorizontalPanel();
		DataTable table = DataTable.create();

		/* create a table with 3 columns */
		table.addColumn(ColumnType.NUMBER, "x");
		table.addColumn(ColumnType.NUMBER, "x * x");
		table.addColumn(ColumnType.NUMBER, "sqrt(x)");
		table.addRows(10);
		for (int i = 0; i < table.getNumberOfRows(); i++)
		{
			table.setValue(i, 0, i);
			table.setValue(i, 1, i * i);
			table.setValue(i, 2, Math.sqrt(i));
		}
		/* Add original table */
		Panel flowPanel = new FlowPanel();
		panel.add(flowPanel);
		flowPanel.add(new Label("Original DataTable:"));
		Table chart = new Table();
		flowPanel.add(chart);
		chart.draw(table);

		flowPanel = new FlowPanel();
		flowPanel.add(new Label("DataView with columns 2 and 1:"));
		/* create a view on this table, with columns 2 and 1 */
		Table viewChart = new Table();
		DataView view = DataView.create(table);
		view.setColumns(new int[] { 2, 1 });
		flowPanel.add(viewChart);
		panel.add(flowPanel);
		viewChart.draw(view);

		return panel;
	}


	private ArrowFormat createFormatter()
	{
		ArrowFormat.Options options = ArrowFormat.Options.create();
		options.setBase(1.5);
		return ArrowFormat.create(options);
	}


	/**
	 * Creates a pie chart visualization.
	 * 
	 * @return panel with pie chart.
	 */
	private Widget createPieChart()
	{
		/* create a datatable */
		DataTable data = DataTable.create();
		data.addColumn(ColumnType.STRING, "Task");
		data.addColumn(ColumnType.NUMBER, "Hours per Day");
		data.addRows(5);
		data.setValue(0, 0, "Work");
		data.setValue(0, 1, 11);
		data.setValue(1, 0, "Eat");
		data.setValue(1, 1, 2);
		data.setValue(2, 0, "Commute");
		data.setValue(2, 1, 2);
		data.setValue(3, 0, "Watch TV");
		data.setValue(3, 1, 2);
		data.setValue(4, 0, "Sleep");
		data.setValue(4, 1, 7);

		/* create pie chart */

		PieChart.Options options = PieChart.Options.create();
		options.setWidth(400);
		options.setHeight(240);
		options.set3D(true);
		options.setTitle("My Daily Activities");
		return new PieChart(data, options);
	}


	/**
	 * Creates a table visualization from a spreadsheet.
	 * 
	 * @return panel with a table.
	 */
	private Widget createTable()
	{
		final String noSelectionString = "<i>No rows selected.</i>";
		final Panel panel = new FlowPanel();
		final HTML label = new HTML(noSelectionString);
		panel.add(new HTML(
		        "<h2>Table visualization with selection support</h2>"));
		panel.add(label);
		// Read data from spreadsheet
		String dataUrl = "http://spreadsheets.google.com/tq?key=prll1aQH05yQqp_DKPP9TNg&pub=1";
		Query query = Query.create(dataUrl);
		query.send(new Callback()
		{

			public void onResponse(QueryResponse response)
			{
				if (response.isError())
				{
					Window.alert("Error in query: " + response.getMessage()
					        + ' ' + response.getDetailedMessage());
					return;
				}

				final Table viz = new Table();
				panel.add(viz);
				Options options = Table.Options.create();
				options.setShowRowNumber(true);
				DataTable dataTable = response.getDataTable();
				ArrowFormat formatter = createFormatter();
				formatter.format(dataTable, 1);
				viz.draw(dataTable, options);

				viz.addSelectHandler(new SelectHandler()
				{
					@Override
					public void onSelect(SelectEvent event)
					{
						StringBuffer b = new StringBuffer();
						Table table = viz;
						JsArray<Selection> s = table.getSelections();
						for (int i = 0; i < s.length(); ++i)
						{
							if (s.get(i).isCell())
							{
								b.append(" cell ");
								b.append(s.get(i).getRow());
								b.append(":");
								b.append(s.get(i).getColumn());
							}
							else if (s.get(i).isRow())
							{
								b.append(" row ");
								b.append(s.get(i).getRow());
							}
							else
							{
								b.append(" column ");
								b.append(s.get(i).getColumn());
							}
						}
						if (b.length() == 0)
						{
							label.setHTML(noSelectionString);
						}
						else
						{
							label.setHTML("<i>Selection changed to"
							        + b.toString() + "<i>");
						}
					}
				});
			}
		});
		return panel;
	}
	
	
	public Widget createAnnotatedTimeLine()
	{
	    @SuppressWarnings("unused")
	    int year, month, day;

	    com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options options = 
	    	com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options.create();
	    options.setDisplayAnnotations(true);

	    DataTable data = DataTable.create();
	    data.addColumn(ColumnType.DATE, "Date");
	    data.addColumn(ColumnType.NUMBER, "Sold Pencils");
	    data.addColumn(ColumnType.STRING, "title1");
	    data.addColumn(ColumnType.STRING, "text1");
	    data.addColumn(ColumnType.NUMBER, "Sold Pens");
	    data.addColumn(ColumnType.STRING, "title2");
	    data.addColumn(ColumnType.STRING, "text2");
	    data.addRows(6);
	    try {
	      data.setValue(0, 0, new Date(year = 2008 - 1900, month = 1, day = 1));
	      data.setValue(1, 0, new Date(year = 2008 - 1900, month = 1, day = 2));
	      data.setValue(2, 0, new Date(year = 2008 - 1900, month = 1, day = 3));
	      data.setValue(3, 0, new Date(year = 2008 - 1900, month = 1, day = 4));
	      data.setValue(4, 0, new Date(year = 2008 - 1900, month = 1, day = 5));
	      data.setValue(5, 0, new Date(year = 2008 - 1900, month = 1, day = 6));
	    } catch (JavaScriptException ex) 
	    {
	      GWT.log("Error creating data table - Date bug on mac?", ex);
	    }
	    data.setValue(0, 1, 30000);
	    data.setValue(0, 4, 40645);
	    data.setValue(1, 1, 14045);
	    data.setValue(1, 4, 20374);
	    data.setValue(2, 1, 55022);
	    data.setValue(2, 4, 50766);
	    data.setValue(3, 1, 75284);
	    data.setValue(3, 4, 14334);
	    data.setValue(3, 5, "Out of Stock");
	    data.setValue(3, 6, "Ran out of stock on pens at 4pm");
	    data.setValue(4, 1, 41476);
	    data.setValue(4, 2, "Bought 200k pens");
	    data.setValue(4, 4, 66467);
	    data.setValue(5, 1, 33322);
	    data.setValue(5, 4, 39463);

	    AnnotatedTimeLine widget = new AnnotatedTimeLine(data, options, "700px", "400px");
	    
	    /*
	    widget.addRangeChangeHandler(new RangeChangeHandler() {
	      @Override
	      public void onRangeChange(RangeChangeEvent event) {
	        Window.alert("The range has changed.\n" + event.getStart() + 
	            "\nFalls mainly on the plains.\n" + event.getEnd());
	      }
	    });
	    */
	    
	    
	    Panel flowPanel = new FlowPanel();
		flowPanel.add(new Label("Annotated Timeline:"));
		flowPanel.add(widget);
	    
	    return flowPanel;
	}
	
	
	public Widget createServerUsageTimeLine()
	{

	    int year, month, day;

	    com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options options = 
	    	com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options.create();
	    options.setDisplayAnnotations(true);
	    options.setWindowMode(WindowMode.TRANSPARENT);

	    final DataTable data = DataTable.create();
	    data.addColumn(ColumnType.DATETIME, "Date");
	    data.addColumn(ColumnType.NUMBER, "serverload");
		
	    Date testDate = new Date(2009-1900, 3, 27);
	    
		m_Desktop.getServerUsageQueryServiceInstance().getTotalDailyUsageData(testDate, "serverload", new AsyncCallback<List<UsageRecord>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        @SuppressWarnings("deprecation")
            public void onSuccess(List<UsageRecord> records)
	        {
	        	data.addRows(records.size());
	        	int i = 0;
	        	for (UsageRecord record : records)
	        	{
	        		data.setValue(i, 0, record.getTime());
	        		data.setValue(i, 1, record.getValue());
	        		
	        		i++;
	        	}
	        	
	        	GWT.log("Size of data set: " + records.size(), null);
	        }
	        
	        
        });
		
		




	    AnnotatedTimeLine widget = new AnnotatedTimeLine(data, options, "700px", "400px");
	    
	    /*
	    widget.addRangeChangeHandler(new RangeChangeHandler() {
	      @Override
	      public void onRangeChange(RangeChangeEvent event) {
	        Window.alert("The range has changed.\n" + event.getStart() + 
	            "\nFalls mainly on the plains.\n" + event.getEnd());
	      }
	    });
	    */
	    
	    
	    Panel flowPanel = new FlowPanel();
		flowPanel.add(new Label("Annotated Timeline:"));
		flowPanel.add(widget);
	    
	    return flowPanel;
 
	}

}
