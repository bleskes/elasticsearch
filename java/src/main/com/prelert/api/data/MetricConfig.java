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
 ************************************************************/


package com.prelert.api.data;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.odata4j.core.OProperty;


/**
 * The metric configuration class
 * 
 * Metric names is the list of all metrics that should be tracked.
 */
public class MetricConfig 
{
	private int	m_Id;
	private int	m_Count	= 0;
	private Compression m_Compression = Compression.PLAIN;
	private byte[] m_MetricNames;
	
	private List<String> m_MetricPaths;

	public MetricConfig()
	{
		m_MetricPaths = Collections.emptyList();
	}
	
	@EntityKey
	public int getId() 
	{
		return m_Id;
	}

	public void setId(int id) 
	{
		m_Id = id;
	}

	public int getCount() 
	{
		return m_Count;
	}

	public void setCount(int count) 
	{
		m_Count = count;
	}

	public String getCompression() 
	{
		return m_Compression.toString();
	}

	public void setCompression(String compression) 
	{
		m_Compression = Compression.valueOf(compression.toUpperCase());
	}

	public byte[] getMetricNames() 
	{
		return m_MetricNames;
	}
	

	public void setMetricNames(byte[] metricNames) 
	{
		if (m_Compression == Compression.GZIP) 
		{
			byte[] buffer = GzipByteArrayUtil.uncompress(metricNames);
			if (buffer != null) 
			{
				m_MetricNames = buffer;
			}
		}
		else
		{
			m_MetricNames = metricNames;
		}
		
		parseJsonData(m_MetricNames);
	}
	
	
	@IgnoreProperty
	public List<String> getMetricPaths()
	{
		return m_MetricPaths;
	}
	
	
	/**
	 * Parse the list of metric names from the Json data
	 * Overwrites any previously set data.
	 * 
	 * @param data
	 * @return
	 */
	// TODO horrible unchecked API look at other Json libraries 
	private boolean parseJsonData(byte [] data)
	{
		if (data.length == 0)
		{
			return false;
		}
		
		m_MetricPaths = new ArrayList<String>();
		
		JSONArray jsonArrayIn = (JSONArray)JSONValue.parse(new String(data, Charset.forName(MetricFeed.JSON_CHARACTER_SET)));
		for (int i=0; i<jsonArrayIn.size(); i++)
		{
			String path = (String)jsonArrayIn.get(i);
			m_MetricPaths.add(path);
		}
		
		return m_MetricPaths.size() > 0;
	}


	
	public String toString() 
	{
		return String.format("MetricConfig[Id=%d, count=%d, compression=%s,"
				+ " rawDataLen = %d, dataLen = %d",m_Id, m_Count,
				m_Compression, m_MetricNames.length, getMetricNames().length);
	}

	
	
	public static MetricConfig fromOProperties(List<OProperty<?>> props)
	{
		MetricConfig mc = new MetricConfig();
		
		// compression type must be set first before data
		for (OProperty<?> prop : props)
		{		
			if ("Compression".equals(prop.getName()))
			{
				mc.setCompression((String)prop.getValue());
				break;
			}
		}
		
		for (OProperty<?> prop : props)
		{
			if ("Id".equals(prop.getName()))
			{
				mc.setId((Integer)prop.getValue());
			}
			else if ("Count".equals(prop.getName()))
			{
				mc.setCount((Integer)prop.getValue());
			}
			else if ("MetricNames".equals(prop.getName()))
			{
				mc.setMetricNames((byte[])prop.getValue());
			}
		}	
		
		return mc;
	}
	
	
	@SuppressWarnings("unchecked")
	public static byte[] jsonEncodeMetricPaths(List<String> metricPaths)
	{
		JSONArray jsonArray = new JSONArray();
		for (String path : metricPaths)
		{
			jsonArray.add(path);
			
		}

		return jsonArray.toJSONString().getBytes(Charset.forName(MetricFeed.JSON_CHARACTER_SET));
	}
}
