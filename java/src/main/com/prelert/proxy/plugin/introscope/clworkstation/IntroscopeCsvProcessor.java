/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.proxy.plugin.introscope.clworkstation;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.supercsv.exception.SuperCSVException;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.prelert.data.Attribute;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.plugin.introscope.AgentMetricPair;
import static com.prelert.proxy.plugin.introscope.IntroscopePlugin.*;


public class IntroscopeCsvProcessor
{
	private static Logger s_Logger = Logger.getLogger(IntroscopeCsvProcessor.class);

	private String m_DataType;
	private Pattern m_NewLineTabRegex;
	private DateFormat m_DateFormat;
	
	public IntroscopeCsvProcessor()
	{
		this("Introscope");
	}


	/**
	 * @param dataTypeName the name of the datatype usually 'Introscope'
	 */
	public IntroscopeCsvProcessor(String dataTypeName)
	{
		m_DataType = dataTypeName;
		
		m_NewLineTabRegex = Pattern.compile("\r?\n|\t");
		
		m_DateFormat = new SimpleDateFormat("EEE MMM dd H:m:s z yyyy");
	}


	/**
	 * Example line
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

	SuperDomain/OLB,aolbmab00001003,WebSphere,cefs_OP03_01,JSP|_paybillsSessionCleanup,Concurrent Invocations,JSP Concurrent Invocations,60,Tue Mar 01 02:00:00 EST 2011,Tue Mar 01 00:00:00 EST 2011,Tue Mar 01 00:01:00 EST 2011,0,Integer,0,0,0,,,,,

	 * @param csvDataReader
	 * @return
	 */
	public Collection<TimeSeriesData> processCsv(Reader csvDataReader)
	{
		
		StringBuilder stringBuilder = new StringBuilder();

		Map<String, TimeSeriesData> agentMetricToTimeSeriesData = new HashMap<String, TimeSeriesData>(); 

		CsvListReader csvReader = new CsvListReader(csvDataReader, CsvPreference.EXCEL_PREFERENCE);

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

					stringBuilder.delete(0, stringBuilder.length());
					stringBuilder.append(host);
					stringBuilder.append(process);
					stringBuilder.append(agent);
					stringBuilder.append(resource);
					stringBuilder.append(metric);

					String key = stringBuilder.toString();
					
					TimeSeriesData data = agentMetricToTimeSeriesData.get(key);
					if (data == null)
					{
						List<Attribute> attributes = new ArrayList<Attribute>();
						attributes.add(new Attribute(PROCESS_ATTRIBUTE, process,
												PATH_SEPARATOR, 1));
						attributes.add(new Attribute(AGENT_ATTRIBUTE, agent,
												PATH_SEPARATOR, 2));
				
						
						String [] resourcePaths = resource.split("\\|");
						for (int i = 0; i < resourcePaths.length; i++)
						{
							Attribute attr = new Attribute(RESOURCE_PATH_ATTRIBUTE + i, resourcePaths[i],
																PATH_SEPARATOR, i + 3);
							attributes.add(attr);
						}
											
						
						TimeSeriesConfig config = new TimeSeriesConfig(m_DataType, metric, host, attributes);
						config.setMetricPrefix(METRIC_SEPARATOR);
						config.setSourcePosition(0);

						String externalKey = AgentMetricPair.createExternalKey(host, process, agent,
								resource, metric);
						config.setExternalKey(externalKey);


						List<TimeSeriesDataPoint> points = new ArrayList<TimeSeriesDataPoint>();

						data = new TimeSeriesData(config, points);
						
						agentMetricToTimeSeriesData.put(key, data);
					}

					data.getDataPoints().add(new TimeSeriesDataPoint(actualEnd.getTime(), value));
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

		// Data points may have been added out of order so sort by time.
		for (TimeSeriesData data : agentMetricToTimeSeriesData.values())
		{
			Collections.sort(data.getDataPoints());
		}
				
		// clear memory
		stringBuilder.delete(0, stringBuilder.length());
				
		
		return agentMetricToTimeSeriesData.values();
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
