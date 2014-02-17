package demo.app.client;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.data.UsageRecord;


public class LineChart extends GChart implements ClickListener
{
	private DesktopApp m_Desktop;
	
	public LineChart()
	{
		addStyleName("peteGChart");

		setBackgroundColor(USE_CSS);
		setBorderStyle(USE_CSS); 
		
		createChartWithTimeAxis();
		//createSimpleLineChart();
	}
	
	
	public void setDesktop(DesktopApp desktop)
	{
		m_Desktop = desktop;
	}
	
	
	/**
	 * Sets the list of UsageRecord objects for display in the chart,
	 * and then updates the chart itself.
	 * @param records the UsageRecord objects to be plotted in the chart.
	 */
	public void setUsageRecords(List<UsageRecord> records)
	{
		setUsageRecords(null, null, records);
	}
	
	
	/**
	 * Sets the list of UsageRecord objects for display in the chart,
	 * and then updates the chart itself.
	 * @param source the name of the Source whose records have been supplied.
	 * @param records the UsageRecord objects to be plotted in the chart.
	 */
	public void setUsageRecords(String source, List<UsageRecord> records)
	{
		setUsageRecords(source, null, records);
	}
	
	
	/**
	 * Sets the list of UsageRecord objects for display in the chart,
	 * and then updates the chart itself.
	 * @param source the name of the Source whose records have been supplied.
	 * @param records the UsageRecord objects to be plotted in the chart.
	 */
	public void setUsageRecords(String source, String username, List<UsageRecord> records)
	{
		// Clear out current data.
		getCurve().clearPoints();
		
		// Set the scales on the x and y axes.
		// TO DO: Set appropriate values if size of records is zero.
		if (records.size() > 0)
		{
			double xAxisMin = calculateXAxisMin(records);
			getXAxis().setAxisMin(xAxisMin);
		
			double yAxisMax = calculateYAxisMax(records);
			getYAxis().setAxisMax(yAxisMax);
		}
		
		for (UsageRecord record : records)
		{
			// Note that getTime() returns milliseconds since 1/1/70
			// required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			getCurve().addPoint(record.getTime().getTime(), record.getTotal());
		}
		
		// Update the title of the chart.
		String chartTitle = "<b><big>User Usage - ";
		
		if (source != null)
		{
			chartTitle += source;
		}
		else
		{
			chartTitle += "All Sources";
		}
		
		chartTitle += ", ";
		
		if (username != null)
		{
			chartTitle += username;
		}
		else
		{
			chartTitle += "All Users";
		}
		
		chartTitle += "</big></b>";
		
		setChartTitle(chartTitle);

		
		update();
	}
	
	
	protected void createSimpleLineChart()
	{
		setChartTitle("<b>x<sup>2</sup> vs x</b>");
		setChartSize(300, 200);
		addCurve(); // solid, 2px thick, 1px resolution, connecting lines:
		getCurve().getSymbol().setSymbolType(SymbolType.LINE);
		getCurve().getSymbol().setFillThickness(2);
		getCurve().getSymbol().setFillSpacing(1); // Make center-fill of square
												  // point, markers same color
												  // as line:
		getCurve().getSymbol().setBackgroundColor(
		        getCurve().getSymbol().getBorderColor());
		for (int i = 0; i < 10; i++)
		{
			getCurve().addPoint(i, i * i);
		}

		getCurve().setLegendLabel("x<sup>2</sup>");
		getXAxis().setAxisLabel("x");
		getYAxis().setAxisLabel("x<sup>2</sup>");

		getXAxis().setTickLabelFontSize(8);
	}
	
	
	protected void createChartWithTimeAxis()
	{
		
		addClickListener(this);
		
		setChartSize(500, 300);
		setChartTitle("<b><big>Usage vs Time</big></b>");
		setPadding("5px");

		getXAxis().setAxisLabel("<small><b><i>Time</i></b></small>");
		getXAxis().setHasGridlines(true);
		
		// For the Week chart, show one tick per day.
		getXAxis().setTickCount(8);
		getXAxis().setTicksPerLabel(2);
		// Except for "=(Date)", a standard GWT DateTimeFormat string
		getXAxis().setTickLabelFormat("=(Date)MMM dd HH:mm");

		getYAxis().setAxisLabel("<small><b><i>Total</i></b></small>");
	    getYAxis().setHasGridlines(false);
	    // last bar 'sticks out' over right edge, so extend 'grid' right:
	    getYAxis().setTickCount(11);
	    getYAxis().setTickLength(5); 
	    getYAxis().setAxisMin(0);
		//getYAxis().setAxisMax(100);
		

		// Add the dataset.
		addCurve();
		//getCurve().setLegendLabel("<i>Total usage</i>");
		getCurve().setYAxis(Y_AXIS);
		getCurve().getSymbol().setBorderColor("red");
		getCurve().getSymbol().setBackgroundColor("red");
		getCurve().getSymbol().setSymbolType(SymbolType.LINE);
		getCurve().getSymbol().setFillThickness(1);
		getCurve().getSymbol().setFillSpacing(1); 


	}
	
	
	protected void createDualAxisChart()
	{
		addClickListener(this);
		
		setChartSize(400, 300);
		setChartTitle("<b><big>Temperature and CPU vs Time</big></b>");
		setPadding("5px");

		getXAxis().setAxisLabel("<small><b><i>Time</i></b></small>");
		getXAxis().setHasGridlines(true);
		getXAxis().setTickCount(6);
		// Except for "=(Date)", a standard GWT DateTimeFormat string
		getXAxis().setTickLabelFormat("=(Date)dd/h:mm a");

		getYAxis().setAxisLabel("<small><b><i>&deg;C</i></b></small>");
		getYAxis().setHasGridlines(true);
		getYAxis().setTickCount(11);
		getYAxis().setAxisMin(11);
		getYAxis().setAxisMax(16);
		
	    getY2Axis().setAxisLabel("<small><b><i>%CPU</i></b></small>");
	    getY2Axis().setHasGridlines(false);
	    // last bar 'sticks out' over right edge, so extend 'grid' right:
	    getY2Axis().setTickCount(11);
	    getY2Axis().setTickLength(15); 
	    getY2Axis().setAxisMin(0);
		getY2Axis().setAxisMax(100);

	    // Add the first dataset.
		addCurve();
		getCurve().setLegendLabel("<i>T (&deg;C)</i>");
		getCurve().setYAxis(Y_AXIS);
		getCurve().getSymbol().setBorderColor("blue");
		getCurve().getSymbol().setBackgroundColor("blue");
		getCurve().getSymbol().setFillSpacing(10);
		getCurve().getSymbol().setFillThickness(3);

		for (int i = 0; i < dateSequence.length; i++)
		{
			// Note that getTime() returns milliseconds since
			// 1/1/70--required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			getCurve().addPoint(dateSequence[i].date.getTime(),
			        dateSequence[i].value);
		}
		
		
		// Add the second dataset.
		addCurve();
		getCurve().setLegendLabel("<i>CPU</i>");
		getCurve().setYAxis(Y2_AXIS);
		getCurve().getSymbol().setBorderColor("red");
		getCurve().getSymbol().setBackgroundColor("red");
		getCurve().getSymbol().setSymbolType(SymbolType.LINE);
		getCurve().getSymbol().setFillThickness(1);
		getCurve().getSymbol().setFillSpacing(1); 

		for (int i = 0; i < cpuSequence.length; i++)
		{
			// Note that getTime() returns milliseconds since
			// 1/1/70--required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			getCurve().addPoint(cpuSequence[i].date.getTime(),
					cpuSequence[i].value);
		}

	}
	
	
	/**
	 * Calculates the minimum value to be displayed on the x axis.
	 * @param records the records being displayed in the chart.
	 * @return minimum value visible on the x-axis.
	 */
	protected double calculateXAxisMin(List<UsageRecord> records)
	{
		UsageRecord lastRecord = records.get(records.size() - 1);
		long lastTimeMillis = lastRecord.getTime().getTime();
		
		long millisInWeek = 7 * 24 * 60 * 60 * 1000;
		
		return lastTimeMillis - millisInWeek;
	}
	
	
	/**
	 * Calculates the maximum value to be displayed on the y axis.
	 * @param records the records being displayed in the chart.
	 * @return minimum value visible on the y axis.
	 */
	protected double calculateYAxisMax(List<UsageRecord> records)
	{
		double maxVal = 0;
		
		for (UsageRecord record : records)
		{
			// Note that getTime() returns milliseconds since 1/1/70
			// required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			maxVal = Math.max(maxVal, record.getTotal());
		}
		
		double powerFloor = Math.floor(Math.log10(maxVal)); // e.g. gives 5 for 416521
		double raiseToPower = Math.pow(10, powerFloor); // e.g. 100000 for 416521
		
		double nearestMax = (Math.floor(maxVal/raiseToPower) + 1) * raiseToPower;
		
		return nearestMax;
	}
	
	
    public void onClick(Widget sender)
	{
		GChart g = (GChart) sender;
		
		
		// Test out clicking on a point to open an Evidence Window.
		Point touchedPoint = g.getTouchedPoint();
		if (touchedPoint != null)
		{
			GWT.log("X coord is " + new Date((long)g.getTouchedPoint().getX()), null);
			
			double xCoord = touchedPoint.getX();
			final Date usageDate = new Date((long)xCoord);
			
			// Run in a DeferredCommand to ensure all Event Handlers have completed
			// so that the Evidence Window can come to the front.
			DeferredCommand.addCommand(new Command()
            {
                public void execute()
                {
                	//m_Desktop.showEvidenceWindow();
                	m_Desktop.showDailyUsageWindow(usageDate);
                }
            });

		}
	}
	
	
	class DateStampedValue
	{
		Date date;
		double value;


		public DateStampedValue(String dateTimeString, double value)
		{
			this.date = new Date(dateTimeString);
			this.value = value;
		}
	}
	
	DateStampedValue[] dateSequence = {
	    new DateStampedValue("1/28/2009 03:00", 13.0),
	    new DateStampedValue("1/28/2009 03:30", 12.9),
	    new DateStampedValue("1/28/2009 03:51", 12.9),
	    new DateStampedValue("1/28/2009 04:11", 12.9),
	    new DateStampedValue("1/28/2009 04:24", 13.0),
	    new DateStampedValue("1/28/2009 04:46", 12.5),
	    new DateStampedValue("1/28/2009 05:00", 12.2),
	    new DateStampedValue("1/28/2009 05:30", 12.8),
	    new DateStampedValue("1/28/2009 06:00", 11.6),
	    new DateStampedValue("1/28/2009 06:30", 12.5),
	    new DateStampedValue("1/28/2009 07:00", 11.4),
	    new DateStampedValue("1/28/2009 07:30", 12.9),
	    new DateStampedValue("1/28/2009 08:00", 12.9),
	    new DateStampedValue("1/28/2009 08:30", 11.2),
	    new DateStampedValue("1/28/2009 09:00", 11.7),
	    new DateStampedValue("1/28/2009 09:30", 12.4),
	    new DateStampedValue("1/28/2009 10:00", 14.4),
	    new DateStampedValue("1/28/2009 10:12", 13.7),
	    new DateStampedValue("1/28/2009 10:30", 11.9),
	    new DateStampedValue("1/28/2009 11:00", 14.3),
	    new DateStampedValue("1/28/2009 11:30", 14.0),
	    new DateStampedValue("1/28/2009 12:00", 14.7),
	    new DateStampedValue("1/28/2009 12:30", 15.4),
	    new DateStampedValue("1/28/2009 13:00", 15.5),
	};
	
	
	DateStampedValue[] cpuSequence = {
		    new DateStampedValue("1/28/2009 00:00", 3.0),
		    new DateStampedValue("1/28/2009 00:15", 2.9),
		    new DateStampedValue("1/28/2009 00:30", 4.9),
		    new DateStampedValue("1/28/2009 00:45", 3.9),
		    new DateStampedValue("1/28/2009 01:00", 3.0),
		    new DateStampedValue("1/28/2009 01:15", 1.9),
		    new DateStampedValue("1/28/2009 01:30", 4.9),
		    new DateStampedValue("1/28/2009 01:45", 8.9),
		    new DateStampedValue("1/28/2009 02:00", 13.0),
		    new DateStampedValue("1/28/2009 02:15", 12.9),
		    new DateStampedValue("1/28/2009 02:30", 7.9),
		    new DateStampedValue("1/28/2009 02:45", 2.9),
		    new DateStampedValue("1/28/2009 03:00", 3.0),
		    new DateStampedValue("1/28/2009 03:15", 2.9),
		    new DateStampedValue("1/28/2009 03:30", 2.9),
		    new DateStampedValue("1/28/2009 03:45", 2.9),
		    new DateStampedValue("1/28/2009 04:00", 3.0),
		    new DateStampedValue("1/28/2009 04:15", 2.1),
		    new DateStampedValue("1/28/2009 04:30", 11.9),
		    new DateStampedValue("1/28/2009 04:45", 53.1),
		    new DateStampedValue("1/28/2009 05:00", 73.0),
		    new DateStampedValue("1/28/2009 05:15", 86.0),
		    new DateStampedValue("1/28/2009 05:30", 93.9),
		    new DateStampedValue("1/28/2009 05:45", 92.9),
		    new DateStampedValue("1/28/2009 06:00", 73.0),
		    new DateStampedValue("1/28/2009 06:15", 52.9),
		    new DateStampedValue("1/28/2009 06:30", 22.9),
		    new DateStampedValue("1/28/2009 06:45", 12.9),
		    new DateStampedValue("1/28/2009 07:00", 3.0),
		    new DateStampedValue("1/28/2009 07:15", 2.9),
		    new DateStampedValue("1/28/2009 07:30", 2.9),
		    new DateStampedValue("1/28/2009 07:45", 1.5),
		    new DateStampedValue("1/28/2009 08:00", 23.0)

		};

}
