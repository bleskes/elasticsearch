/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package com.prelert.api.test.clw;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.supercsv.exception.SuperCSVException;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Parse the CSV strings returned by the CLWorkstation.jar and 
 * convert to metric path -> value pairs. 
 * <p/>
 * Header:<br/>
Domain,
Host,
Process,
AgentName,
Resource,
MetricName,
Record Type,
Period,
Intended End Timestamp,
Actual Start Timestamp,
Actual End Timestamp,
Value Count,
Value Type,
Integer Value,
Integer Min,
Integer Max,
Float Value,
Float Min,
Float Max,
String Value,
Date Value
<p/>
Example Line:<br/>
SuperDomain/OLB,aolbmab00001003,WebSphere,cefs_OP03_01,JSP|_paybillsSessionCleanup,Concurrent Invocations,JSP Concurrent Invocations,60,Tue Mar 01 02:00:00 EST 2011,Tue Mar 01 00:00:00 EST 2011,Tue Mar 01 00:01:00 EST 2011,0,Integer,0,0,0,,,,,
*/
public class ClwCsvProcessor 
{
	private static Logger s_Logger = Logger.getLogger(ClwCsvProcessor.class);

	private Pattern m_NewLineTabRegex;
	private DateFormat m_DateFormat;

	public ClwCsvProcessor()
	{
		m_NewLineTabRegex = Pattern.compile("\r?\n|\t");

		m_DateFormat = new SimpleDateFormat("EEE MMM dd H:m:s z yyyy");
	}
	
	
	/**
	 * Read and process the csv outputted by CLWorkstation.jar.
	 * Extracts the metric path and metric value from the data. Only
	 * data of type <code>Long</code> or <code>Integer</code> is processed
	 * data of other types (<code>Strings</code> etc) is ignored. Each returned
	 * <code>MetricData</code> contains all the data for that point in time.
	 * 
	 * @param reader
	 * @return List of <code>MetricData</code>, one element for each point in 
	 * time data was received. For example if the csv data contains 30 seconds of data 
	 * at 15s intervals then the size of the returned list will be 2.
	 */
	public List<MetricData> processCsv(Reader reader)
	{
		
		StringBuilder metricPathBuilder = new StringBuilder();

		Map<Date, MetricData> dateToMetricData = new HashMap<Date, MetricData>();
		
		CsvListReader csvReader = new CsvListReader(reader, CsvPreference.EXCEL_PREFERENCE);

		try
		{
			List<String> line;
			// Throw away header info in the first 2 lines.
			csvReader.read();
			csvReader.read();
			
			while ((line = csvReader.read()) != null)
			{
				if (line.size() <= 1)
				{
					continue;
				}
				
				try
				{
					// If not an integer or long then throw the metric away.
					String valueType = line.get(12);
					Double value;
					if (valueType.equals("Integer"))
					{
						value = new Integer(Integer.parseInt(line.get(13))).doubleValue();
					}
					else if (valueType.equals("Long"))
					{
						value = new Long(Long.parseLong(line.get(13))).doubleValue();
					}
					else 
					{
						s_Logger.trace("Invalid Value Type: " + valueType); 
						continue;
					}
					
					String domain = line.get(0);
					String host = line.get(1);
					String process = line.get(2);
					String agent = line.get(3);	
					String resource = stripNewLinesAndTabs(line.get(4));
					String metric = stripNewLinesAndTabs(line.get(5));

					
					Date actualEnd;
					try
					{
						actualEnd = m_DateFormat.parse(line.get(10));
					}
					catch (Exception pe)
					{
						s_Logger.error("Date Parse error from line = " + line + " Error: " + pe);
						csvReader.read();
						continue;
					}

					int valueCount = Integer.parseInt(line.get(11));
					if (valueCount == 0)
					{
						s_Logger.trace("ValueCount == 0 for line: " + Arrays.toString(line.toArray())); 
					}

					metricPathBuilder.delete(0, metricPathBuilder.length());
					metricPathBuilder.append(domain).append("|");
					metricPathBuilder.append(host).append("|");
					metricPathBuilder.append(process).append("|");
					metricPathBuilder.append(agent).append("|");
					metricPathBuilder.append(resource).append(":");
					metricPathBuilder.append(metric);
					
					MetricData md = dateToMetricData.get(actualEnd);
					if (md == null)
					{
						md = new MetricData(actualEnd);
						dateToMetricData.put(actualEnd, md);
					}
					
					md.addPoint(metricPathBuilder.toString(), value);			
				}
				catch (Exception e)
				{
					s_Logger.error("Error from line = " + line + "Error:" + e);

					continue;
				}
			}
		}
		catch (IOException e)
		{
			s_Logger.error("Error Parsing csv: " + e);
		}
		catch (SuperCSVException e)
		{
			s_Logger.error("Error reading line: " + e);
		}
				
		
		return new ArrayList<MetricData>(dateToMetricData.values());
	}
	
	/**
	 * Strip new lines and tabs out of the argument.
	 * @param value
	 * @return
	 */
	private String stripNewLinesAndTabs(String value)
	{
		return m_NewLineTabRegex.matcher(value).replaceAll(" ");
	}
	
}
