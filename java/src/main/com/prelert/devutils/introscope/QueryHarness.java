package com.prelert.devutils.introscope;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.prelert.proxy.inputmanager.querymonitor.HistoricalQueryMonitor;
import com.prelert.proxy.plugin.Plugin.InvalidPluginPropertyException;
import com.prelert.proxy.plugin.introscope.IntroscopePlugin;

/**
 * 
 */
public class QueryHarness 
{
	private static Logger s_Logger = Logger.getLogger(QueryHarness.class);
	
	private static final int COUNT_QUERIES = 10;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws InvalidPluginPropertyException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) 
	throws IOException, InvalidPluginPropertyException, ParseException, Exception
	
	{
		BasicConfigurator.configure();
		
		IntroscopePlugin plugin = new IntroscopePlugin();

		Properties pluginProps = new Properties();
		InputStream inputStream = ClassLoader.getSystemResourceAsStream("CaTimeSeriesMetrics.properties");
		pluginProps.load(inputStream);
		plugin.configure(null, pluginProps);
		plugin.setQueryMonitorPolicy(new HistoricalQueryMonitor());
		
		Properties harnessProps = new Properties();
		inputStream = ClassLoader.getSystemResourceAsStream("harness.properties");
		harnessProps.load(inputStream);
		
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		Date start = dateFormat.parse(harnessProps.getProperty("start"));
		
		
		Calendar cal = Calendar.getInstance();
		for (int minCount=1; minCount<=40; minCount++)
		{
			cal.setTime(start);
			cal.add(Calendar.MINUTE, minCount);
			Date end = cal.getTime();

			s_Logger.info("Setting query length to " + minCount + " minutes.");				
			for (int i=0; i<COUNT_QUERIES; ++i)
			{
				plugin.getAllDataPointsForTimeSpan(start, end, 15);
			}
		}

		System.exit(0);

	}
	
}
