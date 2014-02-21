package demo.app.client;


import java.util.Date;

import com.googlecode.gchart.client.GChart;


public class BarChart extends GChart
{
	private DesktopApp m_Desktop;
	
	public BarChart()
	{
		addStyleName("peteGChart");

		setBackgroundColor(USE_CSS);
		setBorderStyle(USE_CSS); 
		
		addTimeLine(new Date());
		
		createChartWithTimeAxis();
		
	}
	
	
	public void setDesktop(DesktopApp desktop)
	{
		m_Desktop = desktop;
	}
	
	
	protected void createSimpleBarChart()
	{
		setChartTitle("<b>Simple Bar Chart</b>");
		setChartSize(150, 150);
		addCurve();
		for (int i = 0; i < 10; i++)
		{
			getCurve().addPoint(i, i * i);
		}

		getCurve().setLegendLabel("x<sup>2</sup>");
		getCurve().getSymbol().setSymbolType(SymbolType.VBAR_SOUTH);
		getCurve().getSymbol().setBackgroundColor("red");
		getCurve().getSymbol().setBorderColor("black");
		getCurve().getSymbol().setModelWidth(1.0);
		getXAxis().setAxisLabel("<b>x</b>");
		getXAxis().setHasGridlines(true);
		getYAxis().setAxisLabel("<b>x<sup>2</sup></b>");
		getYAxis().setHasGridlines(true);

	}
	
	
	protected void createChartWithTimeAxis()
	{
		setChartSize(400, 300);
		setChartTitle("<b><big>Temperature and CPU vs Time</big></b>");
		setPadding("5px");

		getXAxis().setAxisLabel("<small><b><i>Time</i></b></small>");
		getXAxis().setHasGridlines(true);
		getXAxis().setTickCount(6);
		// Except for "=(Date)", a standard GWT DateTimeFormat string
		getXAxis().setTickLabelFormat("=(Date)dd/h:mm a");
		getXAxis().setTickLength(6);    // small tick-like gap... 
	    getXAxis().setTickThickness(0); // but with invisible ticks


		
	    getYAxis().setAxisLabel("<small><b><i>%CPU</i></b></small>");
	    getYAxis().setHasGridlines(false);
	    // last bar 'sticks out' over right edge, so extend 'grid' right:
	    getYAxis().setTickCount(11);
	    getYAxis().setTickLength(15); 
	    getYAxis().setAxisMin(0);
		getYAxis().setAxisMax(100);


		
		
		// Add the second dataset.
		addCurve();
		getCurve().setLegendLabel("<i>CPU</i>");
		getCurve().setYAxis(Y_AXIS);
		getCurve().getSymbol().setSymbolType(SymbolType.VBAR_SOUTH);
		getCurve().getSymbol().setBackgroundColor("red");
		getCurve().getSymbol().setBorderColor("black");
		



		for (int i = 0; i < cpuSequence.length; i++)
		{
			// Note that getTime() returns milliseconds since
			// 1/1/70--required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			getCurve().addPoint(cpuSequence[i].date.getTime(),
					cpuSequence[i].value);
		}

	}
	
	
	public void addTimeLine(Date date)
	{
		/*
		addCurve();
		
		getCurve().getSymbol().setSymbolType(SymbolType.XGRIDLINE);
		getCurve().getSymbol().setBackgroundColor("silver");
		getCurve().getSymbol().setBorderColor("black");
		getCurve().getSymbol().setFillThickness(1);
		getCurve().getSymbol().setFillSpacing(1); 
		
		getCurve().addPoint(new Date("1/28/2008 08:45").getTime(), 0d);
		*/
		
		addCurve();
		
		getCurve().getSymbol().setSymbolType(SymbolType.BOX_CENTER);
	    getCurve().getSymbol().setWidth(2);
	    getCurve().getSymbol().setHeight(2);
	    getCurve().getSymbol().setBorderWidth(0);
	    getCurve().getSymbol().setBackgroundColor("navy");
	    getCurve().getSymbol().setFillThickness(2);
	    getCurve().getSymbol().setFillSpacing(5);
		getCurve().getSymbol().setHovertextTemplate("${x}");

		
		getCurve().addPoint(new Date("1/28/2008 11:45").getTime(), 0d);
		getCurve().addPoint(new Date("1/28/2008 11:45").getTime(), 100d);
		
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
	
	DateStampedValue[] cpuSequence = {
		    new DateStampedValue("1/28/2008 03:00", 3.0),
		    new DateStampedValue("1/28/2008 03:30", 2.9),
		    new DateStampedValue("1/28/2008 03:51", 42.9),
		    new DateStampedValue("1/28/2008 04:11", 82.9),
		    new DateStampedValue("1/28/2008 04:24", 83.0),
		    new DateStampedValue("1/28/2008 04:46", 22.5),
		    new DateStampedValue("1/28/2008 05:00", 12.2),
		    new DateStampedValue("1/28/2008 05:30", 2.8),
		    new DateStampedValue("1/28/2008 06:00", 1.6),
		    new DateStampedValue("1/28/2008 06:30", 2.5),
		    new DateStampedValue("1/28/2008 07:00", 1.4),
		    new DateStampedValue("1/28/2008 07:30", 2.9),
		    new DateStampedValue("1/28/2008 08:00", 2.9),
		    new DateStampedValue("1/28/2008 08:30", 1.2),
		    new DateStampedValue("1/28/2008 09:00", 1.7),
		    new DateStampedValue("1/28/2008 09:30", 2.4),
		    new DateStampedValue("1/28/2008 10:00", 4.4),
		    new DateStampedValue("1/28/2008 10:12", 3.7),
		    new DateStampedValue("1/28/2008 10:30", 21.9),
		    new DateStampedValue("1/28/2008 11:00", 64.3),
		    new DateStampedValue("1/28/2008 11:30", 74.0),
		    new DateStampedValue("1/28/2008 12:00", 84.7),
		    new DateStampedValue("1/28/2008 12:30", 85.4),
		    new DateStampedValue("1/28/2008 13:00", 75.5)
		};
	
}
